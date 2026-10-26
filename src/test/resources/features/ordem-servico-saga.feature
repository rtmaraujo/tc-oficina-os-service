# language: pt
Funcionalidade: Ciclo de vida completo da Ordem de Servico (Saga orquestrada)
  Como atendente da oficina
  Quero acompanhar a Ordem de Servico do orcamento ate a finalizacao
  Para garantir que cada etapa da saga seja coordenada pelo OS Service

  Cenario: Fluxo completo de uma OS aprovada, paga, executada e finalizada
    Dado que o servico de OS esta disponivel
    Quando eu abrir uma ordem de servico para o cliente "12345678909" com descricao "Revisao completa"
    Entao a OS deve estar com status "ABERTA"
    E o evento "os-created" deve ter sido publicado
    Quando o Billing gerar o orcamento da OS
    Entao a OS deve estar com status "ORCAMENTO_GERADO"
    Quando o cliente aprovar o orcamento
    Entao a OS deve estar com status "APROVADA"
    Quando o pagamento for confirmado
    Entao a OS deve estar com status "PAGA"
    E o evento "execution-requested" deve ter sido publicado
    Quando a execucao iniciar o diagnostico
    Entao a OS deve estar com status "EXECUCAO"
    Quando a execucao for finalizada
    Entao a OS deve estar com status "CONCLUIDA"
    Quando eu finalizar a OS
    Entao a OS deve estar com status "FINALIZADA"

  Cenario: Compensacao quando o orcamento e rejeitado
    Dado que o servico de OS esta disponivel
    Quando eu abrir uma ordem de servico para o cliente "12345678909" com descricao "Troca de motor"
    E o Billing gerar o orcamento da OS
    Quando o cliente rejeitar o orcamento
    Entao a OS deve estar com status "CANCELADA"
    E o evento "compensate-budget" deve ter sido publicado
    E o evento "compensate-execution" deve ter sido publicado
    E o evento "saga-compensated" deve ter sido publicado
