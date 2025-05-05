package dev.ploeger.dapr.shop.vaadin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.Route;
import dev.ploeger.dapr.shop.adapter.OrderAdapter;
import dev.ploeger.dapr.shop.adapter.WarehouseAdapter;
import dev.ploeger.dapr.shop.model.Order;
import dev.ploeger.dapr.shop.model.Sweet;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route("")
public class ShoppingView extends VerticalLayout {

    private Grid<Sweet> shopGrid;
    private Grid<CartItem> cartGrid;
    private final WarehouseAdapter warehouseAdapter;
    private final OrderAdapter orderAdapter;
    private final List<Sweet> availableSweets = new ArrayList<>();
    private final List<CartItem> cartItems = new ArrayList<>();
    private Button orderButton;

    public ShoppingView(@Autowired WarehouseAdapter warehouseAdapter, @Autowired OrderAdapter orderAdapter) {
        this.warehouseAdapter = warehouseAdapter;
        this.orderAdapter = orderAdapter;

        // UI-Komponenten initialisieren
        H1 title = new H1("Süßigkeiten Shop");
        Button inventarButton = new Button("Verfügbare Süßigkeiten anzeigen");

        // Erstelle zwei Grids: eins für den Shop und eins für den Warenkorb
        VerticalLayout shopLayout = createShopLayout();
        VerticalLayout cartLayout = createCartLayout();

        HorizontalLayout mainLayout = new HorizontalLayout(shopLayout, cartLayout);
        mainLayout.setWidthFull();

        // Button-Klick-Handler
        inventarButton.addClickListener(e -> {
            try {
                availableSweets.clear();
                availableSweets.addAll(warehouseAdapter.getInventory());
                shopGrid.setItems(availableSweets);
                shopGrid.setVisible(true);
                Notification.show("Inventar erfolgreich abgerufen!");
            } catch (Exception ex) {
                Notification.show("Fehler beim Abrufen des Inventars: " + ex.getMessage(),
                        3000, Notification.Position.MIDDLE);
            }
        });

        // Layout zusammenstellen
        setSpacing(true);
        setPadding(true);
        add(title, inventarButton, mainLayout);
    }

    private VerticalLayout createShopLayout() {
        VerticalLayout layout = new VerticalLayout();
        H3 shopTitle = new H3("Verfügbare Artikel");

        shopGrid = new Grid<>();
        shopGrid.addColumn(Sweet::name).setHeader("Name");
        shopGrid.addColumn(Sweet::price).setHeader("Preis (€)");
        shopGrid.addColumn(Sweet::quantity).setHeader("Verfügbar");

        shopGrid.addComponentColumn(sweet -> {
            IntegerField quantityField = new IntegerField();
            quantityField.setMin(1);
            quantityField.setMax(sweet.quantity());
            quantityField.setValue(1);
            quantityField.setWidth("100px");

            Button addButton = new Button("Zum Warenkorb");
            addButton.addClickListener(e -> {
                if (quantityField.getValue() != null && quantityField.getValue() > 0
                        && quantityField.getValue() <= sweet.quantity()) {
                    addToCart(sweet, quantityField.getValue());
                } else {
                    Notification.show("Bitte gültige Menge eingeben");
                }
            });

            return new HorizontalLayout(quantityField, addButton);
        }).setHeader("Zum Warenkorb hinzufügen");

        shopGrid.setVisible(false);

        layout.add(shopTitle, shopGrid);
        return layout;
    }

    private VerticalLayout createCartLayout() {
        VerticalLayout layout = new VerticalLayout();
        H3 cartTitle = new H3("Warenkorb");

        cartGrid = new Grid<>();
        cartGrid.setDataProvider(new ListDataProvider<>(cartItems));
        cartGrid.addColumn(CartItem::getName).setHeader("Name");
        cartGrid.addColumn(CartItem::getPrice).setHeader("Preis (€)");
        cartGrid.addColumn(CartItem::getQuantity).setHeader("Menge");
        cartGrid.addColumn(item -> item.getPrice() * item.getQuantity()).setHeader("Gesamt (€)");

        cartGrid.addComponentColumn(item -> {
            Button removeButton = new Button("Entfernen");
            removeButton.addClickListener(e -> removeFromCart(item));
            return removeButton;
        });

        orderButton = new Button("Bestellung aufgeben");
        orderButton.setEnabled(false);
        orderButton.addClickListener(e -> showOrderDialog());

        layout.add(cartTitle, cartGrid, orderButton);
        return layout;
    }

    private void addToCart(Sweet sweet, int quantity) {
        // Prüfe, ob das Item bereits im Warenkorb ist
        boolean found = false;
        for (CartItem item : cartItems) {
            if (item.getName().equals(sweet.name())) {
                item.setQuantity(item.getQuantity() + quantity);
                found = true;
                break;
            }
        }

        // Wenn nicht gefunden, neuen CartItem erstellen
        if (!found) {
            CartItem newItem = new CartItem(sweet.name(), sweet.price(), quantity);
            cartItems.add(newItem);
        }

        // UI aktualisieren
        cartGrid.getDataProvider().refreshAll();
        orderButton.setEnabled(!cartItems.isEmpty());
        Notification.show(quantity + "x " + sweet.name() + " zum Warenkorb hinzugefügt");
    }

    private void removeFromCart(CartItem item) {
        cartItems.remove(item);
        cartGrid.getDataProvider().refreshAll();
        orderButton.setEnabled(!cartItems.isEmpty());
        Notification.show(item.getName() + " aus Warenkorb entfernt");
    }

    private void showOrderDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Bestellinformationen eingeben");

        TextField nameField = new TextField("Name");
        nameField.setRequired(true);

        TextField addressField = new TextField("Adresse");
        addressField.setRequired(true);

        EmailField emailField = new EmailField("E-Mail");
        emailField.setRequired(true);

        TextField phoneField = new TextField("Telefon");
        phoneField.setRequired(true);

        TextField paymentField = new TextField("Zahlungsmethode");
        paymentField.setRequired(true);
        paymentField.setValue("Kreditkarte");

        Button submitButton = new Button("Bestellung absenden", e -> {
            if (nameField.getValue().isEmpty() || addressField.getValue().isEmpty() ||
                    emailField.getValue().isEmpty() || phoneField.getValue().isEmpty() ||
                    paymentField.getValue().isEmpty()) {
                Notification.show("Bitte alle Felder ausfüllen");
                return;
            }

            placeOrder(
                    nameField.getValue(),
                    addressField.getValue(),
                    emailField.getValue(),
                    phoneField.getValue(),
                    paymentField.getValue()
            );

            dialog.close();
        });

        Button cancelButton = new Button("Abbrechen", e -> dialog.close());

        VerticalLayout dialogLayout = new VerticalLayout(
                nameField, addressField, emailField, phoneField, paymentField,
                new HorizontalLayout(cancelButton, submitButton)
        );
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(true);

        dialog.add(dialogLayout);
        dialog.open();
    }

    private void placeOrder(String name, String address, String email, String phone, String paymentMethod) {
        // Konvertiere CartItems zurück zu Sweet-Objekten für die Bestellung
        List<Sweet> orderedSweets = cartItems.stream()
                .map(item -> new Sweet(item.getName(), item.getPrice(), item.getQuantity()))
                .collect(Collectors.toList());

        Order order = new Order(name, address, email, phone, paymentMethod, orderedSweets);

        try {
            boolean success = orderAdapter.publishOrder(order);
            if (success) {
                Notification.show("Bestellung erfolgreich aufgegeben!", 3000, Notification.Position.MIDDLE);
                // Warenkorb leeren
                cartItems.clear();
                cartGrid.getDataProvider().refreshAll();
                orderButton.setEnabled(false);
            } else {
                Notification.show("Fehler beim Aufgeben der Bestellung.", 3000, Notification.Position.MIDDLE);
            }
        } catch (Exception ex) {
            Notification.show("Fehler: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }

    // Innere Klasse für Warenkorb-Items
    private static class CartItem {
        private final String name;
        private final double price;
        private int quantity;

        public CartItem(String name, double price, int quantity) {
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }

        public String getName() {
            return name;
        }

        public double getPrice() {
            return price;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
}