package io.github.some_example_name;

public class PatrolStrategy implements EnemyStrategy {
    private float speed = 80f;
    private float direction = 1f;
    private float patrolDistance = 100f;
    private float startX;
    private boolean initialized = false;

    @Override
    public void execute(Enemy enemy, Player player) {
        float delta = com.badlogic.gdx.Gdx.graphics.getDeltaTime();
        if (!initialized) {
            startX = enemy.getX();
            initialized = true;
        }
        float currentX = enemy.getX();
        enemy.setX(currentX + speed * direction * delta);
        if (Math.abs(currentX - startX) > patrolDistance) {
            direction *= -1;
        }
    }
}
