package io.github.ballsofsteel.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.ballsofsteel.entity.Player;
import io.github.ballsofsteel.events.GameEvent;
import io.github.ballsofsteel.events.GameEventType;
import io.github.ballsofsteel.events.EventBus;
import io.github.ballsofsteel.ui.Fonts;

import java.util.*;
import java.util.List;

public final class UpgradeMenu {

    private final Stage stage;
    private final Skin skin;
    private Window root;
    private Table upgradeTable;
    private Label goldLabel;
    private Label errorLabel;
    private Table skipButtonTable;

    private boolean visible = false;
    private Player player;
    private int currentWave = 0;

    private float inputDelayTimer = 0f;
    private boolean blockKeyboard = true;
    private float errorMessageTimer = 0f;
    private InputProcessor previousInputProcessor;
    private final Map<Integer, List<UpgradeOption>> waveUpgrades = new HashMap<>();

    public UpgradeMenu() {
        stage = new Stage(new ScreenViewport());

        skin = new Skin();
        skin.add("default-font", Fonts.HUD);

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        Texture white = new Texture(pm);
        pm.dispose();
        skin.add("white", white);

        Label.LabelStyle lblStyle = new Label.LabelStyle();
        lblStyle.font = Fonts.HUD;
        lblStyle.fontColor = Color.WHITE;
        skin.add("default", lblStyle);

        Label.LabelStyle errorStyle = new Label.LabelStyle();
        errorStyle.font = Fonts.HUD;
        errorStyle.fontColor = Color.RED;
        skin.add("error", errorStyle);

        TextButton.TextButtonStyle btn = new TextButton.TextButtonStyle();
        btn.font = Fonts.HUD_COMPACT;
        btn.up = skin.newDrawable("white", new Color(0.2f, 0.3f, 0.4f, 0.95f));
        btn.over = skin.newDrawable("white", new Color(0.3f, 0.4f, 0.6f, 1.0f));
        btn.down = skin.newDrawable("white", new Color(0.1f, 0.2f, 0.3f, 1.0f));
        btn.fontColor = Color.WHITE;
        skin.add("upgrade", btn);

        TextButton.TextButtonStyle disabledBtn = new TextButton.TextButtonStyle();
        disabledBtn.font = Fonts.HUD_COMPACT;
        disabledBtn.up = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.7f));
        disabledBtn.fontColor = Color.GRAY;
        skin.add("upgrade-disabled", disabledBtn);

        Window.WindowStyle win = new Window.WindowStyle();
        win.titleFont = Fonts.TITLE;
        win.background = skin.newDrawable("white", new Color(0f, 0f, 0f, 0.85f));
        skin.add("root", win);

        setupWaveUpgrades();
        buildUI();
    }

    private void setupWaveUpgrades() {
        waveUpgrades.put(0, Arrays.asList(
            new UpgradeOption("HEALTH +25", Upgrade.HEALTH, 10, 25f),
            new UpgradeOption("DAMAGE +20%", Upgrade.DAMAGE, 15, 1.2f),
            new UpgradeOption("SPEED +15%", Upgrade.SPEED, 12, 1.15f)
        ));

        waveUpgrades.put(1, Arrays.asList(
            new UpgradeOption("HEALTH +50", Upgrade.HEALTH, 20, 50f),
            new UpgradeOption("DAMAGE +30%", Upgrade.DAMAGE, 25, 1.3f),
            new UpgradeOption("SPEED +25%", Upgrade.SPEED, 22, 1.25f)
        ));

        waveUpgrades.put(2, Arrays.asList(
            new UpgradeOption("HEALTH +75", Upgrade.HEALTH, 30, 75f),
            new UpgradeOption("DAMAGE +50%", Upgrade.DAMAGE, 35, 1.5f),
            new UpgradeOption("SPEED +40%", Upgrade.SPEED, 32, 1.4f)
        ));

        waveUpgrades.put(3, Arrays.asList(
            new UpgradeOption("HEALTH +100", Upgrade.HEALTH, 45, 100f),
            new UpgradeOption("DAMAGE +75%", Upgrade.DAMAGE, 50, 1.75f),
            new UpgradeOption("SPEED +60%", Upgrade.SPEED, 48, 1.6f)
        ));
    }

    private void buildUI() {
        root = new Window("", skin, "root");
        root.pad(40);
        root.setModal(true);
        root.setMovable(false);
        root.setWidth(500);

        Label title = new Label("CHOOSE AN UPGRADE", skin);
        title.setFontScale(1.4f);
        title.setAlignment(Align.center);
        root.add(title).padBottom(20).center().row();

        goldLabel = new Label("Gold: 0", skin);
        goldLabel.setFontScale(1.2f);
        goldLabel.setAlignment(Align.center);
        root.add(goldLabel).padBottom(20).center().row();

        upgradeTable = new Table();
        upgradeTable.top().left();

        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        ScrollPane scrollPane = new ScrollPane(upgradeTable, scrollStyle);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        root.add(scrollPane).height(240).width(520).padBottom(20).expandX().center().row();

        errorLabel = new Label("", skin, "error");
        errorLabel.setFontScale(1.1f);
        errorLabel.setAlignment(Align.center);
        errorLabel.setVisible(false);
        root.add(errorLabel).padBottom(10).center().row();

        skipButtonTable = new Table();
        root.add(skipButtonTable).center().row();

        root.pack();
        root.setPosition((Gdx.graphics.getWidth() - root.getWidth()) / 2f,
            (Gdx.graphics.getHeight() - root.getHeight()) / 2f);
        stage.addActor(root);

        root.setColor(1, 1, 1, 0);
        root.setScale(0.9f);
        root.setVisible(false);
    }

    private void refreshUpgradeOptions() {
        upgradeTable.clear();
        skipButtonTable.clear();
        List<UpgradeOption> options = waveUpgrades.getOrDefault(currentWave, Collections.emptyList());
        boolean canAfford = false;

        for (int i = 0; i < options.size(); i++) {
            final UpgradeOption option = options.get(i);
            String buttonText = (i + 1) + "  :  " + option.name + " (Cost: " + option.cost + " Gold)";
            TextButton b = new TextButton(buttonText, skin, "upgrade");
            b.pad(16, 0, 16, 0);
            b.getLabel().setFontScale(0.8f);

            if (player.getGold() < option.cost) {
                b.setDisabled(true);
                b.setStyle(skin.get("upgrade-disabled", TextButton.TextButtonStyle.class));
            } else {
                canAfford = true;
                b.setDisabled(false);
                b.setStyle(skin.get("upgrade", TextButton.TextButtonStyle.class));
                b.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) {
                        applyUpgrade(option);
                    }
                });
            }
            upgradeTable.add(b).expandX().fillX().height(56).pad(6).center().row();
        }

        if (!canAfford) {
            TextButton skipBtn = new TextButton("Skip", skin, "upgrade");
            skipBtn.pad(2, 0, 5, 0);
            skipBtn.getLabel().setFontScale(1.1f);
            skipBtn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    Gdx.app.log("UpgradeMenu", "Skip button clicked");
                    EventBus.post(new GameEvent(GameEventType.UPGRADE_SELECTED, "SKIP"));
                    hide();
                }
            });
            skipButtonTable.add(skipBtn).width(250).height(25).padTop(-200f).center().row();
        }

        upgradeTable.pack();
        upgradeTable.validate();
    }

    public void show(Player p, int wave) {
        Gdx.app.log("UpgradeMenu", "Show called for wave: " + wave);
        if (visible && root.isVisible()) return; // Only block if both are true
        player = p;
        currentWave = wave;
        visible = true;

        inputDelayTimer = 0.3f;
        blockKeyboard = true;
        errorLabel.setVisible(false);

        goldLabel.setText("Gold: " + player.getGold());
        refreshUpgradeOptions();

        previousInputProcessor = Gdx.input.getInputProcessor();
        Gdx.input.setInputProcessor(stage);

        root.setVisible(true);
        root.setColor(1, 1, 1, 1);
        root.setPosition(
            (Gdx.graphics.getWidth() - root.getWidth()) / 2f,
            (Gdx.graphics.getHeight() - root.getHeight()) / 2f
        );
        root.setScale(1.0f);

        root.getColor().a = 0;
        root.addAction(Actions.fadeIn(0.3f));
    }

    public void hide() {
        Gdx.app.log("UpgradeMenu", "Hide called (visible=" + visible + ")");
        if (!visible) return;
        root.addAction(Actions.sequence(
            Actions.fadeOut(.2f),
            Actions.visible(false),
            Actions.run(() -> {
                visible = false; // Set here, after fade-out
                if (previousInputProcessor != null) {
                    Gdx.input.setInputProcessor(previousInputProcessor);
                }
            })
        ));
    }

    public boolean isVisible() {
        return visible;
    }

    public void handleInput() {
        if (!visible) return;
        if (blockKeyboard) {
            inputDelayTimer -= Gdx.graphics.getDeltaTime();
            if (inputDelayTimer <= 0f) blockKeyboard = false;
            else return;
        }

        // Handle error message timer
        if (errorLabel.isVisible()) {
            errorMessageTimer -= Gdx.graphics.getDeltaTime();
            if (errorMessageTimer <= 0f) {
                errorLabel.setVisible(false);
            }
        }

        // Handle number key inputs
        List<UpgradeOption> options = waveUpgrades.getOrDefault(currentWave, Collections.emptyList());
        for (int i = 0; i < options.size(); i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) {
                UpgradeOption option = options.get(i);
                if (player.getGold() >= option.cost) {
                    applyUpgrade(option);
                } else {
                    showErrorMessage("Not enough gold for this upgrade!");
                }
                break;
            }
        }
    }

    private void showErrorMessage(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorMessageTimer = 2.0f; // Show for 2 seconds
    }

    private enum Upgrade {HEALTH, DAMAGE, SPEED}

    private static class UpgradeOption {
        String name;
        Upgrade type;
        int cost;
        float value;

        UpgradeOption(String name, Upgrade type, int cost, float value) {
            this.name = name;
            this.type = type;
            this.cost = cost;
            this.value = value;
        }
    }

    private void applyUpgrade(UpgradeOption option) {
        Gdx.app.log("UpgradeMenu", "applyUpgrade called: " + option.name);

        // Check if player has enough gold
        if (player.getGold() < option.cost) {
            showErrorMessage("Not enough gold for this upgrade!");
            return;
        }

        // Apply the upgrade
        switch (option.type) {
            case HEALTH:
                player.increaseHealth(option.value);
                break;
            case DAMAGE:
                player.increaseAttackDamage(option.value);
                break;
            case SPEED:
                player.increaseMoveSpeed(option.value);
                break;
        }

        // Deduct the cost
        player.addGold(-option.cost);

        // Notify the system that an upgrade was selected
        EventBus.post(new GameEvent(GameEventType.UPGRADE_SELECTED, option.type.name()));

        Gdx.app.log("UpgradeMenu", "Posted UPGRADE_SELECTED for: " + option.type.name());
        hide();
    }

    public void render(SpriteBatch sb) {
        if (!visible) return;
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    public Stage getStage() {
        return stage;
    }
}
