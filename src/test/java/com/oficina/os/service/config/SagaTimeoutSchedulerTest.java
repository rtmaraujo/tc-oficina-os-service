package com.oficina.os.service.config;

import com.oficina.os.service.application.saga.SagaOrchestrator;
import com.oficina.os.service.infrastructure.persistence.SagaInstanciaEntity;
import com.oficina.os.service.infrastructure.repository.SagaInstanciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SagaTimeoutScheduler - Compensacao por timeout")
class SagaTimeoutSchedulerTest {

    @Mock
    private SagaInstanciaRepository sagaRepository;
    @Mock
    private SagaOrchestrator orchestrator;

    private SagaTimeoutScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SagaTimeoutScheduler(sagaRepository, orchestrator, 30L);
    }

    @Test
    @DisplayName("Deve compensar sagas expiradas")
    void shouldCompensateExpiredSagas() {
        SagaInstanciaEntity saga = new SagaInstanciaEntity("SAGA-1", 1L, "OS-1");
        saga.setPassoAtual("ORCAMENTO_GERADO");
        when(sagaRepository.findByStatusAndDataAtualizacaoBefore(eq(SagaInstanciaEntity.EM_ANDAMENTO),
            any(LocalDateTime.class))).thenReturn(List.of(saga));

        scheduler.verificarTimeouts();

        ArgumentCaptor<String> motivo = ArgumentCaptor.forClass(String.class);
        verify(orchestrator).compensar(eq("SAGA-1"), motivo.capture());
        assertTrue(motivo.getValue().contains("30"));
        assertTrue(motivo.getValue().contains("ORCAMENTO_GERADO"));
    }

    @Test
    @DisplayName("Nao deve compensar quando nao houver sagas expiradas")
    void shouldNotCompensateWhenNothingExpired() {
        when(sagaRepository.findByStatusAndDataAtualizacaoBefore(anyString(),
            any(LocalDateTime.class))).thenReturn(List.of());

        scheduler.verificarTimeouts();

        verifyNoInteractions(orchestrator);
    }
}
