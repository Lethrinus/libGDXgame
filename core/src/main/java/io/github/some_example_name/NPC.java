package io.github.some_example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.Input;

public class NPC {
    private float x, y;
    private Animation<TextureRegion> idleAnimation;
    private float stateTime = 0f;

    private String[] dialogues;
    private int currentDialogueIndex = 0;
    private boolean inDialogue = false;

    private Texture bubbleTexture;

    // This font will be generated in pixels, then scaled down to world units
    private BitmapFont font;

    private float bubbleWidth = 5f;
    private float bubbleHeight = 1.5f;

    private float interactionRadius = 2.0f;

    private Texture pawnTexture;

    // Typing effect fields
    private float typedTimer = 0f;
    private float typedSpeed = 5f;
    private int typedIndex = 0;
    private String typedText = "";
    private boolean typedComplete = false;

    public NPC(float x, float y, String[] dialogues) {
        this.x = x;
        this.y = y;
        this.dialogues = dialogues;

        // 1) Load pawn animation
        pawnTexture = new Texture(Gdx.files.internal("Pawn/pawn_animations.png"));
        TextureRegion idleRegion = new TextureRegion(pawnTexture, 2, 2, 1152, 186);
        TextureRegion[][] tmp = idleRegion.split(192, 186);
        idleAnimation = new Animation<>(0.2f, tmp[0]);
        idleAnimation.setPlayMode(Animation.PlayMode.LOOP);

        // 2) Load dialogue bubble texture
        bubbleTexture = new Texture(Gdx.files.internal("bubble.png"));

        // 3) Generate a font in pixel size, then scale it down
        // Generate the font
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Homer_Simpson_Revised.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 16;  // smaller glyph size
        parameter.color = Color.BLACK;
        font = generator.generateFont(parameter);
        generator.dispose();

// Scale down in the 16×9 world
        font.getData().setScale(1/16f);  // about 0.0625

// Remove or comment out the extra line spacing for now
        font.getData().down *= 0.15f; // optional, fine-tune later

                // ... later in startTypingEffect:
        typedSpeed = 9999f;  // to test if your text is sized & wrapped correctly


        // 4) Start typing effect with the first dialogue
        startTypingEffect(dialogues[0]);
    }

    public void update(float delta, Vector2 playerPos) {
        stateTime += delta;
        float dist = playerPos.dst(x, y);

        // If player is close enough and presses E, handle dialogue
        if (dist <= interactionRadius && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (!inDialogue) {
                // Start dialogue
                inDialogue = true;
                currentDialogueIndex++;
                if (currentDialogueIndex >= dialogues.length) {
                    currentDialogueIndex = 0;
                }
                startTypingEffect(dialogues[currentDialogueIndex]);
            } else {
                // Already in dialogue
                if (!typedComplete) {
                    // If text not fully typed yet, jump to the end
                    typedIndex = typedText.length();
                    typedComplete = true;
                } else {
                    // Go to next dialogue
                    currentDialogueIndex++;
                    if (currentDialogueIndex >= dialogues.length) {
                        currentDialogueIndex = 0;
                        inDialogue = false;
                    } else {
                        startTypingEffect(dialogues[currentDialogueIndex]);
                    }
                }
            }
        }

        // If in dialogue but text is empty, start it again
        if (inDialogue && (typedText == null || typedText.isEmpty())) {
            startTypingEffect(dialogues[currentDialogueIndex]);
        }

        // Typewriter effect
        if (inDialogue && !typedComplete) {
            typedTimer += delta;
            int charsToShow = (int)(typedTimer * typedSpeed);
            if (charsToShow >= typedText.length()) {
                charsToShow = typedText.length();
                typedComplete = true;
            }
            typedIndex = charsToShow;
        }
    }

    private void startTypingEffect(String text) {
        typedTimer = 0f;
        typedSpeed = 999999f; // Very large => instantly display the entire text
        typedIndex = 0;
        typedComplete = false;
        typedText = text;
    }

    public void render(SpriteBatch batch) {
        // Draw NPC idle animation
        TextureRegion currentFrame = idleAnimation.getKeyFrame(stateTime, true);
        float spriteW = currentFrame.getRegionWidth() * (1f / 64f);
        float spriteH = currentFrame.getRegionHeight() * (1f / 64f);
        float drawX = x - spriteW / 2f;
        float drawY = y - spriteH / 2f;
        batch.draw(currentFrame, drawX, drawY, spriteW, spriteH);

        // If in dialogue, draw the bubble + text
        if (inDialogue) {
            float bubbleX = x - bubbleWidth / 2f;
            float bubbleY = y + spriteH / 2f - 1f;
            batch.draw(bubbleTexture, bubbleX, bubbleY, bubbleWidth, bubbleHeight);

            // Get the portion of text to display
            String visibleText = typedText.substring(0, Math.min(typedIndex, typedText.length()));

            // Lay out the text with wrapping at (bubbleWidth - some padding)
            // The font is scaled, so "bubbleWidth" is the correct width in world units
            float textWrapWidth = bubbleWidth - 0.3f;
            GlyphLayout layout = new GlyphLayout(
                font,
                visibleText,
                Color.BLACK,
                textWrapWidth,
                Align.center,
                false
            );

            // Position the text near the top inside the bubble
            float textX = bubbleX + 0.2f;
            float textY = bubbleY + bubbleHeight - 0.2f;
            font.draw(batch, layout, textX, textY);
        }
    }

    public void dispose() {
        bubbleTexture.dispose();
        font.dispose();
        pawnTexture.dispose();
    }

    public float getX() { return x; }
    public float getY() { return y; }
}
