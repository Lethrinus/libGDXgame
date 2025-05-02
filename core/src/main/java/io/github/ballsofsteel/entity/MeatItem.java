package io.github.ballsofsteel.entity;

import com.badlogic.gdx.graphics.Texture;


public class MeatItem extends Item {
    private float healAmount;

    public MeatItem(String name, Texture icon, float healAmount) {
        super(name, icon);
        this.healAmount = healAmount;
    }

    @Override
    public void use(Player player) {
        player.increaseHealth(healAmount);
        player.triggerHealEffect();
        player.getInventory().removeItem(this);
        System.out.println(name + " consumed! Player healed for " + healAmount + " points.");
    }
}
