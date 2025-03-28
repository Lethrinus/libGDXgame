package io.github.some_example_name;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.math.Interpolation;

public class MainMenu extends Game {
    private Stage stage;
    private TextureAtlas atlas;
    private Skin skin;
    private BitmapFont font, titleFont;
    private float screenWidth = 1200;
    private float screenHeight = 800;
    private Pixmap cursorPixmap;
    @Override
    public void create() {
        // Set up stage with FitViewport for consistent UI scaling
        stage = new Stage(new FitViewport(screenWidth, screenHeight));
        Gdx.input.setInputProcessor(stage);
        // Only create cursor if it hasn't been created already
        if (cursorPixmap == null) {
            cursorPixmap = new Pixmap(Gdx.files.internal("HUD/cursor.png"));
            Cursor customCursor = Gdx.graphics.newCursor(cursorPixmap, 0, 0);
            Gdx.graphics.setCursor(customCursor);
            // Don't dispose pixmap here anymore
        }
        // Load texture atlas
        atlas = new TextureAtlas(Gdx.files.internal("UI/mainmenu_ui.atlas"));

        // Create fonts
        createFonts();

        // Create UI skin and styles
        createSkin();

        // Build the menu UI
        buildMenuUI();
    }

    private void createFonts() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Homer_Simpson_Revised.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();

        // Regular font for buttons
        parameter.size = 32;
        parameter.color = Color.WHITE;
        parameter.borderWidth = 2;
        parameter.borderColor = new Color(0.2f, 0.2f, 0.4f, 1);
        font = generator.generateFont(parameter);

        // Title font for "Balls of Steel"
        parameter.size = 40;
        parameter.borderWidth = 2;
        titleFont = generator.generateFont(parameter);

        generator.dispose();
    }

    private void createSkin() {
        skin = new Skin();
        skin.add("default-font", font);
        skin.add("title-font", titleFont);

        // Blue button style (normal)
        TextButton.TextButtonStyle blueButtonStyle = new TextButton.TextButtonStyle();
        blueButtonStyle.up = new TextureRegionDrawable(atlas.findRegion("Button_Blue_3Slides"));
        blueButtonStyle.down = new TextureRegionDrawable(atlas.findRegion("Button_Blue_3Slides_Pressed"));
        blueButtonStyle.font = font;
        blueButtonStyle.fontColor = Color.WHITE;
        skin.add("blue", blueButtonStyle);

        // Red button style (for exit)
        TextButton.TextButtonStyle redButtonStyle = new TextButton.TextButtonStyle();
        redButtonStyle.up = new TextureRegionDrawable(atlas.findRegion("Button_Red_3Slides"));
        redButtonStyle.down = new TextureRegionDrawable(atlas.findRegion("Button_Blue_3Slides_Pressed"));
        redButtonStyle.font = font;
        redButtonStyle.fontColor = Color.WHITE;
        skin.add("red", redButtonStyle);

        // Title label style
        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, Color.WHITE);
        skin.add("title", titleStyle);
    }

    private void buildMenuUI() {
        // Main table that fills the screen
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        // Ribbon - positioned on the left side
        Image ribbon = new Image(atlas.findRegion("Ribbon_Blue_3Slides"));
        ribbon.setScale(2f);
        ribbon.setPosition(50, screenHeight - 250);
        stage.addActor(ribbon);

        // "Balls of Steel" text placed on the ribbon
        Label titleLabel = new Label("Balls of Steel", skin, "title");
        titleLabel.setPosition(130, screenHeight - 190);
        stage.addActor(titleLabel);

        // Button table - positioned on the right side
        Table buttonTable = new Table();
        buttonTable.defaults().width(300).height(80).pad(10);
        buttonTable.setPosition(screenWidth - 350, screenHeight / 2);
        stage.addActor(buttonTable);

        // Create buttons
        TextButton playButton = new TextButton("Play Game", skin, "blue");
        TextButton optionsButton = new TextButton("Options", skin, "blue");
        TextButton creditsButton = new TextButton("Credits", skin, "blue");
        TextButton quitButton = new TextButton("Exit", skin, "red");

        // Add hover animations to buttons
        addHoverEffect(playButton);
        addHoverEffect(optionsButton);
        addHoverEffect(creditsButton);
        addHoverEffect(quitButton);

        // Add buttons to table
        buttonTable.add(playButton).row();
        buttonTable.add(optionsButton).row();
        buttonTable.add(creditsButton).row();
        buttonTable.add(quitButton).row();

        // Button click handlers
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stage.addAction(Actions.sequence(
                    Actions.fadeOut(0.5f),
                    Actions.run(() -> {
                        setScreen(new GameScreen(MainMenu.this));
                    })
                ));
            }
        });

        quitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        optionsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stage.addAction(Actions.sequence(
                    Actions.fadeOut(0.5f),
                    Actions.run(() -> {
                        setScreen(new SettingsScreen(MainMenu.this));
                    })
                ));
            }
        });
    }
    public void returnToMainMenu() {
        setScreen(null);  // Remove any active screen
    }
    private void addHoverEffect(final Actor actor) {
        actor.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                actor.addAction(Actions.scaleTo(1.1f, 1.1f, 0.2f, Interpolation.swing));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                actor.addAction(Actions.scaleTo(1.0f, 1.0f, 0.2f, Interpolation.swing));
            }
        });
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.1f, 0.1f, 0.2f, 1);

        float delta = Gdx.graphics.getDeltaTime();
        stage.act(delta);
        stage.draw();

        // Call render on the active screen (if set)
        super.render();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (atlas != null) atlas.dispose();
        if (skin != null) skin.dispose();
        if (font != null) font.dispose();
        if (titleFont != null) titleFont.dispose();


        }
    }

