package io.github.some_example_name;

import com.badlogic.gdx.graphics.Texture;

/**
 * meat item example.
 */
public class MeatItem extends Item {
    private float healAmount;

    public MeatItem(String name, Texture icon, float healAmount) {
        super(name, icon);
        this.healAmount = healAmount;
    }

    @Override
    public void use(Player player) {
        // increase hp
        player.increaseHealth(healAmount);

        System.out.println("Ate " + name + " -> +" + healAmount + " HP!");

        // if one use remove the item
        player.getInventory().removeItem(this);
    }
}
