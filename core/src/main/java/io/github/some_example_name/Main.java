package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.List;

public class Main extends ApplicationAdapter {
    SpriteBatch batch;
    Texture playerTexture;
    Player player;
    List<Enemy> enemies;

    @Override
    public void create () {
        batch = new SpriteBatch();
        playerTexture = new Texture("bucket.png"); // Place player.png in your assets folder
        player = new Player(playerTexture, 100, 100);

        enemies = new ArrayList<>();

        // Create enemies using Factory Method pattern
        EnemyFactory chaseFactory = new ChaseEnemyFactory();
        EnemyFactory patrolFactory = new PatrolEnemyFactory();

        // Create enemies with different behaviors
        enemies.add(chaseFactory.createEnemy(300, 300));
        enemies.add(patrolFactory.createEnemy(500, 200));
    }

    @Override
    public void render () {
        // Update game logic
        player.update();
        for (Enemy enemy : enemies) {
            enemy.update(player);
        }

        // Render all objects
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        player.render(batch);
        for (Enemy enemy : enemies) {
            enemy.render(batch);
        }
        batch.end();
    }

    @Override
    public void dispose () {
        batch.dispose();
        playerTexture.dispose();
        for (Enemy enemy : enemies) {
            enemy.dispose();
        }
    }
}
