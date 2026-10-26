package com.oficina.os.service.infrastructure.repository;

import com.oficina.os.service.infrastructure.persistence.OrdemServicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdemServicoRepository extends JpaRepository<OrdemServicoEntity, Long> {
    OrdemServicoEntity findByNumeroOS(String numeroOS);
    List<OrdemServicoEntity> findByClienteCpf(String clienteCpf);
}
