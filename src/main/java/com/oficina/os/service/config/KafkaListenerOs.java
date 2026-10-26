package com.oficina.os.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oficina.os.service.application.event.SagaEvent;
import com.oficina.os.service.application.saga.SagaOrchestrator;
import com.oficina.os.service.infrastructure.persistence.EventoProcessadoEntity;
import com.oficina.os.service.infrastructure.repository.EventoProcessadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumidor de eventos da Saga no OS Service (orquestrador).
 *
 * <p>Os eventos chegam como JSON no envelope {@link SagaEvent}. O eventId e
 * registrado para garantir idempotencia em caso de reentrega do Kafka.</p>
 */
@Component
public class KafkaListenerOs {

    private static final Logger log = LoggerFactory.getLogger(KafkaListenerOs.class);

    private final SagaOrchestrator orchestrator;
    private final EventoProcessadoRepository eventoRepository;
    private final ObjectMapper objectMapper;

    public KafkaListenerOs(SagaOrchestrator orchestrator,
                           EventoProcessadoRepository eventoRepository,
                           ObjectMapper objectMapper) {
        this.orchestrator = orchestrator;
        this.eventoRepository = eventoRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
        topics = {
            "budget-generated",
            "budget-approved",
            "budget-rejected",
            "payment-completed",
            "payment-failed",
            "order-progress",
            "order-finalized",
            "execution-compensated"
        },
        groupId = "os-service-group"
    )
    public void consumir(String message,
                         @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = false) String topico) {
        SagaEvent event;
        try {
            event = objectMapper.readValue(message, SagaEvent.class);
        } catch (Exception e) {
            log.error("Payload invalido no topico {}: {}", topico, message);
            throw new IllegalArgumentException("Payload invalido em " + topico, e);
        }

        if (event.eventId != null && eventoRepository.existsById(event.eventId)) {
            log.info("Evento {} ja processado, ignorando", event.eventId);
            return;
        }

        MDC.put("sagaId", event.sagaId);
        MDC.put("osNumero", event.osNumero);
        try {
            switch (topico != null ? topico : String.valueOf(event.type)) {
                case "budget-generated" -> orchestrator.onBudgetGenerated(event);
                case "budget-approved" -> orchestrator.onBudgetApproved(event);
                case "budget-rejected" -> orchestrator.onBudgetRejected(event);
                case "payment-completed" -> orchestrator.onPaymentCompleted(event);
                case "payment-failed" -> orchestrator.onPaymentFailed(event);
                case "order-progress" -> orchestrator.onOrderProgress(event);
                case "order-finalized" -> orchestrator.onOrderFinalized(event);
                case "execution-compensated" -> orchestrator.onExecutionCompensated(event);
                default -> log.warn("Topico sem tratamento: {}", topico);
            }
            if (event.eventId != null) {
                eventoRepository.save(new EventoProcessadoEntity(event.eventId, topico));
            }
            log.info("Evento {} processado com sucesso (saga {})", topico, event.sagaId);
        } finally {
            MDC.remove("sagaId");
            MDC.remove("osNumero");
        }
    }
}
