package com.oficina.os.service.bdd;

import com.oficina.os.service.application.impl.OrdemServicoApplicationServiceImpl;
import com.oficina.os.service.domain.OrdemServico;
import com.oficina.os.service.domain.StatusOS;
import com.oficina.os.service.infrastructure.repository.OrdemServicoRepository;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Step definitions do fluxo BDD da Saga de Ordem de Servico.
 *
 * Usa o servico de aplicacao real com um repositorio em memoria e um KafkaTemplate
 * que apenas registra os topicos publicados, permitindo validar o fluxo completo
 * sem depender de infraestrutura externa.
 */
public class OrdemServicoSagaSteps {

    private final Map<Long, OrdemServico> bancoEmMemoria = new LinkedHashMap<>();
    private final List<String> eventosPublicados = new ArrayList<>();
    private final AtomicLong sequenciaId = new AtomicLong(0);

    private OrdemServicoApplicationServiceImpl service;
    private OrdemServico ultimaOrdem;
    private int eventosAntesDaUltimaAcao;

    @Before
    public void prepararServico() {
        bancoEmMemoria.clear();
        eventosPublicados.clear();
        sequenciaId.set(0);
        ultimaOrdem = null;
        eventosAntesDaUltimaAcao = 0;

        OrdemServicoRepository repository = mock(OrdemServicoRepository.class);

        when(repository.save(any(OrdemServico.class))).thenAnswer(invocation -> {
            OrdemServico os = invocation.getArgument(0);
            if (os.getId() == null) {
                os.setId(sequenciaId.incrementAndGet());
            }
            bancoEmMemoria.put(os.getId(), os);
            return os;
        });

        when(repository.findById(any(Long.class)))
            .thenAnswer(invocation -> Optional.ofNullable(bancoEmMemoria.get(invocation.getArgument(0))));

        when(repository.findByClienteCpf(anyString())).thenAnswer(invocation -> {
            String cpf = invocation.getArgument(0);
            return bancoEmMemoria.values().stream()
                .filter(os -> cpf.equals(os.getClienteCpf()))
                .toList();
        });

        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            eventosPublicados.add(invocation.getArgument(0));
            return null;
        });

        service = new OrdemServicoApplicationServiceImpl(repository, kafkaTemplate);
    }

    private void registrarInicioDaAcao() {
        eventosAntesDaUltimaAcao = eventosPublicados.size();
    }

    @Dado("que o servico de OS esta disponivel")
    public void servicoDisponivel() {
        assertNotNull(service);
    }

    @Quando("eu abrir uma ordem de servico para o cliente {string} com descricao {string}")
    public void abrirOrdemServico(String clienteCpf, String descricao) {
        registrarInicioDaAcao();
        ultimaOrdem = service.criarOrdemServico(clienteCpf, descricao);
    }

    @Quando("eu aprovar o orcamento da OS")
    public void aprovarOrcamento() {
        registrarInicioDaAcao();
        ultimaOrdem = service.atualizarStatus(ultimaOrdem.getId(), StatusOS.APROVADA);
    }

    @Quando("eu iniciar a execucao da OS")
    public void iniciarExecucao() {
        registrarInicioDaAcao();
        ultimaOrdem = service.atualizarStatus(ultimaOrdem.getId(), StatusOS.EXECUCAO);
    }

    @Quando("eu concluir a execucao da OS")
    public void concluirExecucao() {
        registrarInicioDaAcao();
        ultimaOrdem = service.atualizarStatus(ultimaOrdem.getId(), StatusOS.CONCLUIDA);
    }

    @Quando("eu finalizar a OS")
    public void finalizarOrdemServico() {
        registrarInicioDaAcao();
        ultimaOrdem = service.atualizarStatus(ultimaOrdem.getId(), StatusOS.FINALIZADA);
    }

    @Quando("eu cancelar a OS")
    public void cancelarOrdemServico() {
        registrarInicioDaAcao();
        ultimaOrdem = service.atualizarStatus(ultimaOrdem.getId(), StatusOS.CANCELADA);
    }

    @Entao("a OS deve estar com status {string}")
    public void osDeveEstarComStatus(String statusEsperado) {
        assertNotNull(ultimaOrdem, "Nenhuma OS foi manipulada no fluxo");
        assertEquals(StatusOS.valueOf(statusEsperado), ultimaOrdem.getStatus());
        assertNotNull(ultimaOrdem.getDataAtualizacao());
    }

    @E("o evento {string} deve ter sido publicado")
    public void eventoDeveTerSidoPublicado(String topico) {
        assertTrue(eventosPublicados.contains(topico),
            "Evento '" + topico + "' nao foi publicado. Eventos: " + eventosPublicados);
    }

    @E("nenhum novo evento deve ter sido publicado")
    public void nenhumNovoEventoDeveTerSidoPublicado() {
        assertEquals(eventosAntesDaUltimaAcao, eventosPublicados.size(),
            "Era esperado nao publicar eventos, mas foram publicados: " + eventosPublicados);
    }
}
