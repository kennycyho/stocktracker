package org.example.notifier;

import org.example.dto.Product;

import java.util.List;

public interface Notifier {

    void send(String title, List<Product> productList);
}
