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
    private Texture backgroundImage;
    private Texture ribbonImage;

    @Override
    public void create() {
        batch = new SpriteBatch();
        backgroundImage = new Texture(Gdx.files.internal("UI/Carved_9Slides.png"));
        ribbonImage = new Texture(Gdx.files.internal("UI/Ribbon_Blue_3Slides.png"));
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Homer_Simpson_Revised.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();

        font = generator.generateFont(parameter);
        generator.dispose(); // Generator'ı serbest bırak
        font.getData().setScale(3); // Yazıyı büyüt
    }

    @Override
    public void render() {
        ScreenUtils.clear(0, 0, 0, 1);
        batch.begin();
        batch.draw(backgroundImage, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.draw(ribbonImage, ((Gdx.graphics.getWidth()-500)/2), 650,
            500, 100);

        // Başlık
        font.setColor(Color.WHITE);
        font.draw(batch, "Balls of Steel", 475, 725); // Başlık konumu

        // Menü Seçeneği
        font.draw(batch, "Press ENTER to Start", 400, 300); // Menü seçeneği

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
