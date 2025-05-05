package dev.ploeger.dapr.billing.adapter;

import dev.ploeger.dapr.billing.model.Order;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Der @RestController in der OrderAdapter-Klasse der Billing-Anwendung ist notwendig, da Dapr die Subscription für PubSub-Nachrichten über HTTP-Endpunkte implementiert. Das funktioniert folgendermaßen:
 *
 *
 * Die @Topic-Annotation definiert, dass diese Methode Nachrichten vom angegebenen Topic empfangen soll.
 * Bei der Initialisierung sucht der Dapr-Sidecar nach HTTP-Endpunkten, die mit @Topic annotiert sind.
 * Der Dapr-Sidecar abonniert dann das angegebene Topic und leitet empfangene Nachrichten an den entsprechenden HTTP-Endpunkt der Anwendung weiter.
 * Obwohl wir keine direkte REST-API für externe Clients anbieten, benötigt Dapr diese HTTP-Schnittstelle für die interne Kommunikation zwischen dem Dapr-Sidecar und der Anwendung. Der Sidecar fungiert als Proxy und sendet die empfangenen Nachrichten vom PubSub-System als HTTP-POST-Requests an den definierten Endpunkt unserer Anwendung.
 *
 * Diese Architektur ermöglicht eine saubere Trennung zwischen der Nachrichtenempfangslogik (durch Dapr verwaltet) und der Geschäftslogik unserer Anwendung.
 */
@RestController
@Slf4j
public class OrderAdapter {
    private static final String PUBSUB_NAME = "pubsub";
    private static final String TOPIC_NAME = "orders";

    /**
     * Empfängt eine Bestellung, die über die Dapr PubSub-Komponente veröffentlicht wurde.
     *
     * @param cloudEvent Das CloudEvent-Objekt, das die Bestellung enthält
     * @return Die empfangene Bestellung
     */
    @Topic(name = TOPIC_NAME, pubsubName = PUBSUB_NAME)
    @PostMapping(path = "/orders", consumes = MediaType.ALL_VALUE)
    public Order receiveOrder(@RequestBody CloudEvent<Order> cloudEvent) {
        Order order = cloudEvent.getData();
        log.info("Bestellung empfangen: {}", order);

        // Hier könnte die Logik zur Verarbeitung der Bestellung implementiert werden
        processOrder(order);

        return order;
    }

    /**
     * Verarbeitet eine empfangene Bestellung.
     * Diese Methode kann je nach Geschäftsanforderungen erweitert werden.
     *
     * @param order Die zu verarbeitende Bestellung
     */
    private void processOrder(Order order) {
        // Beispiel für eine einfache Verarbeitungslogik
        double totalAmount = order.sweets().stream()
                .mapToDouble(sweet -> sweet.price() * sweet.quantity())
                .sum();

        log.info("Verarbeite Bestellung für Kunde: {}, Gesamtbetrag: {}", order.name(), totalAmount);

        // Weitere Logik wie Rechnungserstellung, Zahlungsverarbeitung usw. könnte hier implementiert werden
    }
}