package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class MainMenu extends ApplicationAdapter {
    private SpriteBatch batch;
    private BitmapFont font;
    private BitmapFont headlineFont;
    private Texture backgroundImage;
    private Texture ribbonImage;
    private Texture buttonImage1;
    private Texture buttonImage2;
    private Texture buttonImage3;
    private Texture buttonImage4;


    @Override
    public void create() {
        batch = new SpriteBatch();
        backgroundImage = new Texture(Gdx.files.internal("UI/Banner_Horizontal.png"));
        ribbonImage = new Texture(Gdx.files.internal("UI/Ribbon_Blue_3Slides.png"));
        buttonImage1 = new Texture(Gdx.files.internal("UI/Button_Blue_3Slides.png"));
        buttonImage2 = new Texture(Gdx.files.internal("UI/Button_Blue_3Slides.png"));
        buttonImage3 = new Texture(Gdx.files.internal("UI/Button_Blue_3Slides.png"));
        buttonImage4 = new Texture(Gdx.files.internal("UI/Button_Red_3Slides.png"));
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Homer_Simpson_Revised.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();

        font = generator.generateFont(parameter);
        headlineFont = generator.generateFont(parameter);
        generator.dispose(); // Generator'ı serbest bırak
        headlineFont.getData().setScale(4);
        font.getData().setScale(3); // Yazıyı büyüt
    }

    @Override
    public void render() {
        ScreenUtils.clear(0, 0, 0, 1);
        batch.begin();
        batch.draw(backgroundImage, -150, -105, 1500, 925);
        batch.draw(ribbonImage, ((Gdx.graphics.getWidth()-700)/2), 600, 700, 125);
        batch.draw(buttonImage1, ((Gdx.graphics.getWidth()-500)/2), 450, 500, 105);
        batch.draw(buttonImage2, ((Gdx.graphics.getWidth()-500)/2), 350, 500, 105);
        batch.draw(buttonImage3, ((Gdx.graphics.getWidth()-500)/2), 250, 500, 105);
        batch.draw(buttonImage4, ((Gdx.graphics.getWidth()-500)/2), 150, 500, 105);

        // Başlık
        headlineFont.setColor(Color.WHITE);
        headlineFont.draw(batch, "Balls of Steel", 440, 700); // Başlık konumu

        // Menü Seçeneği
        font.draw(batch, "Start Game", 500, 530); // Menü konumu
        font.draw(batch, "Load Game", 500, 430); // Load konumu
        font.draw(batch, "Options", 535, 330); // Options konumu
        font.draw(batch, "Quit", 555, 230); // Quit konumu
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }


    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
