package io.github.ballsofsteel.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.ballsofsteel.entity.Item;
import io.github.ballsofsteel.entity.Player;;

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


    private float[] slotScales;      // Her slot için ayrı scale değeri
    private final float normalScale = 1f;  // Normal slot boyutu
    private final float selectedScale = 1.15f; // Seçili slot için boyut
    private final float scaleLerpSpeed = 3.5f;
    private float stateTime = 0f;
    /**
     * hud camera and batch
     */
    public void initializeCamera(float viewportWidth, float viewportHeight) {
        hudCamera = new OrthographicCamera();
        hudCamera.setToOrtho(false, viewportWidth, viewportHeight);
        batch = new SpriteBatch();

        slotScales = new float[totalSlots];
        for (int i = 0; i < totalSlots; i++) {
            slotScales[i] = normalScale;
        }
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

        float delta = Gdx.graphics.getDeltaTime();
        stateTime += delta;

        // Sinüs dalgası oluştur: Değer 0.9 - 1.1 arası değişir
        float sinWave = 1.0f + 0.1f * (float)Math.sin(stateTime * 3.0f);

        float bgX = (hudCamera.viewportWidth - bgWidth) / 2f;
        float bgY = 20;
        batch.draw(inventoryBg, bgX, bgY, bgWidth, bgHeight);

        float totalWidthAllSlots = totalSlots * slotSize + (totalSlots - 1) * spacing;
        float slotsStartX = bgX + (bgWidth - totalWidthAllSlots) / 2f;
        float slotsY = bgY + (bgHeight - slotSize) / 2f;

        for (int i = 0; i < totalSlots; i++) {
            float xPos = slotsStartX + i * (slotSize + spacing);

            float targetScale = (i == selectedSlot) ? selectedScale * sinWave : normalScale;

            // Lerp ile geçişli scale değeri
            slotScales[i] += (targetScale - slotScales[i]) * delta * scaleLerpSpeed;

            float currentSlotSize = slotSize * slotScales[i];
            float xSlot = xPos + (slotSize - currentSlotSize) / 2f;
            float ySlot = slotsY + (slotSize - currentSlotSize) / 2f;

            if (i == selectedSlot) {
                batch.draw(slotSelected, xSlot, ySlot, currentSlotSize, currentSlotSize);
            } else {
                batch.draw(slotNormal, xSlot, ySlot, currentSlotSize, currentSlotSize);
            }

            if (i < player.getInventory().getItems().size()) {
                Item item = player.getInventory().getItems().get(i);
                if (item != null && item.getIcon() != null) {
                    float iconSize = 135;
                    if(i == selectedSlot) {
                        iconSize += 35;
                    }

                    float iconX = xSlot + (currentSlotSize - iconSize) / 2f;
                    float iconY = ySlot + (currentSlotSize - iconSize) / 2f;

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
    public OrthographicCamera getCamera(){ return hudCamera; }

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
