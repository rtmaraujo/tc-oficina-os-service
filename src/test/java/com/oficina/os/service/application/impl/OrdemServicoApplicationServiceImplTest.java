package com.oficina.os.service.application.impl;

import com.oficina.os.service.application.saga.SagaOrchestrator;
import com.oficina.os.service.domain.OrdemServico;
import com.oficina.os.service.domain.StatusOS;
import com.oficina.os.service.infrastructure.persistence.OrdemServicoEntity;
import com.oficina.os.service.infrastructure.persistence.OrdemServicoHistoricoEntity;
import com.oficina.os.service.infrastructure.repository.OrdemServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private SagaOrchestrator sagaOrchestrator;

    private OrdemServicoApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrdemServicoApplicationServiceImpl(repository, sagaOrchestrator);
    }

    private OrdemServicoEntity ordemExistente() {
        OrdemServicoEntity entity = new OrdemServicoEntity(
            new OrdemServico(1L, "OS-1", "Revisao", StatusOS.ABERTA, "12345678909", 0.0));
        entity.setId(1L);
        return entity;
    }

    @Test
    @DisplayName("Deve criar OS, registrar historico e iniciar a saga")
    void shouldCreateOrderServiceAndStartSaga() {
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
        verify(sagaOrchestrator).registrarHistorico(any(OrdemServicoEntity.class), isNull(),
            eq("ABERTA"), anyString(), isNull());
        verify(sagaOrchestrator).iniciarSaga(any(OrdemServicoEntity.class));
    }

    @Test
    @DisplayName("Deve atualizar status manualmente e registrar historico")
    void shouldUpdateStatusManually() {
        when(repository.findById(1L)).thenReturn(Optional.of(ordemExistente()));
        when(repository.save(any(OrdemServicoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrdemServico result = service.atualizarStatus(1L, StatusOS.APROVADA);

        assertEquals(StatusOS.APROVADA, result.getStatus());
        assertNotNull(result.getDataAtualizacao());
        verify(sagaOrchestrator).registrarHistorico(any(OrdemServicoEntity.class),
            eq(StatusOS.ABERTA), eq("APROVADA"), anyString(), isNull());
    }

    @Test
    @DisplayName("Deve delegar cancelamento para a compensacao da saga")
    void shouldDelegateCancellationToSaga() {
        OrdemServico cancelada = new OrdemServico(1L, "OS-1", "Revisao",
            StatusOS.CANCELADA, "12345678909", 0.0);
        when(repository.findById(1L)).thenReturn(Optional.of(ordemExistente()));
        when(sagaOrchestrator.compensarPorOs(eq(1L), anyString())).thenReturn(cancelada);

        OrdemServico result = service.atualizarStatus(1L, StatusOS.CANCELADA);

        assertEquals(StatusOS.CANCELADA, result.getStatus());
        verify(sagaOrchestrator).compensarPorOs(eq(1L), anyString());
        verify(repository, never()).save(any(OrdemServicoEntity.class));
    }

    @Test
    @DisplayName("Deve delegar finalizacao para o orquestrador")
    void shouldDelegateFinalization() {
        OrdemServicoEntity entity = ordemExistente();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(sagaOrchestrator.finalizar(eq(entity), anyString())).thenAnswer(invocation -> {
            OrdemServicoEntity os = invocation.getArgument(0);
            os.setStatus(StatusOS.FINALIZADA);
            return os;
        });

        OrdemServico result = service.atualizarStatus(1L, StatusOS.FINALIZADA);

        assertEquals(StatusOS.FINALIZADA, result.getStatus());
        verify(sagaOrchestrator).finalizar(eq(entity), anyString());
    }

    @Test
    @DisplayName("Deve retornar null ao atualizar status de OS inexistente")
    void shouldReturnNullWhenUpdatingUnknownOrder() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertNull(service.atualizarStatus(99L, StatusOS.APROVADA));
        verifyNoInteractions(sagaOrchestrator);
        verify(repository, never()).save(any(OrdemServicoEntity.class));
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

    @Test
    @DisplayName("Deve consultar historico da OS")
    void shouldConsultHistory() {
        List<OrdemServicoHistoricoEntity> historico = List.of(
            new OrdemServicoHistoricoEntity(1L, null, "ABERTA", "criada", "SAGA-OS-1"));
        when(sagaOrchestrator.historico(1L)).thenReturn(historico);

        assertEquals(1, service.consultarHistorico(1L).size());
        verify(sagaOrchestrator).historico(1L);
    }
}
