package com.oficina.os.service.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrdemServico - Testes de Dominio")
class OrdemServicoTest {

    @Test
    @DisplayName("Deve criar ordem de servico vazia")
    void shouldCreateEmptyOrdemServico() {
        OrdemServico os = new OrdemServico();

        assertNull(os.getId());
        assertNull(os.getNumeroOS());
        assertNull(os.getDescricao());
        assertNull(os.getStatus());
        assertNull(os.getClienteCpf());
        assertEquals(0.0, os.getOrcamento());
        assertNull(os.getDataCriacao());
        assertNull(os.getDataAtualizacao());
    }

    @Test
    @DisplayName("Deve criar ordem de servico com construtor completo")
    void shouldCreateOrdemServicoWithFullConstructor() {
        OrdemServico os = new OrdemServico(1L, "OS-1", "Revisao", StatusOS.ABERTA, "12345678909", 250.0);

        assertEquals(1L, os.getId());
        assertEquals("OS-1", os.getNumeroOS());
        assertEquals("Revisao", os.getDescricao());
        assertEquals(StatusOS.ABERTA, os.getStatus());
        assertEquals("12345678909", os.getClienteCpf());
        assertEquals(250.0, os.getOrcamento());
        assertNotNull(os.getDataCriacao());
        assertEquals(os.getDataCriacao(), os.getDataAtualizacao());
    }

    @Test
    @DisplayName("Deve atualizar todos os campos via setters")
    void shouldSetAllFields() {
        OrdemServico os = new OrdemServico();

        os.setId(2L);
        os.setNumeroOS("OS-2");
        os.setDescricao("Troca de oleo");
        os.setStatus(StatusOS.EXECUCAO);
        os.setClienteCpf("98765432100");
        os.setOrcamento(180.0);
        os.setDataCriacao("2026-01-01T10:00:00");
        os.setDataAtualizacao("2026-01-02T10:00:00");

        assertEquals(2L, os.getId());
        assertEquals("OS-2", os.getNumeroOS());
        assertEquals("Troca de oleo", os.getDescricao());
        assertEquals(StatusOS.EXECUCAO, os.getStatus());
        assertEquals("98765432100", os.getClienteCpf());
        assertEquals(180.0, os.getOrcamento());
        assertEquals("2026-01-01T10:00:00", os.getDataCriacao());
        assertEquals("2026-01-02T10:00:00", os.getDataAtualizacao());
    }

    @Test
    @DisplayName("Deve expor todos os status da OS")
    void shouldExposeAllStatus() {
        assertEquals(7, StatusOS.values().length);
        assertEquals(StatusOS.ABERTA, StatusOS.valueOf("ABERTA"));
        assertEquals(StatusOS.ORCAMENTO_GERADO, StatusOS.valueOf("ORCAMENTO_GERADO"));
        assertEquals(StatusOS.APROVADA, StatusOS.valueOf("APROVADA"));
        assertEquals(StatusOS.EXECUCAO, StatusOS.valueOf("EXECUCAO"));
        assertEquals(StatusOS.CONCLUIDA, StatusOS.valueOf("CONCLUIDA"));
        assertEquals(StatusOS.CANCELADA, StatusOS.valueOf("CANCELADA"));
        assertEquals(StatusOS.FINALIZADA, StatusOS.valueOf("FINALIZADA"));
    }
}
