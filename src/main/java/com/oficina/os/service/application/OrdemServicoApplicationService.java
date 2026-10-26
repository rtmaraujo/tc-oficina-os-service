package com.oficina.os.service.application;

import com.oficina.os.service.domain.OrdemServico;
import com.oficina.os.service.domain.StatusOS;
import com.oficina.os.service.infrastructure.persistence.OrdemServicoHistoricoEntity;

import java.util.List;

public interface OrdemServicoApplicationService {
    OrdemServico criarOrdemServico(String clienteCpf, String descricao);
    OrdemServico atualizarStatus(Long id, StatusOS novoStatus);
    OrdemServico consultarPorId(Long id);
    List<OrdemServico> listarPorCliente(String clienteCpf);
    List<OrdemServicoHistoricoEntity> consultarHistorico(Long id);
}
