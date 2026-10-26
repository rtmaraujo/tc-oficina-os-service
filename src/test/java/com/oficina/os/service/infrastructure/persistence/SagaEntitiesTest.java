package com.oficina.os.service.infrastructure.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Entidades da Saga - Testes de Persistencia")
class SagaEntitiesTest {

    @Test
    @DisplayName("SagaInstanciaEntity deve iniciar em andamento e expor campos")
    void shouldCreateSagaInstancia() {
        SagaInstanciaEntity saga = new SagaInstanciaEntity("SAGA-1", 1L, "OS-1");

        assertEquals("SAGA-1", saga.getSagaId());
        assertEquals(1L, saga.getOsId());
        assertEquals("OS-1", saga.getOsNumero());
        assertEquals(SagaInstanciaEntity.EM_ANDAMENTO, saga.getStatus());
        assertEquals("OS_CRIADA", saga.getPassoAtual());
        assertNotNull(saga.getDataInicio());
        assertNotNull(saga.getDataAtualizacao());

        saga.setId(10L);
        saga.setSagaId("SAGA-2");
        saga.setOsId(2L);
        saga.setOsNumero("OS-2");
        saga.setStatus(SagaInstanciaEntity.COMPENSADA);
        saga.setPassoAtual("COMPENSACAO");
        saga.setMotivo("falha");
        saga.setDataInicio(LocalDateTime.of(2026, 1, 1, 10, 0));
        saga.setDataAtualizacao(LocalDateTime.of(2026, 1, 2, 10, 0));

        assertEquals(10L, saga.getId());
        assertEquals("SAGA-2", saga.getSagaId());
        assertEquals(2L, saga.getOsId());
        assertEquals("OS-2", saga.getOsNumero());
        assertEquals(SagaInstanciaEntity.COMPENSADA, saga.getStatus());
        assertEquals("COMPENSACAO", saga.getPassoAtual());
        assertEquals("falha", saga.getMotivo());
        assertEquals(LocalDateTime.of(2026, 1, 1, 10, 0), saga.getDataInicio());
        assertEquals(LocalDateTime.of(2026, 1, 2, 10, 0), saga.getDataAtualizacao());
    }

    @Test
    @DisplayName("SagaPassoEntity deve registrar passo")
    void shouldCreateSagaPasso() {
        SagaPassoEntity passo = new SagaPassoEntity("SAGA-1", "OS_CRIADA", "CONCLUIDO", "ok");

        assertEquals("SAGA-1", passo.getSagaId());
        assertEquals("OS_CRIADA", passo.getPasso());
        assertEquals("CONCLUIDO", passo.getStatus());
        assertEquals("ok", passo.getDetalhe());
        assertNotNull(passo.getTimestamp());

        passo.setId(1L);
        passo.setSagaId("SAGA-2");
        passo.setPasso("PAGAMENTO");
        passo.setStatus("FALHA");
        passo.setDetalhe("timeout");
        passo.setTimestamp(LocalDateTime.of(2026, 1, 1, 10, 0));

        assertEquals(1L, passo.getId());
        assertEquals("SAGA-2", passo.getSagaId());
        assertEquals("PAGAMENTO", passo.getPasso());
        assertEquals("FALHA", passo.getStatus());
        assertEquals("timeout", passo.getDetalhe());
        assertEquals(LocalDateTime.of(2026, 1, 1, 10, 0), passo.getTimestamp());
    }

    @Test
    @DisplayName("OrdemServicoHistoricoEntity deve registrar transicao")
    void shouldCreateHistorico() {
        OrdemServicoHistoricoEntity h = new OrdemServicoHistoricoEntity(
            1L, "ABERTA", "APROVADA", "aprovado", "SAGA-1");

        assertEquals(1L, h.getOsId());
        assertEquals("ABERTA", h.getStatusAnterior());
        assertEquals("APROVADA", h.getStatusNovo());
        assertEquals("aprovado", h.getMotivo());
        assertEquals("SAGA-1", h.getSagaId());
        assertNotNull(h.getTimestamp());

        h.setId(2L);
        h.setOsId(2L);
        h.setStatusAnterior("APROVADA");
        h.setStatusNovo("PAGA");
        h.setMotivo("pago");
        h.setSagaId("SAGA-2");
        h.setTimestamp(LocalDateTime.of(2026, 1, 1, 10, 0));

        assertEquals(2L, h.getId());
        assertEquals(2L, h.getOsId());
        assertEquals("APROVADA", h.getStatusAnterior());
        assertEquals("PAGA", h.getStatusNovo());
        assertEquals("pago", h.getMotivo());
        assertEquals("SAGA-2", h.getSagaId());
    }

    @Test
    @DisplayName("EventoProcessadoEntity deve registrar evento")
    void shouldCreateEventoProcessado() {
        EventoProcessadoEntity e = new EventoProcessadoEntity("evt-1", "os-created");

        assertEquals("evt-1", e.getEventId());
        assertEquals("os-created", e.getTopico());
        assertNotNull(e.getProcessadoEm());

        e.setProcessadoEm(LocalDateTime.of(2026, 1, 1, 10, 0));

        assertEquals(LocalDateTime.of(2026, 1, 1, 10, 0), e.getProcessadoEm());

        e.setEventId("evt-2");
        e.setTopico("budget-approved");

        assertEquals("evt-2", e.getEventId());
        assertEquals("budget-approved", e.getTopico());
    }
}
