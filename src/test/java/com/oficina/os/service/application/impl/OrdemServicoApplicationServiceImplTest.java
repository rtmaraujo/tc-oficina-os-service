package com.oficina.os.service.application.impl;

import com.oficina.os.service.domain.OrdemServico;
import com.oficina.os.service.domain.StatusOS;
import com.oficina.os.service.infrastructure.persistence.OrdemServicoEntity;
import com.oficina.os.service.infrastructure.repository.OrdemServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrdemServicoApplicationServiceImpl - Testes de Aplicacao")
class OrdemServicoApplicationServiceImplTest {

    @Mock
    private OrdemServicoRepository repository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OrdemServicoApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrdemServicoApplicationServiceImpl(repository, kafkaTemplate);
    }

    private OrdemServicoEntity ordemExistente() {
        OrdemServicoEntity entity = new OrdemServicoEntity(
            new OrdemServico(1L, "OS-1", "Revisao", StatusOS.ABERTA, "12345678909", 0.0));
        entity.setId(1L);
        return entity;
    }

    @Test
    @DisplayName("Deve criar ordem de servico e publicar evento os-created")
    void shouldCreateOrderServiceAndPublishEvent() {
        when(repository.save(any(OrdemServicoEntity.class))).thenAnswer(invocation -> {
            OrdemServicoEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });

        OrdemServico result = service.criarOrdemServico("12345678909", "Revisao");

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertTrue(result.getNumeroOS().startsWith("OS-"));
        assertEquals("Revisao", result.getDescricao());
        assertEquals("12345678909", result.getClienteCpf());
        assertEquals(0.0, result.getOrcamento());
        assertEquals(StatusOS.ABERTA, result.getStatus());
        verify(repository).save(any(OrdemServicoEntity.class));
        verify(kafkaTemplate).send(eq("os-created"), eq("10"), contains("status=ABERTA"));
    }

    @Test
    @DisplayName("Deve atualizar status para APROVADA e publicar evento")
    void shouldUpdateStatusToApproved() {
        when(repository.findById(1L)).thenReturn(Optional.of(ordemExistente()));
        when(repository.save(any(OrdemServicoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrdemServico result = service.atualizarStatus(1L, StatusOS.APROVADA);

        assertEquals(StatusOS.APROVADA, result.getStatus());
        assertNotNull(result.getDataAtualizacao());
        verify(kafkaTemplate).send(eq("budget-approved"), eq("1"), contains("OS-1"));
    }

    @Test
    @DisplayName("Deve atualizar status para EXECUCAO e publicar evento")
    void shouldUpdateStatusToExecution() {
        when(repository.findById(1L)).thenReturn(Optional.of(ordemExistente()));
        when(repository.save(any(OrdemServicoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrdemServico result = service.atualizarStatus(1L, StatusOS.EXECUCAO);

        assertEquals(StatusOS.EXECUCAO, result.getStatus());
        verify(kafkaTemplate).send(eq("order-finalized"), eq("1"), contains("OS-1"));
    }

    @Test
    @DisplayName("Deve atualizar status para CONCLUIDA e publicar evento")
    void shouldUpdateStatusToCompleted() {
        when(repository.findById(1L)).thenReturn(Optional.of(ordemExistente()));
        when(repository.save(any(OrdemServicoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrdemServico result = service.atualizarStatus(1L, StatusOS.CONCLUIDA);

        assertEquals(StatusOS.CONCLUIDA, result.getStatus());
        verify(kafkaTemplate).send(eq("order-completed"), eq("1"), contains("OS-1"));
    }

    @Test
    @DisplayName("Deve atualizar status para CANCELADA e publicar evento")
    void shouldUpdateStatusToCancelled() {
        when(repository.findById(1L)).thenReturn(Optional.of(ordemExistente()));
        when(repository.save(any(OrdemServicoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrdemServico result = service.atualizarStatus(1L, StatusOS.CANCELADA);

        assertEquals(StatusOS.CANCELADA, result.getStatus());
        verify(kafkaTemplate).send(eq("order-cancelled"), eq("1"), contains("OS-1"));
    }

    @Test
    @DisplayName("Nao deve publicar evento para status sem tratamento")
    void shouldNotPublishEventForOpenStatus() {
        when(repository.findById(1L)).thenReturn(Optional.of(ordemExistente()));
        when(repository.save(any(OrdemServicoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrdemServico result = service.atualizarStatus(1L, StatusOS.ABERTA);

        assertEquals(StatusOS.ABERTA, result.getStatus());
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("Deve retornar null ao atualizar status de OS inexistente")
    void shouldReturnNullWhenUpdatingUnknownOrder() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertNull(service.atualizarStatus(99L, StatusOS.APROVADA));
        verify(repository, never()).save(any(OrdemServicoEntity.class));
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("Deve consultar OS por id")
    void shouldFindOrderById() {
        when(repository.findById(1L)).thenReturn(Optional.of(ordemExistente()));

        assertNotNull(service.consultarPorId(1L));
    }

    @Test
    @DisplayName("Deve retornar null ao consultar OS inexistente")
    void shouldReturnNullWhenOrderNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertNull(service.consultarPorId(99L));
    }

    @Test
    @DisplayName("Deve listar OS por cliente")
    void shouldListOrdersByClient() {
        List<OrdemServicoEntity> ordens = List.of(ordemExistente());
        when(repository.findByClienteCpf("12345678909")).thenReturn(ordens);

        List<OrdemServico> result = service.listarPorCliente("12345678909");

        assertEquals(1, result.size());
        verify(repository).findByClienteCpf("12345678909");
    }
}
