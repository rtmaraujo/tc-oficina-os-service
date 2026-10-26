package com.oficina.os.service.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "saga_instancia")
public class SagaInstanciaEntity {

    public static final String EM_ANDAMENTO = "EM_ANDAMENTO";
    public static final String CONCLUIDA = "CONCLUIDA";
    public static final String COMPENSADA = "COMPENSADA";
    public static final String FALHA = "FALHA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saga_id", unique = true, nullable = false)
    private String sagaId;

    @Column(name = "os_id", nullable = false)
    private Long osId;

    @Column(name = "os_numero", nullable = false)
    private String osNumero;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "passo_atual")
    private String passoAtual;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "data_inicio", nullable = false)
    private LocalDateTime dataInicio;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

    public SagaInstanciaEntity() {
        this.dataInicio = LocalDateTime.now();
        this.dataAtualizacao = this.dataInicio;
    }

    public SagaInstanciaEntity(String sagaId, Long osId, String osNumero) {
        this();
        this.sagaId = sagaId;
        this.osId = osId;
        this.osNumero = osNumero;
        this.status = EM_ANDAMENTO;
        this.passoAtual = "OS_CRIADA";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSagaId() { return sagaId; }
    public void setSagaId(String sagaId) { this.sagaId = sagaId; }
    public Long getOsId() { return osId; }
    public void setOsId(Long osId) { this.osId = osId; }
    public String getOsNumero() { return osNumero; }
    public void setOsNumero(String osNumero) { this.osNumero = osNumero; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPassoAtual() { return passoAtual; }
    public void setPassoAtual(String passoAtual) { this.passoAtual = passoAtual; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public LocalDateTime getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDateTime dataInicio) { this.dataInicio = dataInicio; }
    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }
}
