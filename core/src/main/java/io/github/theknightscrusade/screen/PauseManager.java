package io.github.theknightscrusade.screen;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.theknightscrusade.ui.Fonts;

public class PauseManager {

    private boolean paused = false;
    private final Stage stage;
    private final InputProcessor gameplayInput;

    public PauseManager(Game game, InputProcessor gameplayInput, TextureAtlas atlas) {
        this.gameplayInput = gameplayInput;

        stage = new Stage(new ScreenViewport());

        Skin skin = new Skin();
        skin.add("default-font", Fonts.HUD);
        skin.add("title-font", Fonts.TITLE);

        // style buttons
        TextButton.TextButtonStyle blue = new TextButton.TextButtonStyle();
        blue.up = new TextureRegionDrawable(atlas.findRegion("Button_Blue_3Slides"));
        blue.down = new TextureRegionDrawable(atlas.findRegion("Button_Blue_3Slides_Pressed"));
        blue.font = Fonts.HUD;
        skin.add("blue", blue);

        TextButton.TextButtonStyle red = new TextButton.TextButtonStyle();
        red.up = new TextureRegionDrawable(atlas.findRegion("Button_Red_3Slides"));
        red.down = blue.down;
        red.font = Fonts.HUD;
        skin.add("red", red);

        skin.add("title", new Label.LabelStyle(Fonts.TITLE, com.badlogic.gdx.graphics.Color.WHITE));

        // interface
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Label title = new Label("Paused", skin, "title");

        TextButton resumeBtn = new TextButton("Resume", skin, "blue");
        TextButton menuBtn = new TextButton("Main Menu", skin, "red");

        resumeBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setPaused(false);
            }
        });

        menuBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
            }
        });

        root.center();
        root.add(title).padBottom(40f).row();
        root.add(resumeBtn).width(300).height(80).pad(10f).row();
        root.add(menuBtn).width(300).height(80).pad(10f);
    }

    public void update() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            setPaused(!paused);
        }
    }

    public void render() {
        if (!paused) return;
        stage.act();
        stage.draw();
    }

    public void setPaused(boolean value) {
        paused = value;
        Gdx.input.setInputProcessor(paused ? stage : gameplayInput);
    }

    public boolean isPaused() {
        return paused;
    }
    public Stage getStage() {
        return stage;
    }

    public void dispose() {
        stage.dispose();
    }
}
