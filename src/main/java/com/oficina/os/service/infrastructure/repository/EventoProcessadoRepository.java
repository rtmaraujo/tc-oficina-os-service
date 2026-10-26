package com.oficina.os.service.infrastructure.repository;

import com.oficina.os.service.infrastructure.persistence.EventoProcessadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoProcessadoRepository extends JpaRepository<EventoProcessadoEntity, String> {
}
