package com.oficina.os.service.presentation;

import com.oficina.os.service.application.OrdemServicoApplicationService;
import com.oficina.os.service.domain.OrdemServico;
import com.oficina.os.service.domain.StatusOS;
import com.oficina.os.service.infrastructure.persistence.OrdemServicoHistoricoEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/os")
public class OrdemServicoController {

    @Autowired
    private OrdemServicoApplicationService applicationService;

    @PostMapping
    public ResponseEntity<OrdemServico> criar(@RequestParam String clienteCpf, @RequestParam String descricao) {
        OrdemServico os = applicationService.criarOrdemServico(clienteCpf, descricao);
        return ResponseEntity.ok(os);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdemServico> consultar(@PathVariable Long id) {
        OrdemServico os = applicationService.consultarPorId(id);
        if (os == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(os);
    }

    @GetMapping("/{id}/historico")
    public ResponseEntity<List<OrdemServicoHistoricoEntity>> historico(@PathVariable Long id) {
        if (applicationService.consultarPorId(id) == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(applicationService.consultarHistorico(id));
    }

    @GetMapping("/cliente/{clienteCpf}")
    public ResponseEntity<List<OrdemServico>> listarPorCliente(@PathVariable String clienteCpf) {
        List<OrdemServico> osList = applicationService.listarPorCliente(clienteCpf);
        return ResponseEntity.ok(osList);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrdemServico> atualizarStatus(@PathVariable Long id, @RequestParam StatusOS novoStatus) {
        OrdemServico os = applicationService.atualizarStatus(id, novoStatus);
        if (os == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(os);
    }
}
