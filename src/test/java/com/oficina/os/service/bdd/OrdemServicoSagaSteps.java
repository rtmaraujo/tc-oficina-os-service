package com.oficina.os.service.bdd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oficina.os.service.application.event.SagaEvent;
import com.oficina.os.service.application.impl.OrdemServicoApplicationServiceImpl;
import com.oficina.os.service.application.saga.SagaOrchestrator;
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
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Step definitions do fluxo BDD da Saga orquestrada.
 *
 * Usa o servico de aplicacao e o orquestrador reais, com repositorios em
 * memoria e um KafkaTemplate que registra os topicos publicados.
 */
public class OrdemServicoSagaSteps {

    private final Map<Long, OrdemServicoEntity> bancoOs = new LinkedHashMap<>();
    private final Map<String, SagaInstanciaEntity> bancoSagas = new LinkedHashMap<>();
    private final List<SagaPassoEntity> passos = new ArrayList<>();
    private final List<OrdemServicoHistoricoEntity> historico = new ArrayList<>();
    private final List<String> eventosPublicados = new ArrayList<>();
    private final AtomicLong sequenciaId = new AtomicLong(0);

    private OrdemServicoApplicationServiceImpl service;
    private SagaOrchestrator orchestrator;
    private OrdemServico ultimaOrdem;
    private SagaEvent ultimoEvento;
    private int eventosAntesDaUltimaAcao;

    @Before
    public void prepararServico() {
        bancoOs.clear();
        bancoSagas.clear();
        passos.clear();
        historico.clear();
        eventosPublicados.clear();
        sequenciaId.set(0);
        ultimaOrdem = null;
        ultimoEvento = null;
        eventosAntesDaUltimaAcao = 0;

        OrdemServicoRepository osRepository = mock(OrdemServicoRepository.class);
        when(osRepository.save(any(OrdemServicoEntity.class))).thenAnswer(invocation -> {
            OrdemServicoEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(sequenciaId.incrementAndGet());
            }
            bancoOs.put(entity.getId(), entity);
            return entity;
        });
        when(osRepository.findById(any(Long.class)))
            .thenAnswer(invocation -> Optional.ofNullable(bancoOs.get(invocation.getArgument(0))));

        SagaInstanciaRepository sagaRepository = mock(SagaInstanciaRepository.class);
        when(sagaRepository.save(any(SagaInstanciaEntity.class))).thenAnswer(invocation -> {
            SagaInstanciaEntity saga = invocation.getArgument(0);
            bancoSagas.put(saga.getSagaId(), saga);
            return saga;
        });
        when(sagaRepository.findBySagaId(anyString())).thenAnswer(invocation ->
            Optional.ofNullable(bancoSagas.get(invocation.getArgument(0))));
        when(sagaRepository.findByOsId(any(Long.class))).thenAnswer(invocation -> {
            Long osId = invocation.getArgument(0);
            return bancoSagas.values().stream().filter(s -> osId.equals(s.getOsId())).findFirst();
        });
        when(sagaRepository.findByStatusAndDataAtualizacaoBefore(anyString(), any(LocalDateTime.class)))
            .thenReturn(List.of());

        SagaPassoRepository passoRepository = mock(SagaPassoRepository.class);
        when(passoRepository.save(any(SagaPassoEntity.class))).thenAnswer(invocation -> {
            SagaPassoEntity passo = invocation.getArgument(0);
            passos.add(passo);
            return passo;
        });

        OrdemServicoHistoricoRepository historicoRepository = mock(OrdemServicoHistoricoRepository.class);
        when(historicoRepository.save(any(OrdemServicoHistoricoEntity.class))).thenAnswer(invocation -> {
            OrdemServicoHistoricoEntity registro = invocation.getArgument(0);
            historico.add(registro);
            return registro;
        });
        when(historicoRepository.findByOsIdOrderByTimestampAsc(any(Long.class)))
            .thenAnswer(invocation -> historico);

        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            eventosPublicados.add(invocation.getArgument(0));
            return null;
        });

        ObjectMapper objectMapper = new ObjectMapper();
        orchestrator = new SagaOrchestrator(osRepository, sagaRepository, passoRepository,
            historicoRepository, kafkaTemplate, objectMapper);
        service = new OrdemServicoApplicationServiceImpl(osRepository, orchestrator);
    }

    private void registrarInicioDaAcao() {
        eventosAntesDaUltimaAcao = eventosPublicados.size();
    }

    private SagaEvent eventoParaSaga(String tipo) {
        SagaEvent event = SagaEvent.of(tipo, ultimaOrdem != null
            ? "SAGA-" + ultimaOrdem.getNumeroOS() : "SAGA-DESCONHECIDA");
        event.osId = ultimaOrdem.getId();
        event.osNumero = ultimaOrdem.getNumeroOS();
        return event;
    }

    @Dado("que o servico de OS esta disponivel")
    public void servicoDisponivel() {
        assertNotNull(service);
    }

    @Quando("eu abrir uma ordem de servico para o cliente {string} com descricao {string}")
    public void abrirOrdemServico(String clienteCpf, String descricao) {
        registrarInicioDaAcao();
        ultimaOrdem = service.criarOrdemServico(clienteCpf, descricao);
    }

    @Quando("o Billing gerar o orcamento da OS")
    public void billingGerarOrcamento() {
        SagaEvent event = eventoParaSaga("BUDGET_GENERATED");
        event.codigoOrcamento = "ORC-1";
        event.valor = 250.0;
        ultimoEvento = event;
        ultimaOrdem = orchestrator.onBudgetGenerated(event);
    }

    @Quando("o cliente aprovar o orcamento")
    public void aprovarOrcamento() {
        ultimaOrdem = orchestrator.onBudgetApproved(eventoParaSaga("BUDGET_APPROVED"));
    }

    @Quando("o cliente rejeitar o orcamento")
    public void rejeitarOrcamento() {
        ultimaOrdem = orchestrator.onBudgetRejected(eventoParaSaga("BUDGET_REJECTED"));
    }

    @Quando("o pagamento for confirmado")
    public void confirmarPagamento() {
        SagaEvent event = eventoParaSaga("PAYMENT_COMPLETED");
        event.paymentId = "MP-123";
        ultimaOrdem = orchestrator.onPaymentCompleted(event);
    }

    @Quando("a execucao iniciar o diagnostico")
    public void iniciarExecucao() {
        SagaEvent event = eventoParaSaga("EXECUTION_STARTED");
        event.status = "DIAGNOSTICO";
        ultimaOrdem = orchestrator.onOrderProgress(event);
    }

    @Quando("a execucao for finalizada")
    public void finalizarExecucao() {
        ultimaOrdem = orchestrator.onOrderFinalized(eventoParaSaga("ORDER_FINALIZED"));
    }

    @Quando("eu finalizar a OS")
    public void finalizarOrdemServico() {
        registrarInicioDaAcao();
        ultimaOrdem = service.atualizarStatus(ultimaOrdem.getId(), StatusOS.FINALIZADA);
    }

    @Entao("a OS deve estar com status {string}")
    public void osDeveEstarComStatus(String statusEsperado) {
        assertNotNull(ultimaOrdem, "Nenhuma OS foi manipulada no fluxo");
        assertEquals(StatusOS.valueOf(statusEsperado), ultimaOrdem.getStatus());
        assertNotNull(ultimaOrdem.getDataAtualizacao());
    }

    @E("o evento {string} deve ter sido publicado")
    public void eventoDeveTerSidoPublicado(String topico) {
        assertTrue(eventosPublicados.contains(topico),
            "Evento '" + topico + "' nao foi publicado. Eventos: " + eventosPublicados);
    }

    @E("nenhum novo evento deve ter sido publicado")
    public void nenhumNovoEventoDeveTerSidoPublicado() {
        assertEquals(eventosAntesDaUltimaAcao, eventosPublicados.size(),
            "Era esperado nao publicar eventos, mas foram publicados: " + eventosPublicados);
    }
}
