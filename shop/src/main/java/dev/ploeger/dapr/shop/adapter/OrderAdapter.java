package dev.ploeger.dapr.shop.adapter;

import dev.ploeger.dapr.shop.model.Order;
import io.dapr.client.DaprClient;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderAdapter {
    private static final String PUBSUB_NAME = "pubsub";
    private static final String TOPIC_NAME = "orders";

    @Autowired
    private DaprClient daprClient;

    /**
     * Veröffentlicht eine Bestellung über die Dapr PubSub-Komponente.
     *
     * @param order Die zu veröffentlichende Bestellung
     * @return true, wenn die Bestellung erfolgreich veröffentlicht wurde
     */
    @SneakyThrows
    public boolean publishOrder(Order order) {
        try {
            // Veröffentliche die Bestellung auf dem "orders"-Topic über die "pubsub"-Komponente
            daprClient.publishEvent(PUBSUB_NAME, TOPIC_NAME, order).block();
            return true;
        } catch (Exception e) {
            // Log the error
            System.err.println("Fehler beim Veröffentlichen der Bestellung: " + e.getMessage());
            return false;
        }
    }
}