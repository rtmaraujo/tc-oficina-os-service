package com.oficina.os.service.application.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Envelope unico dos eventos trocados entre os microsservicos da Saga.
 *
 * <p>Campos nulos sao omitidos na serializacao para manter o payload enxuto.</p>
 */
public class SagaEvent {

    public String eventId;
    public String sagaId;
    public String correlationId;
    public String type;
    public Long osId;
    public String osNumero;
    public String clienteCpf;
    public String descricao;
    public Long orcamentoId;
    public String codigoOrcamento;
    public Double valor;
    public Long execucaoId;
    public String status;
    public String paymentId;
    public String message;
    public String occurredAt;

    public SagaEvent() {
        this.occurredAt = OffsetDateTime.now().toString();
    }

    public static SagaEvent of(String type, String sagaId) {
        SagaEvent event = new SagaEvent();
        event.eventId = UUID.randomUUID().toString();
        event.correlationId = UUID.randomUUID().toString();
        event.sagaId = sagaId;
        event.type = type;
        return event;
    }
}
