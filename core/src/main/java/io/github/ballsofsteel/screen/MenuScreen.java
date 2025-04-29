package io.github.ballsofsteel.screen;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.ballsofsteel.ui.Fonts;

public class MenuScreen implements Screen {

    private final Game game;
    private Stage stage;
    private TextureAtlas atlas;
    private Skin skin;

    public MenuScreen(Game game){ this.game = game; }

    @Override public void show() {

        stage = new Stage(new FitViewport(1200,800));
        Gdx.input.setInputProcessor(stage);

        atlas = new TextureAtlas("UI/mainmenu_ui.atlas");

        skin = new Skin();
        skin.add("default-font", Fonts.HUD);
        skin.add("title-font",   Fonts.TITLE);

        TextButton.TextButtonStyle blue = new TextButton.TextButtonStyle();
        blue.up   = new TextureRegionDrawable(atlas.findRegion("Button_Blue_3Slides"));
        blue.down = new TextureRegionDrawable(atlas.findRegion("Button_Blue_3Slides_Pressed"));
        blue.font = Fonts.HUD;   skin.add("blue", blue);

        TextButton.TextButtonStyle red = new TextButton.TextButtonStyle();
        red.up   = new TextureRegionDrawable(atlas.findRegion("Button_Red_3Slides"));
        red.down = blue.down;
        red.font = Fonts.HUD;   skin.add("red", red);

        skin.add("title", new Label.LabelStyle(Fonts.TITLE, Color.WHITE));

        buildUI();
    }

    private void buildUI() {
        Table root = new Table(); root.setFillParent(true); stage.addActor(root);

        Texture bgTex = new Texture(Gdx.files.internal("HUD/background.png"));
        Image   bg    = new Image(new TextureRegionDrawable(new TextureRegion(bgTex)));
        bg.setFillParent(true); stage.addActor(bg);

        Table ui = new Table(); ui.setFillParent(true); ui.right().padRight(100f);
        stage.addActor(ui);

        ui.add(new Label("Balls of Steel", skin, "title"))
            .padBottom(40).padRight(50f).right().row();

        TextButton play = new TextButton("Play Game", skin, "blue");
        TextButton quit = new TextButton("Exit",      skin, "red");

        ui.add(play).width(320).height(90).pad(10).right().row();
        ui.add(quit).width(320).height(90).pad(10).right();

        play.addListener(new ClickListener(){
            @Override public void clicked(InputEvent e,float x,float y){
                stage.addAction(Actions.sequence(
                    Actions.fadeOut(.4f),
                    Actions.run(() -> game.setScreen(new PlayScreen(game)))
                ));
            }
        });
        quit.addListener(new ClickListener(){
            @Override public void clicked(InputEvent e,float x,float y){
                Gdx.app.exit();
            }
        });
    }

    @Override public void render(float dt){
        Gdx.gl.glClearColor(0.1f,0.1f,0.2f,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(dt); stage.draw();
    }

    @Override public void resize(int w,int h){ stage.getViewport().update(w,h,true); }
    @Override public void pause(){}  @Override public void resume(){}
    @Override public void hide(){}   @Override public void dispose(){
        stage.dispose(); atlas.dispose(); skin.dispose();
    }
}
