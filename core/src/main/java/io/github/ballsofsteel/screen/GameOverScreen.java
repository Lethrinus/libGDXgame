package io.github.ballsofsteel.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.Color;
import io.github.ballsofsteel.ui.Fonts;

public class GameOverScreen implements Screen {
    private final Game game;
    private final TextureRegion bgRegion;
    private Stage stage;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Skin skin;
    private TextureAtlas atlas;
    private Table root;
    private float elapsedTime = 0f;

    public GameOverScreen(Game game, Texture snapshot) {
        this.game = game;
        // Wrap & flip vertically so background is right side up
        this.bgRegion = new TextureRegion(snapshot);
        this.bgRegion.flip(false, true);
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight());

        // Load atlas & skin
        atlas = new TextureAtlas(Gdx.files.internal("UI/mainmenu_ui.atlas"));

        skin = new Skin();
        skin.add("default-font", Fonts.HUD);
        skin.add("title-font", Fonts.TITLE);

        // Safe region lookup
        TextureRegion upReg = atlas.findRegion("Button_Red_3Slides");
        TextureRegion downReg = atlas.findRegion("Button_Blue_3Slides_Pressed");
        if (upReg == null || downReg == null) {
            Gdx.app.error("GameOverScreen", "Button regions not found in atlas");
        }

        TextButton.TextButtonStyle redStyle = new TextButton.TextButtonStyle();
        if (upReg != null) redStyle.up = new TextureRegionDrawable(upReg);
        if (downReg != null) redStyle.down = new TextureRegionDrawable(downReg);
        redStyle.font = Fonts.HUD;
        skin.add("red", redStyle);

        Label.LabelStyle titleStyle = new Label.LabelStyle(Fonts.TITLE, Color.RED);
        skin.add("title", titleStyle);

        // Build root table and hide initially
        root = new Table();
        root.setFillParent(true);
        root.setVisible(false);

        Label gameOverLabel = new Label("GAME OVER", skin, "title");
        gameOverLabel.setFontScale(1.5f); // reduced font size

        TextButton menuButton = new TextButton("Main Menu", skin, "red");
        menuButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
            }
        });

        TextButton exitButton = new TextButton("Exit Game", skin, "red");
        exitButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        root.center();
        root.add(gameOverLabel).padBottom(60f).row();
        root.add(menuButton).width(250).height(70).pad(8f).row();
        root.add(exitButton).width(250).height(70).pad(8f);

        stage = new Stage(new ScreenViewport(camera));
        stage.addActor(root);
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        elapsedTime += delta;
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        // Draw flipped background snapshot
        batch.begin();
        batch.draw(bgRegion,
            0, 0,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight());
        batch.end();

        // Reveal buttons & label after 2 seconds
        if (elapsedTime >= 2f && !root.isVisible()) {
            root.setVisible(true);
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        camera.setToOrtho(false, width, height);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { dispose(); }

    @Override
    public void dispose() {
        bgRegion.getTexture().dispose();
        batch.dispose();
        stage.dispose();
        skin.dispose();
        atlas.dispose();
    }
}
