package com.oficina.os.service.application.saga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oficina.os.service.application.event.SagaEvent;
import com.oficina.os.service.domain.OrdemServico;
import com.oficina.os.service.domain.StatusOS;
import com.oficina.os.service.infrastructure.persistence.OrdemServicoEntity;
import com.oficina.os.service.infrastructure.persistence.OrdemServicoHistoricoEntity;
import com.oficina.os.service.infrastructure.persistence.SagaInstanciaEntity;
import com.oficina.os.service.infrastructure.persistence.SagaPassoEntity;
import com.oficina.os.service.infrastructure.repository.OrdemServicoHistoricoRepository;
import com.oficina.os.service.infrastructure.repository.OrdemServicoRepository;
import com.oficina.os.service.infrastructure.repository.SagaInstanciaRepository;
import com.oficina.os.service.infrastructure.repository.SagaPassoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Orquestrador central da Saga da Ordem de Servico.
 *
 * <p>O OS Service mantem a maquina de estados e decide os proximos passos,
 * publicando comandos/eventos para Billing e Execution. Em caso de falha
 * (orcamento rejeitado, pagamento recusado, falha de execucao ou timeout)
 * o orquestrador dispara as compensacoes.</p>
 */
@Service
public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

    public static final String TOPICO_OS_CREATED = "os-created";
    public static final String TOPICO_EXECUTION_REQUESTED = "execution-requested";
    public static final String TOPICO_COMPENSATE_BUDGET = "compensate-budget";
    public static final String TOPICO_COMPENSATE_EXECUTION = "compensate-execution";
    public static final String TOPICO_SAGA_COMPENSATED = "saga-compensated";

    private final OrdemServicoRepository osRepository;
    private final SagaInstanciaRepository sagaRepository;
    private final SagaPassoRepository passoRepository;
    private final OrdemServicoHistoricoRepository historicoRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public SagaOrchestrator(OrdemServicoRepository osRepository,
                            SagaInstanciaRepository sagaRepository,
                            SagaPassoRepository passoRepository,
                            OrdemServicoHistoricoRepository historicoRepository,
                            KafkaTemplate<String, String> kafkaTemplate,
                            ObjectMapper objectMapper) {
        this.osRepository = osRepository;
        this.sagaRepository = sagaRepository;
        this.passoRepository = passoRepository;
        this.historicoRepository = historicoRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public String iniciarSaga(OrdemServicoEntity os) {
        String sagaId = "SAGA-" + os.getNumeroOS();
        SagaInstanciaEntity saga = new SagaInstanciaEntity(sagaId, os.getId(), os.getNumeroOS());
        sagaRepository.save(saga);
        registrarPasso(sagaId, "OS_CRIADA", "CONCLUIDO", "Ordem de servico criada");

        SagaEvent event = novoEvento("OS_CREATED", sagaId, os);
        event.clienteCpf = os.getClienteCpf();
        event.descricao = os.getDescricao();
        publicar(TOPICO_OS_CREATED, os.getId().toString(), event);
        return sagaId;
    }

    @Transactional
    public OrdemServico onBudgetGenerated(SagaEvent event) {
        SagaInstanciaEntity saga = obterSaga(event);
        OrdemServicoEntity os = obterOs(saga);
        if (event.valor != null && event.valor > 0) {
            os.setOrcamento(event.valor);
        }
        avancar(saga, os, StatusOS.ORCAMENTO_GERADO, "ORCAMENTO_GERADO",
            "Orcamento " + event.codigoOrcamento + " gerado", event);
        return os.toDomain();
    }

    @Transactional
    public OrdemServico onBudgetApproved(SagaEvent event) {
        SagaInstanciaEntity saga = obterSaga(event);
        OrdemServicoEntity os = obterOs(saga);
        avancar(saga, os, StatusOS.APROVADA, "ORCAMENTO_APROVADO",
            "Orcamento aprovado pelo cliente", event);
        return os.toDomain();
    }

    @Transactional
    public OrdemServico onBudgetRejected(SagaEvent event) {
        return compensar(event.sagaId, "Orcamento rejeitado pelo cliente");
    }

    @Transactional
    public OrdemServico onPaymentCompleted(SagaEvent event) {
        SagaInstanciaEntity saga = obterSaga(event);
        OrdemServicoEntity os = obterOs(saga);
        avancar(saga, os, StatusOS.PAGA, "PAGAMENTO_CONFIRMADO",
            "Pagamento " + event.paymentId + " confirmado", event);

        SagaEvent comando = novoEvento("EXECUTION_REQUESTED", saga.getSagaId(), os);
        comando.paymentId = event.paymentId;
        publicar(TOPICO_EXECUTION_REQUESTED, os.getId().toString(), comando);
        return os.toDomain();
    }

    @Transactional
    public OrdemServico onPaymentFailed(SagaEvent event) {
        return compensar(event.sagaId, "Pagamento recusado: " + event.message);
    }

    @Transactional
    public OrdemServico onOrderProgress(SagaEvent event) {
        SagaInstanciaEntity saga = obterSaga(event);
        OrdemServicoEntity os = obterOs(saga);
        if (os.getStatus() == StatusOS.PAGA || os.getStatus() == StatusOS.APROVADA) {
            avancar(saga, os, StatusOS.EXECUCAO, "EXECUCAO_INICIADA",
                "Execucao iniciada: " + event.status, event);
        }
        return os.toDomain();
    }

    @Transactional
    public OrdemServico onOrderFinalized(SagaEvent event) {
        SagaInstanciaEntity saga = obterSaga(event);
        OrdemServicoEntity os = obterOs(saga);
        avancar(saga, os, StatusOS.CONCLUIDA, "EXECUCAO_FINALIZADA",
            "Execucao finalizada pelo Execution Service", event);
        saga.setStatus(SagaInstanciaEntity.CONCLUIDA);
        saga.setDataAtualizacao(LocalDateTime.now());
        sagaRepository.save(saga);
        return os.toDomain();
    }

    @Transactional
    public OrdemServico onExecutionCompensated(SagaEvent event) {
        SagaInstanciaEntity saga = obterSaga(event);
        OrdemServicoEntity os = obterOs(saga);
        registrarHistorico(os, os.getStatus(), "Execucao compensada",
            event.message, saga.getSagaId());
        return os.toDomain();
    }

    @Transactional
    public OrdemServico compensarPorOs(Long osId, String motivo) {
        Optional<SagaInstanciaEntity> saga = sagaRepository.findByOsId(osId);
        if (saga.isEmpty()) {
            log.warn("Saga da OS {} nao encontrada para compensacao", osId);
            return null;
        }
        return compensar(saga.get().getSagaId(), motivo);
    }

    /**
     * Executa o rollback logico da saga: cancela a OS e publica comandos de
     * compensacao para orcamento e execucao. Idempotente por sagaId.
     */
    @Transactional
    public OrdemServico compensar(String sagaId, String motivo) {        Optional<SagaInstanciaEntity> optional = sagaRepository.findBySagaId(sagaId);
        if (optional.isEmpty()) {
            log.warn("Saga {} nao encontrada para compensacao", sagaId);
            return null;
        }
        SagaInstanciaEntity saga = optional.get();
        if (SagaInstanciaEntity.COMPENSADA.equals(saga.getStatus())) {
            return obterOs(saga).toDomain();
        }

        OrdemServicoEntity os = obterOs(saga);
        registrarHistorico(os, os.getStatus(), StatusOS.CANCELADA.name(), motivo, sagaId);
        os.setStatus(StatusOS.CANCELADA);
        os.setDataAtualizacao(LocalDateTime.now());
        osRepository.save(os);

        saga.setStatus(SagaInstanciaEntity.COMPENSADA);
        saga.setPassoAtual("COMPENSACAO");
        saga.setMotivo(motivo);
        saga.setDataAtualizacao(LocalDateTime.now());
        sagaRepository.save(saga);
        registrarPasso(sagaId, "COMPENSACAO", "CONCLUIDO", motivo);

        SagaEvent compensacao = novoEvento("SAGA_COMPENSATED", sagaId, os);
        compensacao.message = motivo;
        publicar(TOPICO_COMPENSATE_BUDGET, os.getId().toString(), compensacao);
        publicar(TOPICO_COMPENSATE_EXECUTION, os.getId().toString(), compensacao);
        publicar(TOPICO_SAGA_COMPENSATED, os.getId().toString(), compensacao);
        return os.toDomain();
    }

    /**
     * Finalizacao manual da OS apos a execucao concluida.
     */
    @Transactional
    public OrdemServicoEntity finalizar(OrdemServicoEntity os, String motivo) {
        if (os.getStatus() == StatusOS.FINALIZADA) {
            return os;
        }
        registrarHistorico(os, os.getStatus(), StatusOS.FINALIZADA.name(), motivo, null);
        os.setStatus(StatusOS.FINALIZADA);
        os.setDataAtualizacao(LocalDateTime.now());
        return osRepository.save(os);
    }

    public void registrarHistorico(OrdemServicoEntity os, StatusOS anterior, String novo,
                                   String motivo, String sagaId) {
        historicoRepository.save(new OrdemServicoHistoricoEntity(
            os.getId(),
            anterior != null ? anterior.name() : null,
            novo,
            motivo,
            sagaId));
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoHistoricoEntity> historico(Long osId) {
        return historicoRepository.findByOsIdOrderByTimestampAsc(osId);
    }

    private void avancar(SagaInstanciaEntity saga, OrdemServicoEntity os, StatusOS novoStatus,
                         String passo, String motivo, SagaEvent event) {
        StatusOS anterior = os.getStatus();
        if (anterior != novoStatus) {
            registrarHistorico(os, anterior, novoStatus.name(), motivo, saga.getSagaId());
            os.setStatus(novoStatus);
            os.setDataAtualizacao(LocalDateTime.now());
            osRepository.save(os);
        }
        saga.setPassoAtual(passo);
        saga.setDataAtualizacao(LocalDateTime.now());
        sagaRepository.save(saga);
        registrarPasso(saga.getSagaId(), passo, "CONCLUIDO", motivo);
        log.info("Saga {} avancou para {} (OS {})", saga.getSagaId(), passo, os.getNumeroOS());
    }

    private void registrarPasso(String sagaId, String passo, String status, String detalhe) {
        passoRepository.save(new SagaPassoEntity(sagaId, passo, status, detalhe));
    }

    private SagaInstanciaEntity obterSaga(SagaEvent event) {
        String sagaId = event.sagaId;
        if (sagaId == null && event.osId != null) {
            return sagaRepository.findByOsId(event.osId)
                .orElseThrow(() -> new IllegalStateException("Saga nao encontrada para OS " + event.osId));
        }
        return sagaRepository.findBySagaId(sagaId)
            .orElseThrow(() -> new IllegalStateException("Saga nao encontrada: " + sagaId));
    }

    private OrdemServicoEntity obterOs(SagaInstanciaEntity saga) {
        return osRepository.findById(saga.getOsId())
            .orElseThrow(() -> new IllegalStateException("OS nao encontrada: " + saga.getOsId()));
    }

    private SagaEvent novoEvento(String type, String sagaId, OrdemServicoEntity os) {
        SagaEvent event = SagaEvent.of(type, sagaId);
        event.osId = os.getId();
        event.osNumero = os.getNumeroOS();
        event.clienteCpf = os.getClienteCpf();
        event.valor = os.getOrcamento();
        return event;
    }

    private void publicar(String topico, String chave, SagaEvent event) {
        try {
            kafkaTemplate.send(topico, chave, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar evento para " + topico, e);
        }
    }
}
