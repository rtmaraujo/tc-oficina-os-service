package com.oficina.os.service.config;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaListenerOs {

    // Consumir budget-approved do Billing Service
    @KafkaListener(topics = "budget-approved", groupId = "os-service-group")
    public void handleBudgetApproved(String message) {
        System.out.println("OS Service received budget-approved: " + message);
        // Aqui seria lógica para aprovar orçamento da OS
        // Ex: atualizar status da OS para APROVADA
    }

    // Consumir order-finalized do Execution Service
    @KafkaListener(topics = "order-finalized", groupId = "os-service-group")
    public void handleOrderFinalized(String message) {
        System.out.println("OS Service received order-finalized: " + message);
        // Aqui seria lógica para marcar OS como finalizada
    }
}
