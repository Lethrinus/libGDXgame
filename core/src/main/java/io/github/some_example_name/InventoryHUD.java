package io.github.some_example_name;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;;

public class InventoryHUD {

    private OrthographicCamera hudCamera;
    private SpriteBatch batch;

    private Texture inventoryBg;
    private Texture slotNormal;
    private Texture slotSelected;

    // HUD configuration
    private int selectedSlot = 0;  // which slot selected
    private final int totalSlots = 4;

    private final float bgWidth = 425;
    private final float bgHeight = 100;
    private final float slotSize = 75;
    private final float spacing = 10;

    /**
     * hud camera and batch
     */
    public void initializeCamera(float viewportWidth, float viewportHeight) {
        hudCamera = new OrthographicCamera();
        hudCamera.setToOrtho(false, viewportWidth, viewportHeight);
        batch = new SpriteBatch();
    }

    /**
     * load the pngs for usage in hud
     */
    public void loadTextures() {
        inventoryBg  = new Texture("HUD/inventory_bg.png");   // HUD bg
        slotNormal   = new Texture("HUD/slot_normal.png");     // normalslot
        slotSelected = new Texture("HUD/slot_selected.png");   // selectedslot
    }

    /**
     hud drawing
     checks inventory in player
     */
    public void drawHUD(Player player) {
        if (hudCamera == null || batch == null) return;

        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();

        // center background
        float bgX = (hudCamera.viewportWidth - bgWidth) / 2f;
        float bgY = 20;
        batch.draw(inventoryBg, bgX, bgY, bgWidth, bgHeight);

        // draw slots
        float totalWidthAllSlots = totalSlots * slotSize + (totalSlots - 1) * spacing;
        float slotsStartX = bgX + (bgWidth - totalWidthAllSlots) / 2f;
        float slotsY = bgY + (bgHeight - slotSize) / 2f;

        // list of the items in inventory
        // example player.getInventory().getItems()
        for (int i = 0; i < totalSlots; i++) {
            // draw the slot background
            float xPos = slotsStartX + i * (slotSize + spacing);
            if (i == selectedSlot) {
                batch.draw(slotSelected, xPos, slotsY, slotSize, slotSize);
            } else {
                batch.draw(slotNormal, xPos, slotsY, slotSize, slotSize);
            }

            // if there is item towards to this slot draw it
            // (i < inventory.size() control)
            if (i < player.getInventory().getItems().size()) {
                Item item = player.getInventory().getItems().get(i);
                if (item != null && item.getIcon() != null) {
                    // for centering the icon adding offset
                    float iconSize = 160;
                    if(i == selectedSlot) {
                        iconSize += 125;
                    }
                    float iconX = xPos + (slotSize - iconSize) / 2.2f;
                    float iconY = slotsY + (slotSize - iconSize) /2f;
                    batch.draw(item.getIcon(), iconX, iconY, iconSize, iconSize);
                }
            }
        }

        batch.end();
    }

    /**
     * slot selecting (currently just keyboard)
     */
    public void setSelectedSlot(int slotIndex) {
        if (slotIndex < 0) slotIndex = 0;
        if (slotIndex >= totalSlots) slotIndex = totalSlots - 1;
        this.selectedSlot = slotIndex;
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public void nextSlot() {
        selectedSlot++;
        if (selectedSlot >= totalSlots) {
            selectedSlot = 0;
        }
    }

    public void prevSlot() {
        selectedSlot--;
        if (selectedSlot < 0) {
            selectedSlot = totalSlots - 1;
        }
    }

    public void dispose() {
        if (batch != null) batch.dispose();
        if (inventoryBg != null) inventoryBg.dispose();
        if (slotNormal != null) slotNormal.dispose();
        if (slotSelected != null) slotSelected.dispose();
    }
}
