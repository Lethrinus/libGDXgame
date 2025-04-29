package io.github.ballsofsteel.core;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;

public class CameraShake {

    private final OrthographicCamera camera;

    private float duration = 0f;
    private float timeLeft = 0f;
    private float power = 0f;

    public CameraShake(OrthographicCamera camera) {
        this.camera = camera;
    }

    public void shake(float power, float duration) {
        this.power = power;
        this.duration = duration;
        this.timeLeft = duration;
    }

    public void update(float delta) {
        if (timeLeft > 0f) {
            timeLeft -= delta;

            float currentPower = power * (timeLeft / duration); // azalmalı
            float offsetX = (MathUtils.random() - 0.5f) * 2f * currentPower;
            float offsetY = (MathUtils.random() - 0.5f) * 2f * currentPower;

            camera.translate(offsetX, offsetY);
        }
    }
}
