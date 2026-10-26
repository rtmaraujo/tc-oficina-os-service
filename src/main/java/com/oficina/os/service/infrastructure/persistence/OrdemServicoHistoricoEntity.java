package com.oficina.os.service.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ordem_servico_historico")
public class OrdemServicoHistoricoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "os_id", nullable = false)
    private Long osId;

    @Column(name = "status_anterior")
    private String statusAnterior;

    @Column(name = "status_novo", nullable = false)
    private String statusNovo;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "saga_id")
    private String sagaId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public OrdemServicoHistoricoEntity() {
        this.timestamp = LocalDateTime.now();
    }

    public OrdemServicoHistoricoEntity(Long osId, String statusAnterior, String statusNovo,
                                       String motivo, String sagaId) {
        this();
        this.osId = osId;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.motivo = motivo;
        this.sagaId = sagaId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOsId() { return osId; }
    public void setOsId(Long osId) { this.osId = osId; }
    public String getStatusAnterior() { return statusAnterior; }
    public void setStatusAnterior(String statusAnterior) { this.statusAnterior = statusAnterior; }
    public String getStatusNovo() { return statusNovo; }
    public void setStatusNovo(String statusNovo) { this.statusNovo = statusNovo; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getSagaId() { return sagaId; }
    public void setSagaId(String sagaId) { this.sagaId = sagaId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
