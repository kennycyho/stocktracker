package org.example.notifier;

import org.example.model.Item;

import java.util.List;

public interface StockNotifier {
    
    void send(String title, List<Item> itemList);
}
