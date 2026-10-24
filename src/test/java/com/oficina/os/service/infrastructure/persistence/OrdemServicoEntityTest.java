package com.oficina.os.service.infrastructure.persistence;

import com.oficina.os.service.domain.OrdemServico;
import com.oficina.os.service.domain.StatusOS;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrdemServicoEntity - Testes de Persistencia")
class OrdemServicoEntityTest {

    @Test
    @DisplayName("Deve criar entidade com valores padrao")
    void shouldCreateDefaultEntity() {
        OrdemServicoEntity entity = new OrdemServicoEntity();

        assertNull(entity.getId());
        assertNotNull(entity.getDataCriacao());
        assertNotNull(entity.getDataAtualizacao());
    }

    @Test
    @DisplayName("Deve criar entidade a partir do dominio")
    void shouldCreateEntityFromDomain() {
        OrdemServico dominio = new OrdemServico(5L, "OS-5", "Revisao", StatusOS.APROVADA, "12345678909", 300.0);

        OrdemServicoEntity entity = new OrdemServicoEntity(dominio);

        assertEquals(5L, entity.getId());
        assertEquals("OS-5", entity.getNumeroOS());
        assertEquals("Revisao", entity.getDescricao());
        assertEquals(StatusOS.APROVADA, entity.getStatus());
        assertEquals("12345678909", entity.getClienteCpf());
        assertEquals(300.0, entity.getOrcamento());
        assertNotNull(entity.getDataCriacao());
        assertNotNull(entity.getDataAtualizacao());
    }

    @Test
    @DisplayName("Deve converter entidade para dominio")
    void shouldConvertEntityToDomain() {
        OrdemServicoEntity entity = new OrdemServicoEntity();
        entity.setId(6L);
        entity.setNumeroOS("OS-6");
        entity.setDescricao("Alinhamento");
        entity.setStatus(StatusOS.CONCLUIDA);
        entity.setClienteCpf("11122233344");
        entity.setOrcamento(120.0);
        entity.setDataCriacao(LocalDateTime.of(2026, 1, 1, 10, 0));
        entity.setDataAtualizacao(LocalDateTime.of(2026, 1, 2, 11, 0));

        OrdemServico dominio = entity.toDomain();

        assertEquals(6L, dominio.getId());
        assertEquals("OS-6", dominio.getNumeroOS());
        assertEquals("Alinhamento", dominio.getDescricao());
        assertEquals(StatusOS.CONCLUIDA, dominio.getStatus());
        assertEquals("11122233344", dominio.getClienteCpf());
        assertEquals(120.0, dominio.getOrcamento());
        assertEquals("2026-01-01T10:00", dominio.getDataCriacao());
        assertEquals("2026-01-02T11:00", dominio.getDataAtualizacao());
    }

    @Test
    @DisplayName("Deve atualizar todos os campos via setters")
    void shouldSetAllFields() {
        OrdemServicoEntity entity = new OrdemServicoEntity();
        LocalDateTime criacao = LocalDateTime.of(2026, 2, 1, 8, 30);
        LocalDateTime atualizacao = LocalDateTime.of(2026, 2, 2, 9, 45);

        entity.setId(7L);
        entity.setNumeroOS("OS-7");
        entity.setDescricao("Balanceamento");
        entity.setStatus(StatusOS.CANCELADA);
        entity.setClienteCpf("55566677788");
        entity.setOrcamento(75.5);
        entity.setDataCriacao(criacao);
        entity.setDataAtualizacao(atualizacao);

        assertEquals(7L, entity.getId());
        assertEquals("OS-7", entity.getNumeroOS());
        assertEquals("Balanceamento", entity.getDescricao());
        assertEquals(StatusOS.CANCELADA, entity.getStatus());
        assertEquals("55566677788", entity.getClienteCpf());
        assertEquals(75.5, entity.getOrcamento());
        assertEquals(criacao, entity.getDataCriacao());
        assertEquals(atualizacao, entity.getDataAtualizacao());
    }
}
