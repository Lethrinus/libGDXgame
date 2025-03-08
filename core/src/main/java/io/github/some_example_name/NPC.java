package io.github.some_example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.MathUtils;

public class NPC {
    // NPC position
    private float x, y;

    // Idle animation for the NPC
    private Animation<TextureRegion> idleAnimation;
    private float stateTime = 0f;

    // Dialogue data
    private String[] dialogues;
    private int currentDialogueIndex = 0;
    private boolean inDialogue = false;

    // Speech bubble texture (simple rectangle)
    private Texture speechBubbleTexture;

    // Font for drawing dialogue text
    private BitmapFont font;

    // Dialogue bubble dimensions
    private float bubbleWidth = 150;
    private float bubbleHeight = 50;

    // Interaction radius for starting dialogue
    private float interactionRadius = 2.0f;

    /**
     * Constructs an NPC.
     * @param x The x position
     * @param y The y position
     * @param dialogues An array of dialogue strings
     */
    public NPC(float x, float y, String[] dialogues) {
        this.x = x;
        this.y = y;
        this.dialogues = dialogues;

        // Load NPC idle animation from Pawn assets using TextureAtlas
        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("Pawn/pawn_animations.atlas"));
        // Assuming the atlas has a region named "idle"
        idleAnimation = new Animation<TextureRegion>(0.2f, atlas.findRegions("pawn_idle"), Animation.PlayMode.LOOP);
        atlas.dispose();

        // Create a simple white texture for the speech bubble
        Pixmap pixmap = new Pixmap((int)bubbleWidth, (int)bubbleHeight, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        speechBubbleTexture = new Texture(pixmap);
        pixmap.dispose();

        // Create a default BitmapFont for dialogue text
        font = new BitmapFont();
        font.setColor(Color.BLACK);
    }

    /**
     * Updates the NPC state.
     * @param delta Time delta
     * @param playerPos The player's position for interaction check
     */
    public void update(float delta, Vector2 playerPos) {
        stateTime += delta;
        // If the player is within interaction radius and presses the E key, progress dialogue
        if (playerPos.dst(x, y) <= interactionRadius && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            inDialogue = true;
            currentDialogueIndex++;
            if (currentDialogueIndex >= dialogues.length) {
                // Dialogue finished; reset or close dialogue as desired
                currentDialogueIndex = 0;
                inDialogue = false;
            }
        }
    }

    /**
     * Renders the NPC and, if active, the dialogue speech bubble.
     * @param batch The SpriteBatch used for rendering
     */
    public void render(SpriteBatch batch) {
        // Render NPC idle animation
        TextureRegion currentFrame = idleAnimation.getKeyFrame(stateTime, true);
        float drawX = x - currentFrame.getRegionWidth() / 2f;
        float drawY = y - currentFrame.getRegionHeight() / 2f;
        batch.draw(currentFrame, drawX, drawY);

        // If dialogue is active, render the speech bubble and current dialogue text
        if (inDialogue) {
            float bubbleX = x - bubbleWidth / 2f;
            float bubbleY = y + currentFrame.getRegionHeight() / 2f + 10; // slightly above the NPC
            batch.draw(speechBubbleTexture, bubbleX, bubbleY, bubbleWidth, bubbleHeight);

            // Draw dialogue text inside the speech bubble (offset for padding)
            String dialogueText = dialogues[currentDialogueIndex];
            float textX = bubbleX + 10;
            float textY = bubbleY + bubbleHeight - 10;
            font.draw(batch, dialogueText, textX, textY);
        }
    }

    /**
     * Disposes of NPC resources.
     */
    public void dispose() {
        speechBubbleTexture.dispose();
        font.dispose();
    }

    public float getX() { return x; }
    public float getY() { return y; }
}
