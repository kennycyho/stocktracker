package org.example.notifier;

import org.example.dto.Item;

import java.util.List;

public interface Notifier {

    void send(String title, List<Item> itemList);
}
