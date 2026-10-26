package com.oficina.os.service.application.impl;

import com.oficina.os.service.application.OrdemServicoApplicationService;
import com.oficina.os.service.application.saga.SagaOrchestrator;
import com.oficina.os.service.domain.OrdemServico;
import com.oficina.os.service.domain.StatusOS;
import com.oficina.os.service.infrastructure.persistence.OrdemServicoEntity;
import com.oficina.os.service.infrastructure.persistence.OrdemServicoHistoricoEntity;
import com.oficina.os.service.infrastructure.repository.OrdemServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrdemServicoApplicationServiceImpl implements OrdemServicoApplicationService {

    private final OrdemServicoRepository repository;
    private final SagaOrchestrator sagaOrchestrator;

    public OrdemServicoApplicationServiceImpl(OrdemServicoRepository repository,
                                              SagaOrchestrator sagaOrchestrator) {
        this.repository = repository;
        this.sagaOrchestrator = sagaOrchestrator;
    }

    @Override
    @Transactional
    public OrdemServico criarOrdemServico(String clienteCpf, String descricao) {
        OrdemServico os = new OrdemServico(null, "OS-" + System.currentTimeMillis(),
            descricao, StatusOS.ABERTA, clienteCpf, 0.0);
        OrdemServicoEntity saved = repository.save(new OrdemServicoEntity(os));

        sagaOrchestrator.registrarHistorico(saved, null, StatusOS.ABERTA.name(),
            "Ordem de servico criada", null);
        sagaOrchestrator.iniciarSaga(saved);
        return saved.toDomain();
    }

    @Override
    @Transactional
    public OrdemServico atualizarStatus(Long id, StatusOS novoStatus) {
        Optional<OrdemServicoEntity> optional = repository.findById(id);
        if (optional.isEmpty()) {
            return null;
        }
        OrdemServicoEntity entity = optional.get();

        if (novoStatus == StatusOS.CANCELADA) {
            return sagaOrchestrator.compensarPorOs(id, "Cancelamento solicitado manualmente");
        }
        if (novoStatus == StatusOS.FINALIZADA) {
            return sagaOrchestrator.finalizar(entity, "Finalizacao manual da OS").toDomain();
        }

        sagaOrchestrator.registrarHistorico(entity, entity.getStatus(), novoStatus.name(),
            "Atualizacao manual de status", null);
        entity.setStatus(novoStatus);
        entity.setDataAtualizacao(LocalDateTime.now());
        return repository.save(entity).toDomain();
    }

    @Override
    public OrdemServico consultarPorId(Long id) {
        return repository.findById(id).map(OrdemServicoEntity::toDomain).orElse(null);
    }

    @Override
    public List<OrdemServico> listarPorCliente(String clienteCpf) {
        return repository.findByClienteCpf(clienteCpf).stream()
            .map(OrdemServicoEntity::toDomain)
            .toList();
    }

    @Override
    public List<OrdemServicoHistoricoEntity> consultarHistorico(Long id) {
        return sagaOrchestrator.historico(id);
    }
}
