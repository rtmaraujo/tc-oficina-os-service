package com.oficina.os.service.infrastructure.repository;

import com.oficina.os.service.domain.OrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {
    OrdemServico findByNumeroOS(String numeroOS);
    java.util.List<OrdemServico> findByClienteCpf(String clienteCpf);
}
