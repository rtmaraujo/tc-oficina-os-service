package com.oficina.os.service.presentation;

import com.oficina.os.service.application.OrdemServicoApplicationService;
import com.oficina.os.service.domain.OrdemServico;
import com.oficina.os.service.domain.StatusOS;
import com.oficina.os.service.infrastructure.persistence.OrdemServicoHistoricoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrdemServicoController - Endpoints REST")
class OrdemServicoControllerTest {

    @Mock
    private OrdemServicoApplicationService applicationService;

    private OrdemServicoController controller;

    @BeforeEach
    void setUp() {
        controller = new OrdemServicoController();
        ReflectionTestUtils.setField(controller, "applicationService", applicationService);
    }

    private OrdemServico ordem() {
        return new OrdemServico(1L, "OS-1", "Revisao", StatusOS.ABERTA, "12345678909", 0.0);
    }

    @Test
    @DisplayName("Deve criar uma OS")
    void shouldCreateOrder() {
        when(applicationService.criarOrdemServico("12345678909", "Revisao")).thenReturn(ordem());

        ResponseEntity<OrdemServico> response = controller.criar("12345678909", "Revisao");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OS-1", response.getBody().getNumeroOS());
    }

    @Test
    @DisplayName("Deve consultar OS existente")
    void shouldFindOrder() {
        when(applicationService.consultarPorId(1L)).thenReturn(ordem());

        ResponseEntity<OrdemServico> response = controller.consultar(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getId());
    }

    @Test
    @DisplayName("Deve retornar 404 ao consultar OS inexistente")
    void shouldReturnNotFoundWhenOrderDoesNotExist() {
        when(applicationService.consultarPorId(99L)).thenReturn(null);

        assertEquals(HttpStatus.NOT_FOUND, controller.consultar(99L).getStatusCode());
    }

    @Test
    @DisplayName("Deve consultar historico de OS existente")
    void shouldFindHistory() {
        when(applicationService.consultarPorId(1L)).thenReturn(ordem());
        when(applicationService.consultarHistorico(1L)).thenReturn(List.of(
            new OrdemServicoHistoricoEntity(1L, null, "ABERTA", "criada", "SAGA-OS-1")));

        ResponseEntity<List<OrdemServicoHistoricoEntity>> response = controller.historico(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("Deve retornar 404 no historico de OS inexistente")
    void shouldReturnNotFoundForHistoryOfUnknownOrder() {
        when(applicationService.consultarPorId(99L)).thenReturn(null);

        assertEquals(HttpStatus.NOT_FOUND, controller.historico(99L).getStatusCode());
        verify(applicationService, never()).consultarHistorico(anyLong());
    }

    @Test
    @DisplayName("Deve listar OS por cliente")
    void shouldListByClient() {
        when(applicationService.listarPorCliente("12345678909")).thenReturn(List.of(ordem()));

        ResponseEntity<List<OrdemServico>> response = controller.listarPorCliente("12345678909");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("Deve atualizar status da OS")
    void shouldUpdateStatus() {
        OrdemServico atualizada = ordem();
        atualizada.setStatus(StatusOS.APROVADA);
        when(applicationService.atualizarStatus(1L, StatusOS.APROVADA)).thenReturn(atualizada);

        ResponseEntity<OrdemServico> response = controller.atualizarStatus(1L, StatusOS.APROVADA);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(StatusOS.APROVADA, response.getBody().getStatus());
    }

    @Test
    @DisplayName("Deve retornar 404 ao atualizar status de OS inexistente")
    void shouldReturnNotFoundOnStatusUpdate() {
        when(applicationService.atualizarStatus(99L, StatusOS.APROVADA)).thenReturn(null);

        assertEquals(HttpStatus.NOT_FOUND,
            controller.atualizarStatus(99L, StatusOS.APROVADA).getStatusCode());
    }
}
