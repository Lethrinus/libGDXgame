package io.github.theknightscrusade.ui;

import io.github.theknightscrusade.entity.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * basic inventory class 4 slots
 */
public class Inventory {
    private List<Item> items;
    private int maxSlots = 4;

    public Inventory() {
        items = new ArrayList<>();
    }

    public boolean addItem(Item item) {
        if (items.size() >= maxSlots) {
            return false; // full invent
        }
        items.add(item);
        return true;
    }

    public void removeItem(Item item) {
        items.remove(item);
    }

    public List<Item> getItems() {
        return items;
    }

    public int getMaxSlots() {
        return maxSlots;
    }
}
