package io.github.some_example_name;

import com.badlogic.gdx.graphics.Texture;

public class PatrolEnemyFactory extends EnemyFactory {
    @Override
    public Enemy createEnemy(float x, float y) {
        Texture enemyTexture = new Texture("drop.png"); // Place enemy_patrol.png in your assets folder
        return new Enemy(enemyTexture, x, y, new PatrolStrategy());
    }
}
