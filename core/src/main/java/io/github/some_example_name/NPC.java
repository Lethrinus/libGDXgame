package io.github.some_example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;

/**
 * NPC class with an idle animation and a typewriter dialogue effect.
 * The idle animation is built from "Pawn/pawn_animations.png" using the region (2,2,1152,186)
 * split into 6 frames. Dialogue text is revealed gradually using a typewriter effect,
 * and is drawn with a Matrix4 transform (using the bubble’s center as the pivot) so that
 * it is properly centered within a custom speech bubble.
 */
public class NPC {

    private float x, y;
    private float interactionRadius = 2.0f;

    // Custom speech bubble texture (must be a PNG file in assets)
    private Texture bubbleTexture;

    // BitmapFont for dialogue text
    private BitmapFont font;

    // Dialogue lines and current state
    private String[] dialogues;
    private int currentLineIndex = 0;
    private boolean inDialogue = false;

    // Typewriter effect variables
    private float typedSpeed = 20f;   // Characters per second
    private float typedTimer = 0f;    // Time counter
    private int typedIndex = 0;       // Number of characters to show
    private boolean typedComplete = false;
    private String typedText = "";    // Current dialogue text

    // Speech bubble dimensions in world units
    private float bubbleWidth = 4f;
    private float bubbleHeight = 1.2f;

    // Idle animation for NPC (from pawn_animations.png)
    private Animation<TextureRegion> idleAnimation;
    private float stateTime = 0f;

    // Pawn texture from which the idle animation is built
    private Texture pawnTexture;

    private float lastDistance = Float.MAX_VALUE;


    /**
     * Constructs an NPC at position (x, y) with the given dialogue lines.
     */
    public NPC(float x, float y, String[] dialogues) {
        this.x = x;
        this.y = y;
        this.dialogues = dialogues;

        // Load pawn texture and build idle animation from specified region:
        // Region: x=2, y=2, width=1152, height=186; split horizontally into 6 frames (1152/6 = 192).
        pawnTexture = new Texture(Gdx.files.internal("Pawn/pawn_animations.png"));
        TextureRegion idleRegion = new TextureRegion(pawnTexture, 2, 2, 1152, 186);
        TextureRegion[][] tmp = idleRegion.split(192, 186);
        idleAnimation = new Animation<TextureRegion>(0.2f, tmp[0]);
        idleAnimation.setPlayMode(Animation.PlayMode.LOOP);

        // Load custom bubble texture (bubble.png must exist in assets)
        bubbleTexture = new Texture(Gdx.files.internal("bubble.png"));

        // Create a font using FreeTypeFontGenerator
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Homer_Simpson_Revised.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.size = 82;
        param.color = Color.BLACK;
        param.borderWidth = 0.2f;
        param.borderColor = Color.WHITE;
        param.borderStraight = false;
        param.shadowOffsetX = 0;
        param.shadowOffsetY = 0;
        param.shadowColor = Color.CLEAR;
        this.font = generator.generateFont(param);
        generator.dispose();
        // Use linear filtering for smoother scaling
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        // Set initial font scale (will be temporarily changed during dialogue rendering)
        font.getData().setScale(1f);

        inDialogue = false;
        currentLineIndex = 0;
    }

    /**
     * Updates the NPC's state and handles dialogue interaction and the typewriter effect.
     * Pressing the E key when the player is within the interaction radius starts or advances dialogue.
     */
    public void update(float delta, Vector2 playerPos) {
        stateTime += delta;
        float dist = playerPos.dst(x, y);
        lastDistance = dist;

        // Check if player is close enough and E key is pressed
        if (dist <= interactionRadius && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (!inDialogue) {
                inDialogue = true;
                currentLineIndex = 0;
                startTypingEffect(dialogues[currentLineIndex]);
            } else {
                if (!typedComplete) {
                    // If the current line is still typing, complete it immediately
                    typedIndex = dialogues[currentLineIndex].length();
                    typedComplete = true;
                } else {
                    // Advance to the next dialogue line or end dialogue if finished
                    currentLineIndex++;
                    if (currentLineIndex >= dialogues.length) {
                        inDialogue = false;
                    } else {
                        startTypingEffect(dialogues[currentLineIndex]);
                    }
                }
            }
        }

        // Update the typewriter effect if dialogue is active and not complete
        if (inDialogue && !typedComplete) {
            typedTimer += delta;
            int charsToShow = (int)(typedTimer * typedSpeed);
            int totalLength = dialogues[currentLineIndex].length();
            if (charsToShow >= totalLength) {
                charsToShow = totalLength;
                typedComplete = true;
            }
            typedIndex = charsToShow;
            typedText = dialogues[currentLineIndex];
        }
    }

    /**
     * Initializes the typewriter effect for a new dialogue line.
     */
    private void startTypingEffect(String line) {
        typedTimer = 0f;
        typedIndex = 0;
        typedComplete = false;
        typedText = line;
    }

    /**
     * Renders the NPC's idle animation and, if dialogue is active, draws the speech bubble with
     * the dialogue text using a Matrix4 transform for scaling.
     * The text is centered within the bubble.
     */
    public void render(SpriteBatch batch) {
        // Draw the NPC's idle animation (scaled down by converting from pixels to world units, 1/64)
        TextureRegion currentFrame = idleAnimation.getKeyFrame(stateTime, true);
        float spriteW = currentFrame.getRegionWidth() * (1f / 64f);
        float spriteH = currentFrame.getRegionHeight() * (1f / 64f);
        float drawX = x - spriteW / 2f;
        float drawY = y - spriteH / 2f;
        batch.draw(currentFrame, drawX, drawY, spriteW, spriteH);

        if (!inDialogue || lastDistance > interactionRadius) return;

        if (!inDialogue) return;

        // Draw the speech bubble
        float bubbleX = x - bubbleWidth * 0.5f;
        float bubbleY = y + 1.0f;  // Adjust as needed for proper positioning above the NPC
        batch.draw(bubbleTexture, bubbleX, bubbleY, bubbleWidth, bubbleHeight);

        // Compute the center of the bubble (pivot for scaling)
        float centerX = bubbleX + bubbleWidth / 2f;
        float centerY = bubbleY + bubbleHeight / 2f;

        // Calculate the effective wrap width in pixels (assuming 32 px per world unit)
        float bubblePixelWidth = bubbleWidth * 256f;
        float marginPixels = 16f;
        float wrapWidth = bubblePixelWidth - marginPixels;

        // Get the visible text from the typewriter effect
        String visibleText = typedText.substring(0, Math.min(typedIndex, typedText.length()));

        // Create a GlyphLayout with center alignment
        GlyphLayout layout = new GlyphLayout();
        layout.setText(font, visibleText, Color.BLACK, wrapWidth, Align.left, true);

        // Save the current transform matrix
        Matrix4 oldTransform = batch.getTransformMatrix().cpy();

        // Create a new transform that scales relative to the bubble's center
        float textScale = 0.005f;  // Adjust this value if needed
        Matrix4 transform = new Matrix4();
        transform.translate(centerX, centerY, 0);
        transform.scale(textScale, textScale, 1);
        transform.translate(-centerX, -centerY, 0);
        batch.setTransformMatrix(transform);

        // Draw the text centered at the bubble's center (adjust by half layout width/height in pixels)
        // Note: font.draw expects pixel coordinates, but our transform scales accordingly.
        font.draw(batch, layout, centerX - layout.width / 2, centerY + layout.height / 2);

        // Restore the original transform matrix
        batch.setTransformMatrix(oldTransform);
    }

    /**
     * Disposes of the NPC's resources.
     */
    public void dispose() {
        bubbleTexture.dispose();
        pawnTexture.dispose();
        font.dispose();
    }

    public float getX() { return x; }
    public float getY() { return y; }
}
