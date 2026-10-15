package com.oficina.os.service.application.impl;

import com.oficina.os.service.domain.OrdemServico;
import com.oficina.os.service.domain.StatusOS;
import com.oficina.os.service.application.OrdemServicoApplicationService;
import com.oficina.os.service.infrastructure.repository.OrdemServicoRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class OrdemServicoApplicationServiceImpl implements OrdemServicoApplicationService {

    private final OrdemServicoRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrdemServicoApplicationServiceImpl(OrdemServicoRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public OrdemServico criarOrdemServico(String clienteCpf, String descricao) {
        OrdemServico os = new OrdemServico(null, "OS-" + System.currentTimeMillis(), descricao, StatusOS.ABERTA, clienteCpf, 0.0);
        OrdemServico saved = repository.save(os);
        
        // Publicar evento Kafka para iniciar o saga
        kafkaTemplate.send("os-created", saved.getId().toString(), 
            "OrdemServico{" + 
            "id=" + saved.getId() +
            ", numeroOS='" + saved.getNumeroOS() + '\'' +
            ", status=" + saved.getStatus() +
            '}');
        
        return saved;
    }

    @Override
    public OrdemServico atualizarStatus(Long id, StatusOS novoStatus) {
        Optional<OrdemServico> optional = repository.findById(id);
        if (optional.isPresent()) {
            OrdemServico os = optional.get();
            os.setStatus(novoStatus);
            os.setDataAtualizacao(java.time.LocalDateTime.now().toString());
            OrdemServico updated = repository.save(os);
            
            // Publicar evento conforme novo status
            switch (novoStatus) {
                case APROVADA:
                    kafkaTemplate.send("budget-approved", updated.getId().toString(), 
                        "Budget approved for OS " + updated.getNumeroOS());
                    break;
                case EXECUCAO:
                    kafkaTemplate.send("order-finalized", updated.getId().toString(), 
                        "Order execution started for OS " + updated.getNumeroOS());
                    break;
                case CONCLUIDA:
                    kafkaTemplate.send("order-completed", updated.getId().toString(), 
                        "Order completed: " + updated.getNumeroOS());
                    break;
                case CANCELADA:
                    kafkaTemplate.send("order-cancelled", updated.getId().toString(), 
                        "Order cancelled: " + updated.getNumeroOS());
                    break;
                default:
                    break;
            }
            
            return updated;
        }
        return null;
    }

    @Override
    public OrdemServico consultarPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public java.util.List<OrdemServico> listarPorCliente(String clienteCpf) {
        return repository.findByClienteCpf(clienteCpf);
    }
}
