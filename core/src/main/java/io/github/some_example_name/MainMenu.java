package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;

public class MainMenu extends ApplicationAdapter {
    private Stage stage;
    private TextureAtlas atlas;
    private Skin skin;
    private BitmapFont font, headlineFont;
    private Image bannerImage, ribbonImage;
    private Label titleLabel;
    private Table rootTable, buttonTable;
    private TextButton startBtn, loadBtn, optionsBtn, quitBtn;

    @Override
    public void create() {
        // Stage ve viewport
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Atlas yükle (assets klasöründe mainmenu_ui.atlas ve mainmenu_ui.png olduğundan emin ol)
        atlas = new TextureAtlas(Gdx.files.internal("UI/mainmenu_ui.atlas"));

        // Fontları yükleyelim
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Homer_Simpson_Revised.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 32;
        font = generator.generateFont(parameter);
        parameter.size = 64;
        headlineFont = generator.generateFont(parameter);
        generator.dispose();

        // Skin oluşturma: Fontları ve stilleri ekleyelim
        skin = new Skin();
        skin.add("default-font", font);
        skin.add("headline-font", headlineFont);

        // ----------------------------------------------------------------
        // 1) BUTON STİLLERİ
        // ----------------------------------------------------------------
        Drawable buttonBlueUp = new TextureRegionDrawable(atlas.findRegion("Button_Blue_3Slides"));
        Drawable buttonBlueDown = new TextureRegionDrawable(atlas.findRegion("Button_Blue_3Slides_Pressed"));
        TextButtonStyle blueButtonStyle = new TextButtonStyle();
        blueButtonStyle.up = buttonBlueUp;
        blueButtonStyle.down = buttonBlueDown;
        blueButtonStyle.font = font;
        blueButtonStyle.fontColor = Color.WHITE;
        blueButtonStyle.overFontColor = Color.YELLOW;
        skin.add("blueButtonStyle", blueButtonStyle);

        Drawable buttonRedUp = new TextureRegionDrawable(atlas.findRegion("Button_Red_3Slides"));
        TextButtonStyle redButtonStyle = new TextButtonStyle();
        redButtonStyle.up = buttonRedUp;
        redButtonStyle.down = buttonBlueDown; // Örneğin basılı halleri için maviyi kullanıyoruz
        redButtonStyle.font = font;
        redButtonStyle.fontColor = Color.WHITE;
        redButtonStyle.overFontColor = Color.YELLOW;
        skin.add("redButtonStyle", redButtonStyle);

        // ----------------------------------------------------------------
        // 2) LABEL STİLLERİ
        // ----------------------------------------------------------------
        Label.LabelStyle headlineStyle = new Label.LabelStyle();
        headlineStyle.font = headlineFont;
        headlineStyle.fontColor = Color.WHITE;
        skin.add("headlineStyle", headlineStyle);

        Label.LabelStyle defaultLabelStyle = new Label.LabelStyle();
        defaultLabelStyle.font = font;
        defaultLabelStyle.fontColor = Color.WHITE;
        skin.add("defaultLabel", defaultLabelStyle);

        // ----------------------------------------------------------------
        // 3) ARKAPLAN, BANNER, RIBBON GÖRSELLERİ
        // ----------------------------------------------------------------
        // Banner_Horizontal region'unu Image olarak kullanalım
        bannerImage = new Image(atlas.findRegion("Banner_Horizontal"));
        // Ribbon: Ribbon_Blue_3Slides
        ribbonImage = new Image(atlas.findRegion("Ribbon_Blue_3Slides"));

        // ----------------------------------------------------------------
        // 4) ROOT TABLE - ANA LAYOUT
        // ----------------------------------------------------------------
        rootTable = new Table();
        rootTable.setFillParent(true);
        // Başlangıçta alfa değeri 0, fade in ile görünecek
        rootTable.getColor().a = 0;
        rootTable.addAction(Actions.fadeIn(1f, Interpolation.fade));
        stage.addActor(rootTable);

        // ----------------------------------------------------------------
        // 5) ÜST BÖLÜM: BANNER, RIBBON, BAŞLIK
        // ----------------------------------------------------------------
        Table topTable = new Table();
        topTable.add(bannerImage).width(300).height(150).pad(10);
        topTable.row();
        topTable.add(ribbonImage).width(300).height(64).pad(5);
        topTable.row();
        titleLabel = new Label("Balls of Steel", skin, "headlineStyle");
        topTable.add(titleLabel).pad(5);
        // Başlık için pulsating (nabız efekti) animasyon
        titleLabel.addAction(Actions.forever(
            Actions.sequence(
                Actions.scaleTo(1.05f, 1.05f, 1f, Interpolation.sineOut),
                Actions.scaleTo(1f, 1f, 1f, Interpolation.sineIn)
            )
        ));

        rootTable.top();
        rootTable.add(topTable).padTop(50).center();
        rootTable.row();

        // ----------------------------------------------------------------
        // 6) BUTONLAR: ORTA BÖLÜM
        // ----------------------------------------------------------------
        buttonTable = new Table();
        buttonTable.defaults().pad(10).width(300).height(80);
        startBtn = new TextButton("Start Game", skin, "blueButtonStyle");
        loadBtn = new TextButton("Load Game", skin, "blueButtonStyle");
        optionsBtn = new TextButton("Options", skin, "blueButtonStyle");
        quitBtn = new TextButton("Quit", skin, "redButtonStyle");

        // Butonlara hover (fare ile üzerine gelince) animasyonu ekleyelim
        addHoverAnimation(startBtn);
        addHoverAnimation(loadBtn);
        addHoverAnimation(optionsBtn);
        addHoverAnimation(quitBtn);

        // Butonlara tıklama eventleri ekleyelim
        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                System.out.println("Start Game clicked!");
                // Örneğin: Sahne geçişi için fade out animasyonu eklenebilir
                stage.addAction(Actions.sequence(Actions.fadeOut(0.5f), Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        // Buraya yeni ekran geçiş kodunu ekle
                    }
                })));
            }
        });
        loadBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                System.out.println("Load Game clicked!");
            }
        });
        optionsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                System.out.println("Options clicked!");
            }
        });
        quitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        // Butonları dikey olarak ekleyelim
        buttonTable.add(startBtn).row();
        buttonTable.add(loadBtn).row();
        buttonTable.add(optionsBtn).row();
        buttonTable.add(quitBtn).row();

        // Root table'e buton tablosunu ekleyelim
        rootTable.add(buttonTable).expand().center();

        // Ekstra: Sahnenin tümüne giriş animasyonu (fade in) ekleniyor
        stage.addAction(Actions.sequence(Actions.alpha(0), Actions.fadeIn(1.5f)));
    }

    // Butonlara hover animasyonu ekleyen yardımcı metod
    private void addHoverAnimation(final Actor actor) {
        actor.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                actor.addAction(Actions.scaleTo(1.1f, 1.1f, 0.2f, Interpolation.fade));
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                actor.addAction(Actions.scaleTo(1f, 1f, 0.2f, Interpolation.fade));
            }
        });
    }

    @Override
    public void render() {
        ScreenUtils.clear(0, 0, 0, 1);
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        atlas.dispose();
        skin.dispose();
        font.dispose();
        headlineFont.dispose();
    }
}
