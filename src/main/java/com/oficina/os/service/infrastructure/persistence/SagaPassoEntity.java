package com.oficina.os.service.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "saga_passo")
public class SagaPassoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saga_id", nullable = false)
    private String sagaId;

    @Column(name = "passo", nullable = false)
    private String passo;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "detalhe", length = 500)
    private String detalhe;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public SagaPassoEntity() {
        this.timestamp = LocalDateTime.now();
    }

    public SagaPassoEntity(String sagaId, String passo, String status, String detalhe) {
        this();
        this.sagaId = sagaId;
        this.passo = passo;
        this.status = status;
        this.detalhe = detalhe;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSagaId() { return sagaId; }
    public void setSagaId(String sagaId) { this.sagaId = sagaId; }
    public String getPasso() { return passo; }
    public void setPasso(String passo) { this.passo = passo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDetalhe() { return detalhe; }
    public void setDetalhe(String detalhe) { this.detalhe = detalhe; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
