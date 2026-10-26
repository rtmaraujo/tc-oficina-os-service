package com.oficina.os.service.infrastructure.repository;

import com.oficina.os.service.infrastructure.persistence.OrdemServicoHistoricoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdemServicoHistoricoRepository extends JpaRepository<OrdemServicoHistoricoEntity, Long> {
    List<OrdemServicoHistoricoEntity> findByOsIdOrderByTimestampAsc(Long osId);
}
