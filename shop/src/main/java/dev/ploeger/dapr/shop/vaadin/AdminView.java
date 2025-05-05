package dev.ploeger.dapr.shop.vaadin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.Route;
import dev.ploeger.dapr.shop.adapter.WarehouseAdapter;
import dev.ploeger.dapr.shop.model.Sweet;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Route("admin")
public class AdminView extends VerticalLayout {

    private final WarehouseAdapter warehouseAdapter;
    private final Grid<Sweet> inventoryGrid;
    private final List<Sweet> inventoryItems = new ArrayList<>();
    private final TextField nameField;
    private final NumberField priceField;
    private final NumberField quantityField;

    public AdminView(@Autowired WarehouseAdapter warehouseAdapter) {
        this.warehouseAdapter = warehouseAdapter;

        H1 title = new H1("Warehouse Administration");

        // Formular zum Hinzufügen neuer Süßigkeiten
        H3 addSweetTitle = new H3("Neue Süßigkeit hinzufügen");
        nameField = new TextField("Name");
        nameField.setRequired(true);

        priceField = new NumberField("Preis (€)");
        priceField.setMin(0.01);
        priceField.setStep(0.01);
        priceField.setValue(1.99);
        priceField.setRequiredIndicatorVisible(true);

        quantityField = new NumberField("Menge");
        quantityField.setMin(1);
        quantityField.setStep(1);
        quantityField.setValue(10.0);
        quantityField.setRequiredIndicatorVisible(true);

        Button addButton = new Button("Hinzufügen");
        addButton.addClickListener(e -> addSweet());

        HorizontalLayout formLayout = new HorizontalLayout(nameField, priceField, quantityField, addButton);
        formLayout.setDefaultVerticalComponentAlignment(Alignment.END);

        // Grid für die Anzeige des aktuellen Inventars
        H3 inventoryTitle = new H3("Aktuelles Inventar");
        inventoryGrid = new Grid<>();
        inventoryGrid.setDataProvider(new ListDataProvider<>(inventoryItems));
        inventoryGrid.addColumn(Sweet::name).setHeader("Name");
        inventoryGrid.addColumn(Sweet::price).setHeader("Preis (€)");
        inventoryGrid.addColumn(Sweet::quantity).setHeader("Verfügbar");

        Button refreshButton = new Button("Inventar aktualisieren", e -> loadInventory());

        // Layout zusammenstellen
        setSpacing(true);
        setPadding(true);

        VerticalLayout formSection = new VerticalLayout(addSweetTitle, formLayout);
        VerticalLayout inventorySection = new VerticalLayout(inventoryTitle, inventoryGrid, refreshButton);

        add(title, formSection, inventorySection);

        // Initial Inventar laden
        loadInventory();
    }

    private void addSweet() {
        if (nameField.isEmpty() || priceField.isEmpty() || quantityField.isEmpty()) {
            Notification.show("Bitte alle Felder ausfüllen", 3000, Notification.Position.MIDDLE);
            return;
        }

        String name = nameField.getValue();
        double price = priceField.getValue();
        int quantity = quantityField.getValue().intValue();

        Sweet newSweet = new Sweet(name, price, quantity);

        try {
            boolean success = warehouseAdapter.addToInventory(newSweet);
            if (success) {
                Notification.show("Süßigkeit erfolgreich hinzugefügt", 3000, Notification.Position.MIDDLE);
                nameField.clear();
                priceField.setValue(1.99);
                quantityField.setValue(10.0);

                // Inventar neu laden, um die Änderungen zu sehen
                loadInventory();
            } else {
                Notification.show("Fehler beim Hinzufügen der Süßigkeit", 3000, Notification.Position.MIDDLE);
            }
        } catch (Exception ex) {
            Notification.show("Fehler: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }

    private void loadInventory() {
        try {
            inventoryItems.clear();
            inventoryItems.addAll(warehouseAdapter.getInventory());
            inventoryGrid.getDataProvider().refreshAll();
            Notification.show("Inventar erfolgreich aktualisiert", 1500, Notification.Position.BOTTOM_START);
        } catch (Exception ex) {
            Notification.show("Fehler beim Laden des Inventars: " + ex.getMessage(),
                    3000, Notification.Position.MIDDLE);
        }
    }
}