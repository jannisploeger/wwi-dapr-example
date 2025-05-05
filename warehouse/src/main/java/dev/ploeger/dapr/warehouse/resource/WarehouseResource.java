package dev.ploeger.dapr.warehouse.resource;

import dev.ploeger.dapr.warehouse.adapter.KvStoreAdapter;
import dev.ploeger.dapr.warehouse.model.Sweet;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class WarehouseResource {

    private final KvStoreAdapter kvStoreAdapter;

    @GetMapping("/inventory")
    public List<Sweet> getInventory() {
        return kvStoreAdapter.getInventory().block();
    }

    @PostMapping("/inventory")
    public ResponseEntity<String> addToInventory(@RequestBody Sweet sweet) {
        try {
            kvStoreAdapter.addToInventory(sweet).block();
            return new ResponseEntity<>("Artikel erfolgreich hinzugefügt", HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("Fehler beim Hinzufügen: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}