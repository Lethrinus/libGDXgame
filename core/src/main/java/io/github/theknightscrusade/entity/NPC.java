package io.github.theknightscrusade.entity;

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
import io.github.theknightscrusade.core.CoreGame;
import io.github.theknightscrusade.core.WaveManager;
import io.github.theknightscrusade.events.EventBus;
import io.github.theknightscrusade.events.GameEvent;
import io.github.theknightscrusade.events.GameEventListener;
import io.github.theknightscrusade.events.GameEventType;

import java.util.HashMap;
import java.util.Map;

public class NPC implements GameEventListener {

    private final float x, y;
    private final float interactionRadius = 2.0f;

    private final Texture bubbleTexture;
    private final Texture pointerTexture;
    private final BitmapFont font;

    private final Map<Integer, String[]> waveDialogues = new HashMap<>();
    private String[] currentDialogue;
    private int currentLineIndex = 0;
    private boolean inDialogue = false;
    private boolean showPointer = true;

    private float typedTimer = 0f;
    private int typedIndex = 0;
    private boolean typedComplete = false;
    private String typedText = "";

    private final Animation<TextureRegion> idleAnimation;
    private final Texture pawnTexture;
    private float stateTime = 0f;

    private float lastDistance = Float.MAX_VALUE;
    private static final float POINTER_DISTANCE = 1.2f;
    private static boolean firstWaveStarted = false;

    private final CoreGame game;

    public NPC(CoreGame game, float x, float y) {
        this.game = game;
        this.x = x;
        this.y = y;
        this.currentDialogue = new String[]{};
        EventBus.register(this);

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


        if (game != null) {
            boolean anyAlive = !game.getGoblins().isEmpty()
                || !game.getDynaList().isEmpty()
                || !game.getBarrels().isEmpty();
                 setPointerVisible(!anyAlive);
            if (anyAlive) {
                return;
            }
        }
        // ——————————————————————————————————————————————————————

        float dist = playerPos.dst(x, y);
        lastDistance = dist;

        if (dist <= interactionRadius && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (!isNpcInteractable()) return;

            if (!inDialogue) {
                inDialogue = true;
                currentLineIndex = 0;
                updateCurrentDialogue();
                startTypingEffect(currentDialogue[0]);
            } else {
                if (!typedComplete) {
                    typedIndex = currentDialogue[currentLineIndex].length();
                    typedComplete = true;
                } else {
                    currentLineIndex++;
                    if (currentLineIndex < currentDialogue.length) {
                        startTypingEffect(currentDialogue[currentLineIndex]);
                    } else {
                        inDialogue = false;

                        int wave = game.getWaveManager() != null
                            ? game.getWaveManager().getCurrentWave()
                            : -1;
                        boolean isStillInterval = game != null && game.isIntervalActive();

                        if (!firstWaveStarted && (wave == -1 || wave == 0)) {
                            firstWaveStarted = true;
                            EventBus.post(new GameEvent(GameEventType.WAVE_START_REQUEST, null));
                        } else if (isStillInterval && wave >= 1 && wave < 4) {
                            if (game.getUpgradeMenu() != null && !game.getUpgradeMenu().isVisible()) {
                                EventBus.post(new GameEvent(GameEventType.UPGRADE_MENU_REQUEST, wave));
                            }
                        } else if (wave == 4) {
                            EventBus.post(new GameEvent(GameEventType.WAVE_START_REQUEST, null));
                        }
                    }
                }
            }
        }


        if (inDialogue && !typedComplete) {
            typedTimer += delta;
            float typedSpeed = 20f;
            int show = (int)(typedTimer * typedSpeed);
            int total = currentDialogue[currentLineIndex].length();
            if (show >= total) { show = total; typedComplete = true; }
            typedIndex = show;
            typedText = currentDialogue[currentLineIndex];
        }
    }

    private void updateCurrentDialogue() {
        int wave = game != null && game.getWaveManager() != null ? game.getWaveManager().getCurrentWave() : -1;
        currentDialogue = waveDialogues.getOrDefault(wave, waveDialogues.get(-1));
        if (currentDialogue == null || currentDialogue.length == 0) {
            currentDialogue = new String[]{"..."};
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

        if (distToPlayer > 2f && showPointer) {
            float angleDeg = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
            float dirX = MathUtils.cosDeg(angleDeg);
            float dirY = MathUtils.sinDeg(angleDeg);

            float pointerX = playerPos.x + dirX * POINTER_DISTANCE;
            float pointerY = playerPos.y + dirY * POINTER_DISTANCE;

            float pointerW = pointerTexture.getWidth() * (1f / 48f);
            float pointerH = pointerTexture.getHeight() * (1f / 48f);

            float alpha = MathUtils.clamp((distToPlayer - 2f) / 6f, 0f, 1f);
            float pulse = 1f + 0.25f * MathUtils.sin(stateTime * 5f);

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

        float bubbleWidth = 4f;
        float bubbleX = x - bubbleWidth * 0.5f;
        float bubbleY = y + 1.0f;
        float bubbleHeight = 1f;
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
    public void setPointerVisible(boolean visible) { this.showPointer = visible; }
    private WaveManager getWaveManager() {
        return game != null ? game.getWaveManager() : null;
    }
    public void setDialoguesForWave(int wave, String[] lines) {
        waveDialogues.put(wave, lines);
    }

    public boolean isNpcInteractable() {
        WaveManager waveManager = getWaveManager();
        return waveManager != null && waveManager.isNpcInteractable();
    }
    @Override
    public void onEvent(GameEvent event) {
        switch (event.getType()) {
            case UPGRADE_SELECTED:
            case WAVE_START_REQUEST:
                break;
            default:
                break;
        }
    }
}


