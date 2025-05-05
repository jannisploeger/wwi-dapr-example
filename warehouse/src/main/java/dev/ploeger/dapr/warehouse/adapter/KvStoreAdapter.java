package dev.ploeger.dapr.warehouse.adapter;

import dev.ploeger.dapr.warehouse.model.Sweet;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.State;
import io.dapr.utils.TypeRef;
import jakarta.annotation.PreDestroy; // Keep PreDestroy for cleanup
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class KvStoreAdapter {
    private static final String STATE_STORE_NAME = "kvstore";
    private static final String INVENTORY_KEY = "inventory";
    private static final TypeRef<List<Sweet>> SWEET_LIST_TYPE = new TypeRef<>() {};

    private final DaprClient daprClient;



    @PreDestroy
    public void cleanup() {
        if (this.daprClient != null) {
            try {
                log.info("Closing Dapr client...");
                this.daprClient.close();
                log.info("Dapr client closed.");
            } catch (Exception e) {
                log.error("Error closing Dapr client", e);
            }
        }
    }


    /**
     * Lädt den aktuellen Süßigkeitenbestand aus dem Key-Value Store reaktiv.
     * Falls der Bestand leer ist oder der Key nicht existiert, wird der Store
     * mit Standardwerten initialisiert und diese zurückgegeben.
     *
     * @return Ein Mono, das die Liste aller verfügbaren Süßigkeiten enthält oder ein Fehler-Signal (gibt leere Liste bei Fehler).
     */
    public Mono<List<Sweet>> getInventory() {
        return daprClient.getState(STATE_STORE_NAME, INVENTORY_KEY, SWEET_LIST_TYPE)
                .map(State::getValue) // Extract the value from the State object
                // Filter out null or empty lists after getting the state value
                .filter(inventory -> inventory != null && !inventory.isEmpty())
                // If the Mono is empty after map/filter (key not found, value was null, or value was empty list),
                // switch to the initialization logic.
                .onErrorResume(e -> {
                    // This catches errors during the initial getState call
                    log.error("Fehler beim Laden des Inventars aus dem KV-Store", e);
                    // Return an empty list wrapped in a Mono on general fetch error
                    return Mono.just(new ArrayList<>());
                });
    }
    /**
     * Fügt eine neue Süßigkeit zum Inventar hinzu.
     * Diese Methode ist ein Wrapper für addOrUpdateSweet, speziell für das Hinzufügen.
     *
     * @param sweet Die hinzuzufügende Süßigkeit
     * @return Ein Mono, das true bei Erfolg enthält, oder ein Fehler-Signal.
     */
    public Mono<Boolean> addToInventory(Sweet sweet) {
        return addOrUpdateSweet(sweet);
    }
    /**
     * Aktualisiert den Bestand einer bestimmten Süßigkeit im KV-Store reaktiv.
     * Liest den aktuellen Zustand (ggf. initialisiert), modifiziert ihn und speichert ihn zurück.
     * ACHTUNG: Nicht atomar ohne ETag-Nutzung.
     *
     * @param sweetName Name der Süßigkeit
     * @param quantity  Neue Menge
     * @return Ein Mono, das true bei Erfolg enthält, false wenn die Süßigkeit nicht gefunden wurde, oder ein Fehler-Signal.
     */
    public Mono<Boolean> updateSweetQuantity(String sweetName, int quantity) {
        // getInventory now handles initialization if needed
        return getInventory()
                .flatMap(inventory -> {
                    Optional<Sweet> sweetToUpdate = inventory.stream()
                            .filter(s -> s.name().equals(sweetName))
                            .findFirst();

                    if (sweetToUpdate.isPresent()) {
                        // Create a *new* list with the updated item
                        List<Sweet> updatedInventory = inventory.stream()
                                .map(s -> s.name().equals(sweetName) ? new Sweet(s.name(), s.price(), quantity) : s)
                                .collect(Collectors.toList());

                        // Save the updated list
                        // Consider adding ETag handling here for optimistic concurrency control
                        return daprClient.saveState(STATE_STORE_NAME, INVENTORY_KEY, updatedInventory)
                                .thenReturn(true) // Return true on successful save
                                .onErrorResume(e -> {
                                    log.error("Fehler beim Speichern des aktualisierten Inventars im KV-Store", e);
                                    return Mono.just(false); // Indicate failure on save error
                                });
                    } else {
                        // Sweet not found
                        log.warn("Süßigkeit '{}' nicht im Inventar gefunden für Update.", sweetName);
                        return Mono.just(false);
                    }
                })
                .onErrorResume(e -> {
                    log.error("Fehler beim Aktualisieren der Süßigkeitenmenge (vor dem Speichern)", e);
                    return Mono.just(false); // Indicate failure on fetch error or processing error
                });
    }

    /**
     * Fügt eine neue Süßigkeit zum Inventar hinzu oder aktualisiert eine bestehende reaktiv.
     * Liest den aktuellen Zustand (ggf. initialisiert), modifiziert ihn und speichert ihn zurück.
     * ACHTUNG: Nicht atomar ohne ETag-Nutzung.
     *
     * @param sweet Die hinzuzufügende oder zu aktualisierende Süßigkeit
     * @return Ein Mono, das true bei Erfolg enthält, oder ein Fehler-Signal.
     */
    public Mono<Boolean> addOrUpdateSweet(Sweet sweet) {
        // getInventory now handles initialization if needed
        return getInventory()
                .flatMap(inventory -> {
                    boolean exists = inventory.stream().anyMatch(s -> s.name().equals(sweet.name()));
                    List<Sweet> updatedInventory;

                    if (exists) {
                        // Update existing: create a new list with the sweet updated
                        updatedInventory = inventory.stream()
                                .map(s -> s.name().equals(sweet.name()) ? sweet : s) // Replace existing
                                .collect(Collectors.toList());
                    } else {
                        // Add new: create a new list with the new sweet added
                        updatedInventory = new ArrayList<>(inventory);
                        updatedInventory.add(sweet);
                    }

                    // Save the modified list
                    // Consider adding ETag handling here for optimistic concurrency control
                    return daprClient.saveState(STATE_STORE_NAME, INVENTORY_KEY, updatedInventory)
                            .thenReturn(true) // Return true on success
                            .onErrorResume(e -> {
                                log.error("Fehler beim Speichern des Inventars nach Add/Update im KV-Store", e);
                                return Mono.just(false); // Indicate failure
                            });
                })
                .onErrorResume(e -> {
                    log.error("Fehler beim Hinzufügen/Aktualisieren der Süßigkeit (vor dem Speichern)", e);
                    return Mono.just(false); // Indicate failure
                });
    }


}