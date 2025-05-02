package io.github.ballsofsteel.core;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;


public class SpawnManager {

    private final List<Vector2> spawnPoints = new ArrayList<>();

    public void addSpawnPoint(float x, float y) {
        spawnPoints.add(new Vector2(x, y));
    }

    public Vector2 getRandomSpawnPoint() {
        if (spawnPoints.isEmpty()) return new Vector2(5, 5); // fallback default
        return spawnPoints.get(MathUtils.random(spawnPoints.size() - 1));
    }
}
