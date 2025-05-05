package dev.ploeger.dapr.shop.adapter;

import dev.ploeger.dapr.shop.model.Sweet;
import dev.ploeger.dapr.shop.model.Sweets;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import io.dapr.serializer.DefaultObjectSerializer;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WarehouseAdapter {
    @Autowired
    private DaprClient daprClient;

    @SneakyThrows
    public List<Sweet> getInventory() {
        byte[] bytes = daprClient.invokeMethod("warehouse", "inventory", null, HttpExtension.GET, null, byte[].class).block();
        List<Sweet> list = (new DefaultObjectSerializer()).deserialize(bytes, Sweets.class);
        return list;
    }

    @SneakyThrows
    public boolean addToInventory(Sweet sweet) {

        try {
            // Pass the 'sweet' object directly
            byte[] response = daprClient.invokeMethod(
                    "warehouse",           // Target service app-id
                    "inventory",           // Target method name
                    sweet,                 // Request body object (NOT byte[])
                    HttpExtension.POST,         // HTTP Method (POST)
                    // Optional: Map<String, String> metadata,
                    byte[].class           // Expected response type
            ).block();

            return true;
        } catch (Exception e) {
            System.err.println("Error adding to inventory: " + e.getMessage());
            return false;
        }
    }

}

