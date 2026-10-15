package com.oficina.os.service.infrastructure.persistence;

import com.oficina.os.service.domain.OrdemServico;
import com.oficina.os.service.domain.StatusOS;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ordem_servico")
public class OrdemServicoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_os", unique = true, nullable = false)
    private String numeroOS;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusOS status;

    @Column(name = "cliente_cpf", nullable = false, length = 14)
    private String clienteCpf;

    @Column(name = "orcamento")
    private Double orcamento;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    // Default constructor
    public OrdemServicoEntity() {
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = this.dataCriacao;
    }

    // Constructor from domain
    public OrdemServicoEntity(OrdemServico dominio) {
        this.id = dominio.getId();
        this.numeroOS = dominio.getNumeroOS();
        this.descricao = dominio.getDescricao();
        this.status = dominio.getStatus();
        this.clienteCpf = dominio.getClienteCpf();
        this.orcamento = dominio.getOrcamento();
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = this.dataCriacao;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumeroOS() { return numeroOS; }
    public void setNumeroOS(String numeroOS) { this.numeroOS = numeroOS; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public StatusOS getStatus() { return status; }
    public void setStatus(StatusOS status) { this.status = status; }
    public String getClienteCpf() { return clienteCpf; }
    public void setClienteCpf(String clienteCpf) { this.clienteCpf = clienteCpf; }
    public Double getOrcamento() { return orcamento; }
    public void setOrcamento(Double orcamento) { this.orcamento = orcamento; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }

    // Convert to domain
    public OrdemServico toDomain() {
        OrdemServico dominio = new OrdemServico();
        dominio.setId(this.id);
        dominio.setNumeroOS(this.numeroOS);
        dominio.setDescricao(this.descricao);
        dominio.setStatus(this.status);
        dominio.setClienteCpf(this.clienteCpf);
        dominio.setOrcamento(this.orcamento);
        dominio.setDataCriacao(this.dataCriacao.toString());
        dominio.setDataAtualizacao(this.dataAtualizacao.toString());
        return dominio;
    }
}
