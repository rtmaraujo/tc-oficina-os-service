package com.oficina.os.service.config;

import com.oficina.os.service.application.saga.SagaOrchestrator;
import com.oficina.os.service.infrastructure.persistence.SagaInstanciaEntity;
import com.oficina.os.service.infrastructure.repository.SagaInstanciaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Timeout da Saga: sagas sem evolucao por mais de {@code saga.timeout.minutes}
 * sao compensadas automaticamente (evita OS presa em orcamento/pagamento).
 */
@Component
public class SagaTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(SagaTimeoutScheduler.class);

    private final SagaInstanciaRepository sagaRepository;
    private final SagaOrchestrator orchestrator;
    private final long timeoutMinutes;

    public SagaTimeoutScheduler(SagaInstanciaRepository sagaRepository,
                                SagaOrchestrator orchestrator,
                                @Value("${saga.timeout.minutes:30}") long timeoutMinutes) {
        this.sagaRepository = sagaRepository;
        this.orchestrator = orchestrator;
        this.timeoutMinutes = timeoutMinutes;
    }

    @Scheduled(fixedDelayString = "${saga.timeout.check-ms:60000}")
    public void verificarTimeouts() {
        List<SagaInstanciaEntity> expiradas = sagaRepository
            .findByStatusAndDataAtualizacaoBefore(
                SagaInstanciaEntity.EM_ANDAMENTO,
                LocalDateTime.now().minusMinutes(timeoutMinutes));

        for (SagaInstanciaEntity saga : expiradas) {
            log.warn("Saga {} expirou no passo {}; compensando", saga.getSagaId(), saga.getPassoAtual());
            orchestrator.compensar(saga.getSagaId(),
                "Timeout de " + timeoutMinutes + " minutos no passo " + saga.getPassoAtual());
        }
    }
}
