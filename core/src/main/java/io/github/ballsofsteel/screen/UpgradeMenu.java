package io.github.ballsofsteel.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import io.github.ballsofsteel.events.*;
import io.github.ballsofsteel.ui.Fonts;

/** Basit ama şık yükseltme menüsü – getLabel() kullanılmaz. */
public final class UpgradeMenu implements GameEventListener {

    private final Stage stage;
    private final Skin  skin;
    private Window  root;

    private boolean visible = false;
    private Player  player;

    public UpgradeMenu() {

        /* ---------- Stage ---------- */
        stage = new Stage(new ScreenViewport());

        /* ---------- Skin ---------- */
        skin = new Skin();
        skin.add("default-font", Fonts.HUD);

        /* — Tek renkli 1×1 doku — */
        Pixmap pm = new Pixmap(1,1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE); pm.fill();
        Texture white = new Texture(pm); pm.dispose();
        skin.add("white", white);

        Label.LabelStyle lblStyle = new Label.LabelStyle();
        lblStyle.font  = Fonts.HUD;          // hangi fontu istiyorsanız
        lblStyle.fontColor = Color.WHITE;
        skin.add("default", lblStyle);       // <--


        /* ---------- Styles ---------- */
        TextButton.TextButtonStyle btn = new TextButton.TextButtonStyle();
        btn.font = Fonts.HUD;
        btn.up   = skin.newDrawable("white", new Color(0.2f, 0.3f, 0.4f, 0.95f));  // koyu mavi
        btn.over = skin.newDrawable("white", new Color(0.3f, 0.4f, 0.6f, 1.0f));   // hover rengi
        btn.down = skin.newDrawable("white", new Color(0.1f, 0.2f, 0.3f, 1.0f));   // basıldığında
        btn.fontColor = Color.WHITE;
        skin.add("upgrade", btn);

        Window.WindowStyle win = new Window.WindowStyle();
        win.titleFont = Fonts.TITLE;
        win.background = skin.newDrawable("white", new Color(0f, 0f, 0f, 0.85f));  // yarı saydam siyah
        skin.add("root", win);

        buildUI();
        EventBus.register(this);
    }

    /* ---------- pencere & butonlar ---------- */
    private void buildUI(){
        root = new Window("", skin, "root");
        root.pad(40);
        root.setModal(true); root.setMovable(false);

        Label title = new Label("CHOOSE AN UPGRADE", skin);
        title.setFontScale(1.4f);                        // büyük yazı
        title.setAlignment(Align.center);
        root.add(title).padBottom(40).center().row();

        addButton("1  :  +50 HEALTH", Upgrade.HEALTH);
        addButton("2  :  +50% DAMAGE", Upgrade.DAMAGE);
        addButton("3  :  +50% SPEED" , Upgrade.SPEED );

        root.pack();
        root.setPosition(
            (Gdx.graphics.getWidth()  - root.getWidth())  /2f,
            (Gdx.graphics.getHeight() - root.getHeight()) /2f);
        stage.addActor(root);

        // Animasyonlu giriş burada olmalı
        root.setColor(1,1,1,0);
        root.setScale(0.9f);
        root.setVisible(true);
        root.addAction(Actions.parallel(
            Actions.fadeIn(.25f),
            Actions.scaleTo(1f,1f,.25f)
        ));

        root.setVisible(false); // en son false yapılır
    }

    private void addButton(String text, Upgrade up){
        TextButton b = new TextButton(text, skin, "upgrade");
        b.pad(16,60,16,60);  // daha büyük padding
        b.getLabel().setFontScale(1.2f);  // yazı boyutu
        root.add(b).width(500).pad(12).center().row();
        b.addListener(new ClickListener(){
            @Override public void clicked(InputEvent e,float x,float y){ apply(up); }
        });
    }


    /* ---------- göster / gizle ---------- */
    public void show(Player p){
        if (visible) return;
        player = p; visible = true;

        Gdx.input.setInputProcessor(stage);
        root.setColor(1,1,1,0); root.setVisible(true);
        root.addAction(Actions.fadeIn(.25f));
    }
    private void hide(){
        if (!visible) return;
        visible = false;
        root.addAction(Actions.sequence(
            Actions.fadeOut(.2f), Actions.visible(false)));
        Gdx.input.setInputProcessor(null);
    }
    public boolean isVisible(){ return visible; }

    /* ---------- kısayol tuşları ---------- */
    public void handleInput(){
        if (!visible) return;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) apply(Upgrade.HEALTH);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) apply(Upgrade.DAMAGE);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) apply(Upgrade.SPEED );
    }

    /* ---------- uygulama & Event göndermek ---------- */
    private enum Upgrade{HEALTH,DAMAGE,SPEED}
    private void apply(Upgrade u){
        switch(u){
            case HEALTH: player.increaseHealth(50f);        break;
            case DAMAGE: player.increaseAttackDamage(1.5f); break;
            case SPEED : player.increaseMoveSpeed(1.5f);    break;
        }
        EventBus.post(new GameEvent(GameEventType.UPGRADE_SELECTED, u.name()));
        hide();
    }

    /* ---------- çizim ---------- */
    public void render(SpriteBatch sb) {
        if (!visible) return;
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    /* ---------- temizle ---------- */
    public void dispose(){ stage.dispose(); skin.dispose(); }

    @Override public void onEvent(GameEvent e){ /* gerekirse kullan */ }
}
