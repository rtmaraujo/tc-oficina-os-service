# language: pt
Funcionalidade: Ciclo de vida completo da Ordem de Servico (Saga)
  Como atendente da oficina
  Quero acompanhar a Ordem de Servico do orcamento ate a finalizacao
  Para garantir que os eventos da saga sejam publicados em cada etapa

  Cenario: Fluxo completo de uma OS aprovada, executada e finalizada
    Dado que o servico de OS esta disponivel
    Quando eu abrir uma ordem de servico para o cliente "12345678909" com descricao "Revisao completa"
    Entao a OS deve estar com status "ABERTA"
    E o evento "os-created" deve ter sido publicado
    Quando eu aprovar o orcamento da OS
    Entao a OS deve estar com status "APROVADA"
    E o evento "budget-approved" deve ter sido publicado
    Quando eu iniciar a execucao da OS
    Entao a OS deve estar com status "EXECUCAO"
    E o evento "order-finalized" deve ter sido publicado
    Quando eu concluir a execucao da OS
    Entao a OS deve estar com status "CONCLUIDA"
    E o evento "order-completed" deve ter sido publicado
    Quando eu finalizar a OS
    Entao a OS deve estar com status "FINALIZADA"
    E nenhum novo evento deve ter sido publicado

  Cenario: Cancelamento de uma OS antes da execucao
    Dado que o servico de OS esta disponivel
    Quando eu abrir uma ordem de servico para o cliente "98765432100" com descricao "Troca de oleo"
    E eu cancelar a OS
    Entao a OS deve estar com status "CANCELADA"
    E o evento "order-cancelled" deve ter sido publicado
    E o evento "os-created" deve ter sido publicado
