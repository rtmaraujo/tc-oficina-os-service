package com.oficina.os.service.infrastructure.repository;

import com.oficina.os.service.infrastructure.persistence.SagaInstanciaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SagaInstanciaRepository extends JpaRepository<SagaInstanciaEntity, Long> {
    Optional<SagaInstanciaEntity> findBySagaId(String sagaId);
    Optional<SagaInstanciaEntity> findByOsId(Long osId);
    List<SagaInstanciaEntity> findByStatusAndDataAtualizacaoBefore(String status, LocalDateTime limite);
}
