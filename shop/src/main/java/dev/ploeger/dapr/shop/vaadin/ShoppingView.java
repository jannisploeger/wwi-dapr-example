package dev.ploeger.dapr.shop.vaadin;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class ShoppingView extends VerticalLayout {
    public ShoppingView() {
        // Initialize the view
        add(new H1("Welcome to the Shopping View"));
        // Additional components and logic can be added here
    }

    // Additional methods and components can be added here
}
