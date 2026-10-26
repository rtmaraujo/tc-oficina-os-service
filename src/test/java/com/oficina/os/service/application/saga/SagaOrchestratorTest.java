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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SagaOrchestrator - Maquina de estados e compensacao")
class SagaOrchestratorTest {

    @Mock
    private OrdemServicoRepository osRepository;
    @Mock
    private SagaInstanciaRepository sagaRepository;
    @Mock
    private SagaPassoRepository passoRepository;
    @Mock
    private OrdemServicoHistoricoRepository historicoRepository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final Map<String, SagaInstanciaEntity> sagas = new LinkedHashMap<>();
    private final List<SagaPassoEntity> passos = new ArrayList<>();
    private final List<OrdemServicoHistoricoEntity> historico = new ArrayList<>();

    private SagaOrchestrator orchestrator;
    private OrdemServicoEntity os;

    @BeforeEach
    void setUp() {
        sagas.clear();
        passos.clear();
        historico.clear();
        orchestrator = new SagaOrchestrator(osRepository, sagaRepository, passoRepository,
            historicoRepository, kafkaTemplate, new ObjectMapper());

        os = new OrdemServicoEntity(new OrdemServico(1L, "OS-1", "Revisao",
            StatusOS.ABERTA, "12345678909", 0.0));
        os.setId(1L);

        when(osRepository.findById(1L)).thenReturn(Optional.of(os));
        when(osRepository.save(any(OrdemServicoEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(sagaRepository.save(any(SagaInstanciaEntity.class))).thenAnswer(i -> {
            SagaInstanciaEntity saga = i.getArgument(0);
            sagas.put(saga.getSagaId(), saga);
            return saga;
        });
        when(sagaRepository.findBySagaId(anyString())).thenAnswer(i ->
            Optional.ofNullable(sagas.get(i.getArgument(0))));
        when(sagaRepository.findByOsId(any(Long.class))).thenAnswer(i -> sagas.values().stream()
            .filter(s -> i.getArgument(0).equals(s.getOsId())).findFirst());
        when(passoRepository.save(any(SagaPassoEntity.class))).thenAnswer(i -> {
            passos.add(i.getArgument(0));
            return i.getArgument(0);
        });
        when(historicoRepository.save(any(OrdemServicoHistoricoEntity.class))).thenAnswer(i -> {
            historico.add(i.getArgument(0));
            return i.getArgument(0);
        });
    }

    private void criarSaga() {
        orchestrator.iniciarSaga(os);
    }

    private SagaEvent evento(String tipo) {
        SagaEvent event = SagaEvent.of(tipo, "SAGA-OS-1");
        event.osId = 1L;
        event.osNumero = "OS-1";
        return event;
    }

    @Test
    @DisplayName("Deve iniciar saga, gravar passo e publicar os-created")
    void shouldStartSaga() {
        String sagaId = orchestrator.iniciarSaga(os);

        assertEquals("SAGA-OS-1", sagaId);
        assertEquals(SagaInstanciaEntity.EM_ANDAMENTO, sagas.get(sagaId).getStatus());
        assertEquals("OS_CRIADA", sagas.get(sagaId).getPassoAtual());
        assertEquals(1, passos.size());
        verify(kafkaTemplate).send(eq("os-created"), eq("1"), contains("\"type\":\"OS_CREATED\""));
    }

    @Test
    @DisplayName("Deve avancar para ORCAMENTO_GERADO e gravar valor do orcamento")
    void shouldHandleBudgetGenerated() {
        criarSaga();
        SagaEvent event = evento("BUDGET_GENERATED");
        event.valor = 250.0;
        event.codigoOrcamento = "ORC-1";

        OrdemServico result = orchestrator.onBudgetGenerated(event);

        assertEquals(StatusOS.ORCAMENTO_GERADO, result.getStatus());
        assertEquals(250.0, result.getOrcamento());
        assertEquals("ORCAMENTO_GERADO", sagas.get("SAGA-OS-1").getPassoAtual());
        assertTrue(historico.stream().anyMatch(h -> "ORCAMENTO_GERADO".equals(h.getStatusNovo())
            && "SAGA-OS-1".equals(h.getSagaId())));
    }

    @Test
    @DisplayName("Deve avancar para APROVADA ao aprovar orcamento")
    void shouldHandleBudgetApproved() {
        criarSaga();
        os.setStatus(StatusOS.ORCAMENTO_GERADO);

        OrdemServico result = orchestrator.onBudgetApproved(evento("BUDGET_APPROVED"));

        assertEquals(StatusOS.APROVADA, result.getStatus());
        assertEquals("ORCAMENTO_APROVADO", sagas.get("SAGA-OS-1").getPassoAtual());
    }

    @Test
    @DisplayName("Deve aceitar orcamento sem valor informado")
    void shouldHandleBudgetWithoutValue() {
        criarSaga();
        SagaEvent event = evento("BUDGET_GENERATED");
        event.valor = null;

        OrdemServico result = orchestrator.onBudgetGenerated(event);

        assertEquals(StatusOS.ORCAMENTO_GERADO, result.getStatus());
        assertEquals(0.0, result.getOrcamento());
    }

    @Test
    @DisplayName("Deve ignorar valor zero no orcamento")
    void shouldIgnoreZeroBudgetValue() {
        criarSaga();
        os.setOrcamento(99.0);
        SagaEvent event = evento("BUDGET_GENERATED");
        event.valor = 0.0;

        OrdemServico result = orchestrator.onBudgetGenerated(event);

        assertEquals(99.0, result.getOrcamento());
    }

    @Test
    @DisplayName("Deve falhar quando evento nao traz sagaId nem osId")
    void shouldFailWhenEventHasNoSagaNorOs() {
        SagaEvent event = evento("BUDGET_APPROVED");
        event.sagaId = null;
        event.osId = null;

        assertThrows(IllegalStateException.class, () -> orchestrator.onBudgetApproved(event));
    }

    @Test
    @DisplayName("Deve compensar quando orcamento for rejeitado")
    void shouldCompensateWhenBudgetRejected() {
        criarSaga();
        os.setStatus(StatusOS.ORCAMENTO_GERADO);

        OrdemServico result = orchestrator.onBudgetRejected(evento("BUDGET_REJECTED"));

        assertEquals(StatusOS.CANCELADA, result.getStatus());
        assertEquals(SagaInstanciaEntity.COMPENSADA, sagas.get("SAGA-OS-1").getStatus());
        verify(kafkaTemplate).send(eq("compensate-budget"), eq("1"), anyString());
        verify(kafkaTemplate).send(eq("compensate-execution"), eq("1"), anyString());
        verify(kafkaTemplate).send(eq("saga-compensated"), eq("1"), anyString());
    }

    @Test
    @DisplayName("Deve marcar PAGA e solicitar execucao ao confirmar pagamento")
    void shouldHandlePaymentCompleted() {
        criarSaga();
        os.setStatus(StatusOS.APROVADA);
        SagaEvent event = evento("PAYMENT_COMPLETED");
        event.paymentId = "MP-1";

        OrdemServico result = orchestrator.onPaymentCompleted(event);

        assertEquals(StatusOS.PAGA, result.getStatus());
        assertEquals("PAGAMENTO_CONFIRMADO", sagas.get("SAGA-OS-1").getPassoAtual());
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("execution-requested"), eq("1"), payload.capture());
        assertTrue(payload.getValue().contains("EXECUTION_REQUESTED"));
        assertTrue(payload.getValue().contains("MP-1"));
    }

    @Test
    @DisplayName("Deve compensar quando pagamento falhar")
    void shouldCompensateWhenPaymentFails() {
        criarSaga();
        os.setStatus(StatusOS.APROVADA);
        SagaEvent event = evento("PAYMENT_FAILED");
        event.message = "cartao recusado";

        OrdemServico result = orchestrator.onPaymentFailed(event);

        assertEquals(StatusOS.CANCELADA, result.getStatus());
        assertTrue(sagas.get("SAGA-OS-1").getMotivo().contains("cartao recusado"));
    }

    @Test
    @DisplayName("Deve avancar de PAGA para EXECUCAO no progresso")
    void shouldHandleOrderProgress() {
        criarSaga();
        os.setStatus(StatusOS.PAGA);
        SagaEvent event = evento("EXECUTION_STARTED");
        event.status = "DIAGNOSTICO";

        OrdemServico result = orchestrator.onOrderProgress(event);

        assertEquals(StatusOS.EXECUCAO, result.getStatus());
        assertEquals("EXECUCAO_INICIADA", sagas.get("SAGA-OS-1").getPassoAtual());
    }

    @Test
    @DisplayName("Nao deve alterar status em progresso de OS ainda nao paga")
    void shouldIgnoreProgressBeforePayment() {
        criarSaga();
        os.setStatus(StatusOS.ABERTA);
        SagaEvent event = evento("EXECUTION_STARTED");
        event.status = "DIAGNOSTICO";

        OrdemServico result = orchestrator.onOrderProgress(event);

        assertEquals(StatusOS.ABERTA, result.getStatus());
        verify(osRepository, never()).save(any(OrdemServicoEntity.class));
    }

    @Test
    @DisplayName("Deve concluir a OS e a saga ao finalizar execucao")
    void shouldHandleOrderFinalized() {
        criarSaga();
        os.setStatus(StatusOS.EXECUCAO);

        OrdemServico result = orchestrator.onOrderFinalized(evento("ORDER_FINALIZED"));

        assertEquals(StatusOS.CONCLUIDA, result.getStatus());
        assertEquals(SagaInstanciaEntity.CONCLUIDA, sagas.get("SAGA-OS-1").getStatus());
        assertEquals("EXECUCAO_FINALIZADA", sagas.get("SAGA-OS-1").getPassoAtual());
    }

    @Test
    @DisplayName("Deve registrar historico ao receber execucao compensada")
    void shouldHandleExecutionCompensated() {
        criarSaga();
        SagaEvent event = evento("EXECUTION_COMPENSATED");
        event.message = "execucao cancelada";

        OrdemServico result = orchestrator.onExecutionCompensated(event);

        assertNotNull(result);
        assertTrue(historico.stream().anyMatch(h -> "execucao cancelada".equals(h.getMotivo())));
    }

    @Test
    @DisplayName("Compensacao deve ser idempotente")
    void compensationShouldBeIdempotent() {
        criarSaga();
        orchestrator.compensar("SAGA-OS-1", "motivo");
        int publicadosAntes = passos.size();

        orchestrator.compensar("SAGA-OS-1", "motivo 2");

        assertEquals(publicadosAntes, passos.size());
        assertEquals(StatusOS.CANCELADA, os.getStatus());
    }

    @Test
    @DisplayName("Compensacao de saga inexistente deve retornar null")
    void compensationUnknownSagaShouldReturnNull() {
        when(sagaRepository.findBySagaId("SAGA-X")).thenReturn(Optional.empty());

        assertNull(orchestrator.compensar("SAGA-X", "motivo"));
    }

    @Test
    @DisplayName("Deve compensar pelo id da OS")
    void shouldCompensateByOsId() {
        criarSaga();

        OrdemServico result = orchestrator.compensarPorOs(1L, "cancelamento");

        assertEquals(StatusOS.CANCELADA, result.getStatus());
    }

    @Test
    @DisplayName("Compensacao por OS sem saga deve retornar null")
    void compensateUnknownOsShouldReturnNull() {
        when(sagaRepository.findByOsId(99L)).thenReturn(Optional.empty());

        assertNull(orchestrator.compensarPorOs(99L, "cancelamento"));
    }

    @Test
    @DisplayName("Deve finalizar OS e registrar historico")
    void shouldFinalizeOs() {
        OrdemServicoEntity result = orchestrator.finalizar(os, "finalizacao");

        assertEquals(StatusOS.FINALIZADA, result.getStatus());
        assertTrue(historico.stream().anyMatch(h -> "FINALIZADA".equals(h.getStatusNovo())));
    }

    @Test
    @DisplayName("Finalizar OS ja finalizada deve ser idempotente")
    void finalizeAlreadyFinalizedShouldBeIdempotent() {
        os.setStatus(StatusOS.FINALIZADA);

        OrdemServicoEntity result = orchestrator.finalizar(os, "finalizacao");

        assertSame(os, result);
        verify(osRepository, never()).save(any(OrdemServicoEntity.class));
    }

    @Test
    @DisplayName("Deve consultar historico por OS")
    void shouldConsultHistory() {
        when(historicoRepository.findByOsIdOrderByTimestampAsc(1L)).thenReturn(historico);

        assertNotNull(orchestrator.historico(1L));
        verify(historicoRepository).findByOsIdOrderByTimestampAsc(1L);
    }

    @Test
    @DisplayName("Deve localizar saga pelo osId quando sagaId nao vier no evento")
    void shouldFindSagaByOsIdWhenSagaIdIsMissing() {
        criarSaga();
        os.setStatus(StatusOS.APROVADA);
        SagaEvent event = evento("PAYMENT_COMPLETED");
        event.sagaId = null;
        event.paymentId = "MP-2";

        OrdemServico result = orchestrator.onPaymentCompleted(event);

        assertEquals(StatusOS.PAGA, result.getStatus());
    }

    @Test
    @DisplayName("Deve falhar quando a saga nao existir")
    void shouldFailWhenSagaDoesNotExist() {
        SagaEvent event = evento("BUDGET_APPROVED");
        event.sagaId = "SAGA-INEXISTENTE";

        assertThrows(IllegalStateException.class, () -> orchestrator.onBudgetApproved(event));
    }

    @Test
    @DisplayName("Deve falhar quando a OS nao existir")
    void shouldFailWhenOsDoesNotExist() {
        criarSaga();
        when(osRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
            () -> orchestrator.onBudgetApproved(evento("BUDGET_APPROVED")));
    }

    @Test
    @DisplayName("Deve encapsular erro de serializacao do evento")
    void shouldWrapSerializationError() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("erro") { });
        SagaOrchestrator failing = new SagaOrchestrator(osRepository, sagaRepository, passoRepository,
            historicoRepository, kafkaTemplate, failingMapper);
        when(sagaRepository.save(any(SagaInstanciaEntity.class))).thenAnswer(i -> i.getArgument(0));

        assertThrows(IllegalStateException.class, () -> failing.iniciarSaga(os));
    }

    @Test
    @DisplayName("Deve manter data de atualizacao ao compensar")
    void compensationShouldUpdateTimestamp() {
        criarSaga();
        LocalDateTime antes = sagas.get("SAGA-OS-1").getDataAtualizacao();

        orchestrator.compensar("SAGA-OS-1", "motivo");

        assertNotNull(sagas.get("SAGA-OS-1").getDataAtualizacao());
        assertTrue(!sagas.get("SAGA-OS-1").getDataAtualizacao().isBefore(antes));
    }
}
