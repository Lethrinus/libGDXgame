// src/io/github/some_example_name/MenuScreen.java
package io.github.ballsofsteel.screen;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class MenuScreen implements Screen {
    private final Game game;
    private Stage stage;
    private TextureAtlas atlas;
    private Skin skin;
    private BitmapFont font, titleFont;

    public MenuScreen(Game game) { this.game = game; }

    @Override public void show() {
        stage = new Stage(new FitViewport(1200, 800));
        Gdx.input.setInputProcessor(stage);

        atlas = new TextureAtlas("UI/mainmenu_ui.atlas");

        // font hazırlığı (eski kodunun aynısı)
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Homer_Simpson_Revised.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = 32;  p.color = Color.WHITE;  p.borderWidth = 2;
        font = gen.generateFont(p);  p.size = 48;
        titleFont = gen.generateFont(p);  gen.dispose();

        skin = new Skin();  skin.add("default-font", font);  skin.add("title-font", titleFont);

        TextButton.TextButtonStyle blue = new TextButton.TextButtonStyle();
        blue.up = new TextureRegionDrawable(atlas.findRegion("Button_Blue_3Slides"));
        blue.down = new TextureRegionDrawable(atlas.findRegion("Button_Blue_3Slides_Pressed"));
        blue.font = font; skin.add("blue", blue);

        TextButton.TextButtonStyle red = new TextButton.TextButtonStyle();
        red.up = new TextureRegionDrawable(atlas.findRegion("Button_Red_3Slides"));
        red.down = blue.down; red.font = font; skin.add("red", red);

        Label.LabelStyle title = new Label.LabelStyle(titleFont, Color.WHITE);
        skin.add("title", title);

        buildUI();
    }

    private void buildUI() {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // === Background Ekle ===
        Texture backgroundTexture = new Texture(Gdx.files.internal("HUD/background.png"));
        Image background = new Image(new TextureRegionDrawable(new TextureRegion(backgroundTexture)));
        background.setFillParent(true);
        stage.addActor(background);

        // === UI Elemanları ===
        Table uiTable = new Table();
        uiTable.setFillParent(true);
        uiTable.right().padRight(100f);
        stage.addActor(uiTable);

        Label lbl = new Label("Balls of Steel", skin, "title");
        uiTable.add(lbl).padBottom(40).padRight(50f).right().row();
        TextButton play = new TextButton("Play Game", skin, "blue");
        TextButton quit = new TextButton("Exit", skin, "red");

        uiTable.add(play).width(320).height(90).pad(10).right().row();
        uiTable.add(quit).width(320).height(90).pad(10).right();

        // === Button Listener ===
        play.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                stage.addAction(Actions.sequence(
                    Actions.fadeOut(0.4f),
                    Actions.run(() -> game.setScreen(new PlayScreen(game)))
                ));
            }
        });
        quit.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                Gdx.app.exit();
            }
        });
    }

    @Override public void render(float delta){
        Gdx.gl.glClearColor(0.1f,0.1f,0.2f,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta); stage.draw();
    }

    @Override public void resize(int w,int h){ stage.getViewport().update(w,h,true); }
    @Override public void pause(){}
    @Override public void resume(){}
    @Override public void hide(){}
    @Override public void dispose(){ stage.dispose(); atlas.dispose(); skin.dispose(); font.dispose(); titleFont.dispose(); }
}
