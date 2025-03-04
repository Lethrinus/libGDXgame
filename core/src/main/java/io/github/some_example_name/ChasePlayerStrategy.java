package io.github.some_example_name;

public class ChasePlayerStrategy implements EnemyStrategy {
    private float speed = 100f;

    @Override
    public void execute(Enemy enemy, Player player) {
        float delta = com.badlogic.gdx.Gdx.graphics.getDeltaTime();
        float enemyX = enemy.getX();
        float enemyY = enemy.getY();
        float playerX = player.getX();
        float playerY = player.getY();

        float dx = playerX - enemyX;
        float dy = playerY - enemyY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance != 0) {
            enemy.setX(enemyX + (dx / distance) * speed * delta);
            enemy.setY(enemyY + (dy / distance) * speed * delta);
        }
    }
}
