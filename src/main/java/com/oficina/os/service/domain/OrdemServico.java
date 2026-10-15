package com.oficina.os.service.domain;

public class OrdemServico {
    private Long id;
    private String numeroOS;
    private String descricao;
    private StatusOS status;
    private String clienteCpf;
    private double orcamento;
    private String dataCriacao;
    private String dataAtualizacao;

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
    public double getOrcamento() { return orcamento; }
    public void setOrcamento(double orcamento) { this.orcamento = orcamento; }
    public String getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(String dataCriacao) { this.dataCriacao = dataCriacao; }
    public String getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(String dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }

    // Constructors
    public OrdemServico() {}

    public OrdemServico(Long id, String numeroOS, String descricao, StatusOS status, String clienteCpf, double orcamento) {
        this.id = id;
        this.numeroOS = numeroOS;
        this.descricao = descricao;
        this.status = status;
        this.clienteCpf = clienteCpf;
        this.orcamento = orcamento;
        this.dataCriacao = java.time.LocalDateTime.now().toString();
        this.dataAtualizacao = this.dataCriacao;
    }
}
