package io.github.ballsofsteel.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import io.github.ballsofsteel.core.CoreGame;
import io.github.ballsofsteel.events.EventBus;
import io.github.ballsofsteel.events.GameEvent;
import io.github.ballsofsteel.events.GameEventType;

/**
 * NPC class with idle animation, typewriter dialogue effect, and player-centered pointer.
 */
public class NPC {

    private float x, y;
    private float interactionRadius = 2.0f;

    private Texture bubbleTexture;
    private Texture pointerTexture;
    private BitmapFont font;

    private String[] dialogues;
    private int currentLineIndex = 0;
    private boolean inDialogue = false;


    private boolean interacted = false;

    private boolean finishedDialogue = false;


    private float typedSpeed = 20f;
    private float typedTimer = 0f;
    private int typedIndex = 0;
    private boolean typedComplete = false;
    private String typedText = "";

    private float bubbleWidth = 4f;
    private float bubbleHeight = 1f;

    private Animation<TextureRegion> idleAnimation;
    private Texture pawnTexture;
    private float stateTime = 0f;

    private float lastDistance = Float.MAX_VALUE;
    private static final float POINTER_DISTANCE = 1f;
    private static boolean firstWaveStarted = false;

    public NPC(float x, float y, String[] dialogues) {
        this.x = x;
        this.y = y;
        this.dialogues = dialogues;

        pawnTexture = new Texture(Gdx.files.internal("Pawn/pawn_animations.png"));
        TextureRegion idleRegion = new TextureRegion(pawnTexture, 2, 2, 1152, 186);
        TextureRegion[][] tmp = idleRegion.split(192, 186);
        idleAnimation = new Animation<>(0.2f, tmp[0]);
        idleAnimation.setPlayMode(Animation.PlayMode.LOOP);

        bubbleTexture = new Texture(Gdx.files.internal("bubble.png"));
        pointerTexture = new Texture(Gdx.files.internal("HUD/npc_pointer.png"));

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Homer_Simpson_Revised.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.size = 82;
        param.color = Color.BLACK;
        param.borderWidth = 0.2f;
        param.borderColor = Color.WHITE;
        this.font = generator.generateFont(param);
        generator.dispose();

        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        font.getData().setScale(1f);
    }

    public void update(float delta, Vector2 playerPos) {
        stateTime += delta;
        float dist = playerPos.dst(x, y);
        lastDistance = dist;

        if (dist <= interactionRadius && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (finishedDialogue) {
                return; // ◄◄ Eğer konuşma bittiyse bir daha konuşamazsın!
            }

            if (firstWaveStarted && isIntervalActive()) {
                EventBus.post(new GameEvent(GameEventType.UPGRADE_MENU_REQUEST, null));
                return;
            }

            if (!inDialogue) {
                inDialogue = true;
                currentLineIndex = 0;
                startTypingEffect(dialogues[0]);
            } else {
                if (!typedComplete) {
                    typedIndex = dialogues[currentLineIndex].length();
                    typedComplete = true;
                } else {
                    currentLineIndex++;
                    if (currentLineIndex >= dialogues.length) {
                        inDialogue = false;
                        finishedDialogue = true;
                        if (!firstWaveStarted) {
                            firstWaveStarted = true;
                            EventBus.post(new GameEvent(GameEventType.WAVE_START_REQUEST, null));
                        } else {
                            EventBus.post(new GameEvent(GameEventType.UPGRADE_MENU_REQUEST, null));
                        }
                    } else {
                        startTypingEffect(dialogues[currentLineIndex]);
                    }
                }
            }
        }

        if (inDialogue && !typedComplete) {
            typedTimer += delta;
            int show = (int)(typedTimer * typedSpeed);
            int total = dialogues[currentLineIndex].length();
            if (show >= total) { show = total; typedComplete = true; }
            typedIndex = show;
            typedText = dialogues[currentLineIndex];
        }
    }

    private void startTypingEffect(String line) {
        typedTimer = 0f;
        typedIndex = 0;
        typedComplete = false;
        typedText = line;
    }

    public void render(SpriteBatch batch, Vector2 playerPos) {
        TextureRegion currentFrame = idleAnimation.getKeyFrame(stateTime, true);
        float spriteW = currentFrame.getRegionWidth() * (1f / 64f);
        float spriteH = currentFrame.getRegionHeight() * (1f / 64f);
        batch.draw(currentFrame, x - spriteW / 2f, y - spriteH / 2f, spriteW, spriteH);

        float dx = x - playerPos.x;
        float dy = y - playerPos.y;
        float distToPlayer = (float)Math.sqrt(dx * dx + dy * dy);

        if (distToPlayer > 4f) {
            float angleDeg = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
            float dirX = MathUtils.cosDeg(angleDeg);
            float dirY = MathUtils.sinDeg(angleDeg);

            float pointerX = playerPos.x + dirX * POINTER_DISTANCE;
            float pointerY = playerPos.y + dirY * POINTER_DISTANCE;

            float pointerW = pointerTexture.getWidth() * (1f / 48f);
            float pointerH = pointerTexture.getHeight() * (1f / 48f);

            float alpha = MathUtils.clamp((distToPlayer - 4f) / 6f, 0f, 1f);
            float pulse = 1f + 0.05f * MathUtils.sin(stateTime * 3f);

            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(pointerTexture,
                pointerX - pointerW / 2f, pointerY - pointerH / 2f,
                pointerW / 2f, pointerH / 2f,
                pointerW, pointerH,
                pulse, pulse,
                angleDeg - 90f,
                0, 0,
                pointerTexture.getWidth(), pointerTexture.getHeight(),
                false, false
            );
            batch.setColor(1f, 1f, 1f, 1f);
        }

        if (!inDialogue || lastDistance > interactionRadius) return;

        float bubbleX = x - bubbleWidth * 0.5f;
        float bubbleY = y + 1.0f;
        batch.draw(bubbleTexture, bubbleX, bubbleY, bubbleWidth, bubbleHeight);

        float centerX = bubbleX + bubbleWidth / 2f;
        float centerY = bubbleY + bubbleHeight / 2f;
        float bubblePixelWidth = bubbleWidth * 256f;
        float marginPixels = 16f;
        float wrapWidth = bubblePixelWidth - marginPixels;

        String visibleText = typedText.substring(0, Math.min(typedIndex, typedText.length()));

        GlyphLayout layout = new GlyphLayout();
        layout.setText(font, visibleText, Color.BLACK, wrapWidth, Align.left, true);

        Matrix4 oldTransform = batch.getTransformMatrix().cpy();
        Matrix4 transform = new Matrix4();
        transform.translate(centerX, centerY, 0);
        transform.scale(0.005f, 0.005f, 1);
        transform.translate(-centerX, -centerY, 0);
        batch.setTransformMatrix(transform);

        font.draw(batch, layout, centerX - layout.width / 2, centerY + layout.height / 2);
        batch.setTransformMatrix(oldTransform);
    }

    public void dispose() {
        bubbleTexture.dispose();
        pawnTexture.dispose();
        pointerTexture.dispose();
        font.dispose();
    }

    public float getX() { return x; }
    public float getY() { return y; }

    private boolean isIntervalActive() {

        return false;
    }
    public boolean interact() {
        if (interacted) return false;  // bir kere konuştuktan sonra bir daha konuşma
        interacted = true;
        return true;
    }
}
