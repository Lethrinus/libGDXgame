package io.github.ballsofsteel.entity;

import com.badlogic.gdx.graphics.Texture;

/**
 * basic item class
 * override the use method
 */
public class Item {
    protected String name;
    protected Texture icon;

    public Item(String name, Texture icon) {
        this.name = name;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public Texture getIcon() {
        return icon;
    }

    /**
     * player using the item
     * override lower class
     */
    public void use(Player player) {
        System.out.println(name + " item is used (Item base class)!");
    }
}
