package dev.ploeger.dapr.shop.vaadin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import dev.ploeger.dapr.shop.adapter.WarehouseAdapter;
import dev.ploeger.dapr.shop.model.Sweet;
import org.springframework.beans.factory.annotation.Autowired;

@Route("")
public class ShoppingView extends VerticalLayout {

    private Grid<Sweet> grid;
    private final WarehouseAdapter warehouseAdapter;

    public ShoppingView(@Autowired WarehouseAdapter warehouseAdapter) {
        this.warehouseAdapter = warehouseAdapter;

        // UI-Komponenten initialisieren
        H1 title = new H1("Süßigkeiten Shop");
        Button inventarButton = new Button("Verfügbare Süßigkeiten anzeigen");
        grid = new Grid<>(Sweet.class);

        // Grid-Konfiguration
        grid.setColumns("name", "price", "quantity");
        grid.getColumnByKey("name").setHeader("Name");
        grid.getColumnByKey("price").setHeader("Preis (€)");
        grid.getColumnByKey("quantity").setHeader("Verfügbare Menge");
        grid.setVisible(false);

        // Button-Klick-Handler
        inventarButton.addClickListener(e -> {
            try {
                var sweets = warehouseAdapter.getInventory();
                grid.setItems(sweets);
                grid.setVisible(true);
                Notification.show("Inventar erfolgreich abgerufen!");
            } catch (Exception ex) {
                Notification.show("Fehler beim Abrufen des Inventars: " + ex.getMessage(),
                        3000, Notification.Position.MIDDLE);
            }
        });

        // Layout zusammenstellen
        setSpacing(true);
        setPadding(true);
        add(title, inventarButton, grid);
    }
}