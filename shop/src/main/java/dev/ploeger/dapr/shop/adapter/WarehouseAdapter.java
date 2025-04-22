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

}

