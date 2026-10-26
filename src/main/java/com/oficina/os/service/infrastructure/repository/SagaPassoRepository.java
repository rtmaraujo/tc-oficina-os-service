package com.oficina.os.service.infrastructure.repository;

import com.oficina.os.service.infrastructure.persistence.SagaPassoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SagaPassoRepository extends JpaRepository<SagaPassoEntity, Long> {
    List<SagaPassoEntity> findBySagaIdOrderByTimestampAsc(String sagaId);
}
