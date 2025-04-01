package io.github.some_example_name;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.math.Interpolation;

public class SettingsScreen implements Screen {
    private final MainMenu mainMenu;
    private Stage stage;
    private TextureAtlas atlas;
    private Skin skin;
    private BitmapFont font, titleFont;
    private float screenWidth = 1200;
    private float screenHeight = 800;

    public SettingsScreen(MainMenu mainMenu) {
        this.mainMenu = mainMenu;
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(screenWidth, screenHeight));
        Gdx.input.setInputProcessor(stage);

        atlas = new TextureAtlas(Gdx.files.internal("UI/mainmenu_ui.atlas"));

        createFonts();
        createSkin();
        createUI();
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

        // Title font
        parameter.size = 48;
        parameter.borderWidth = 3;
        titleFont = generator.generateFont(parameter);

        generator.dispose();
    }

    private void createSkin() {
        skin = new Skin();
        skin.add("default-font", font);
        skin.add("title-font", titleFont);

        // Button styles
        TextButton.TextButtonStyle blueButtonStyle = new TextButton.TextButtonStyle();
        blueButtonStyle.up = new TextureRegionDrawable(atlas.findRegion("Button_Blue_3Slides"));
        blueButtonStyle.down = new TextureRegionDrawable(atlas.findRegion("Button_Blue_3Slides_Pressed"));
        blueButtonStyle.font = font;
        blueButtonStyle.fontColor = Color.WHITE;
        skin.add("blue", blueButtonStyle);

        // Slider style
        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = new TextureRegionDrawable(atlas.findRegion("Button_Blue_3Slides"));
        sliderStyle.knob = new TextureRegionDrawable(atlas.findRegion("Button_Red_3Slides"));
        skin.add("default-horizontal", sliderStyle);

        // Label styles
        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, Color.WHITE);
        skin.add("title", titleStyle);

        Label.LabelStyle normalStyle = new Label.LabelStyle(font, Color.WHITE);
        skin.add("default", normalStyle);
    }

    private void createUI() {
        // Ribbon - in upper left
        Image ribbon = new Image(atlas.findRegion("Ribbon_Blue_3Slides"));
        ribbon.setScale(1.6f);
        ribbon.setScaleX(2.0f);
        ribbon.setPosition(50, screenHeight - 180);
        stage.addActor(ribbon);

        // Title - in upper left
        Label titleLabel = new Label("Settings", skin, "title");
        titleLabel.setPosition(130, screenHeight - 150);
        stage.addActor(titleLabel);

        // Button table on the right
        Table buttonTable = new Table();
        buttonTable.setPosition(screenWidth - 350, screenHeight / 2);
        stage.addActor(buttonTable);

        // Volume controls
        Table volumeTable = new Table();
        Label volumeLabel = new Label("Volume       ", skin);
        final Label volumeValueLabel = new Label("50", skin);

        Slider volumeSlider = new Slider(0, 100, 1, false, skin);
        volumeSlider.setValue(50);
        volumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Slider slider = (Slider) actor;
                volumeValueLabel.setText(Integer.toString((int) slider.getValue()));
            }
        });

        volumeTable.add(volumeLabel).padRight(20);
        volumeTable.add(volumeSlider).width(300);
        volumeTable.add(volumeValueLabel).width(50).padLeft(10);

        // Resolution controls
        Table resolutionTable = new Table();
        final Label resolutionLabel = new Label("Resolution      ", skin);

        final TextButton resolutionButton = new TextButton("1200x800", skin, "blue");
        addHoverEffect(resolutionButton);

        resolutionTable.add(resolutionLabel);
        resolutionTable.add(resolutionButton).width(300);

        // Back button
        TextButton backButton = new TextButton("Back", skin, "blue");
        addHoverEffect(backButton);

        // Add all components to button table with spacing
        buttonTable.add(volumeTable).pad(20).row();
        buttonTable.add(resolutionTable).pad(20).row();
        buttonTable.add(backButton).width(200).pad(30);

        // Fix back button handler
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stage.addAction(Actions.sequence(
                    Actions.fadeOut(0.5f),
                    Actions.run(() -> {
                        // Properly dispose resources before returning to main menu
                        dispose();
                        // Call create() on MainMenu to reinitialize everything
                        mainMenu.create();
                        mainMenu.setScreen(null);
                    })
                ));
            }
        });
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
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (atlas != null) atlas.dispose();
        if (skin != null) skin.dispose();
        if (font != null) font.dispose();
        if (titleFont != null) titleFont.dispose();
    }
}
