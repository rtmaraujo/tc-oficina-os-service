package com.oficina.os.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oficina.os.service.application.event.SagaEvent;
import com.oficina.os.service.application.saga.SagaOrchestrator;
import com.oficina.os.service.infrastructure.persistence.EventoProcessadoEntity;
import com.oficina.os.service.infrastructure.repository.EventoProcessadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("KafkaListenerOs - Roteamento de eventos da Saga")
class KafkaListenerOsTest {

    @Mock
    private SagaOrchestrator orchestrator;
    @Mock
    private EventoProcessadoRepository eventoRepository;

    private KafkaListenerOs listener;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        listener = new KafkaListenerOs(orchestrator, eventoRepository, objectMapper);
        when(eventoRepository.existsById(anyString())).thenReturn(false);
        when(eventoRepository.save(any(EventoProcessadoEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private String payload(String tipo) {
        SagaEvent event = SagaEvent.of(tipo, "SAGA-OS-1");
        event.osId = 1L;
        event.osNumero = "OS-1";
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("Deve rotear budget-generated para onBudgetGenerated")
    void shouldRouteBudgetGenerated() {
        listener.consumir(payload("BUDGET_GENERATED"), "budget-generated");

        verify(orchestrator).onBudgetGenerated(any(SagaEvent.class));
        verify(eventoRepository).save(any(EventoProcessadoEntity.class));
    }

    @Test
    @DisplayName("Deve rotear todos os topicos de sucesso e compensacao")
    void shouldRouteAllTopics() {
        listener.consumir(payload("BUDGET_APPROVED"), "budget-approved");
        listener.consumir(payload("BUDGET_REJECTED"), "budget-rejected");
        listener.consumir(payload("PAYMENT_COMPLETED"), "payment-completed");
        listener.consumir(payload("PAYMENT_FAILED"), "payment-failed");
        listener.consumir(payload("EXECUTION_STARTED"), "order-progress");
        listener.consumir(payload("ORDER_FINALIZED"), "order-finalized");
        listener.consumir(payload("EXECUTION_COMPENSATED"), "execution-compensated");

        verify(orchestrator).onBudgetApproved(any(SagaEvent.class));
        verify(orchestrator).onBudgetRejected(any(SagaEvent.class));
        verify(orchestrator).onPaymentCompleted(any(SagaEvent.class));
        verify(orchestrator).onPaymentFailed(any(SagaEvent.class));
        verify(orchestrator).onOrderProgress(any(SagaEvent.class));
        verify(orchestrator).onOrderFinalized(any(SagaEvent.class));
        verify(orchestrator).onExecutionCompensated(any(SagaEvent.class));
    }

    @Test
    @DisplayName("Deve ignorar evento ja processado")
    void shouldIgnoreDuplicatedEvent() {
        when(eventoRepository.existsById(anyString())).thenReturn(true);

        listener.consumir(payload("BUDGET_APPROVED"), "budget-approved");

        verifyNoInteractions(orchestrator);
        verify(eventoRepository, never()).save(any(EventoProcessadoEntity.class));
    }

    @Test
    @DisplayName("Deve usar o tipo do evento quando o topico nao vier no header")
    void shouldUseEventTypeWhenTopicIsMissing() {
        SagaEvent event = SagaEvent.of("payment-completed", "SAGA-OS-1");
        try {
            listener.consumir(objectMapper.writeValueAsString(event), null);
        } catch (Exception e) {
            fail(e);
        }

        verify(orchestrator).onPaymentCompleted(any(SagaEvent.class));
    }

    @Test
    @DisplayName("Deve ignorar topico desconhecido sem chamar o orquestrador")
    void shouldIgnoreUnknownTopic() {
        listener.consumir(payload("DESCONHECIDO"), "topico-x");

        verifyNoInteractions(orchestrator);
        verify(eventoRepository).save(any(EventoProcessadoEntity.class));
    }

    @Test
    @DisplayName("Deve falhar com payload invalido")
    void shouldFailOnInvalidPayload() {
        assertThrows(IllegalArgumentException.class,
            () -> listener.consumir("{invalido}", "budget-approved"));

        verifyNoInteractions(orchestrator);
    }

    @Test
    @DisplayName("Nao deve registrar idempotencia quando o evento nao tiver eventId")
    void shouldNotRegisterWithoutEventId() {
        listener.consumir("{\"type\":\"BUDGET_APPROVED\",\"sagaId\":\"SAGA-1\"}",
            "budget-approved");

        verify(orchestrator).onBudgetApproved(any(SagaEvent.class));
        verify(eventoRepository, never()).save(any(EventoProcessadoEntity.class));
    }
}
