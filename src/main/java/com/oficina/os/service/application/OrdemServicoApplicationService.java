package com.oficina.os.service.application;

import com.oficina.os.service.domain.OrdemServico;
import com.oficina.os.service.domain.StatusOS;

public interface OrdemServicoApplicationService {
    OrdemServico criarOrdemServico(String clienteCpf, String descricao);
    OrdemServico atualizarStatus(Long id, StatusOS novoStatus);
    OrdemServico consultarPorId(Long id);
    java.util.List<OrdemServico> listarPorCliente(String clienteCpf);
}
