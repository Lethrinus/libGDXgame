package io.github.ballsofsteel.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import io.github.ballsofsteel.entity.*;
import io.github.ballsofsteel.events.EventBus;
import io.github.ballsofsteel.events.GameEvent;
import io.github.ballsofsteel.events.GameEventListener;
import io.github.ballsofsteel.events.GameEventType;
import io.github.ballsofsteel.factory.GameEntityFactory;

import java.util.*;

public final class WaveManager implements GameEventListener {

    /* -------- durum -------- */
    private enum State { PRE_WAVE, IN_WAVE, INTERVAL, COMPLETED }
    private State state = State.PRE_WAVE;

    /* -------- dalga arası bayrağı -------- */
    private static boolean intervalActive = false;
    public  static boolean isIntervalActive() { return intervalActive; }

    /* -------- dalga şablonu -------- */
    private static final class WaveSpec {
        final int gob, dyn, barrel;
        WaveSpec(int g,int d,int b){ gob=g; dyn=d; barrel=b; }
    }
    private final List<WaveSpec> waves = Arrays.asList(
            new WaveSpec(2,  0, 0),
            new WaveSpec(12,  4, 0),
            new WaveSpec(14,  5, 3),
            new WaveSpec(18, 10, 6)
    );

    /* -------- referans / sayaç -------- */
    private final CoreGame game;
    private final GameEntityFactory factory;
    private final Random rng = new Random();

    private int currentWave = -1;
    private float intervalTimer = 0f;
    private static final float INTERVAL_LEN = 30f;
    private static final float SPAWN_GAP    = 1.1f;

    private float spawnTimer = 0f;
    private final Deque<Runnable> queue = new ArrayDeque<>();

    public WaveManager(CoreGame g, GameEntityFactory f){
        game = g; factory = f;
        EventBus.register(this);
    }

    /* -------- update -------- */
    public void update(float dt){

        switch (state){

            case IN_WAVE:
                /* sırayla düşman çıkarmak */
                spawnTimer -= dt;
                if (spawnTimer <= 0f && !queue.isEmpty()) {
                    queue.pollFirst().run();
                    spawnTimer = SPAWN_GAP;
                }

                /* dalga bitti mi? */
                if (queue.isEmpty()
                        && game.getGoblins().isEmpty()
                        && game.getDynaList().isEmpty()
                        && game.getBarrels().isEmpty()) {

                    if (currentWave >= waves.size() - 1) {
                        state = State.COMPLETED;
                        intervalActive = false;
                        EventBus.post(new GameEvent(GameEventType.GAME_COMPLETED, null));
                    } else {
                        state = State.INTERVAL;
                        intervalActive = true;
                        intervalTimer = INTERVAL_LEN;
                    }
                }
                break;

            case INTERVAL:
                /* SPACE ile beklemeyi atla */
                if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
                    intervalTimer = 0f;
                intervalTimer -= dt;
                if (intervalTimer <= 0f) startNextWave();
                break;

            default: break;
        }
    }

    /* -------- EventBus -------- */
    @Override
    public void onEvent(GameEvent e) {
        if (e.getType() == GameEventType.WAVE_START_REQUEST && state == State.PRE_WAVE) {
            startNextWave();
        }
    }

    /* -------- yeni dalga -------- */
    public void startNextWave() {

        currentWave++;                              // indis güncel

        if (currentWave >= waves.size()) return;    // güvenlik

        WaveSpec s = waves.get(currentWave);

        queue.clear();
        enqueue(s.gob,    this::spawnGoblin);
        enqueue(s.dyn,    this::spawnDyna);
        enqueue(s.barrel, this::spawnBarrel);

        state = State.IN_WAVE;
        intervalActive = false;
        spawnTimer = 0f;
    }
    private void enqueue(int n, Runnable job){
        for (int i = 0; i < n; i++) queue.add(job);
    }

    /* -------- spawn yardımcıları -------- */
    private void spawnGoblin(){
        Vector2 p = randPos();
        game.addGoblin(factory.createGoblin(
                game.getPlayer(), game.getGoblins(), p.x, p.y, game.getLoot()));
    }
    private void spawnDyna(){
        Vector2 p = randPos();
        game.getDynaList().add(factory.createDynamiteGoblin(
                game.getPlayer(), game.getDynaList(), game.getLoot(), p.x, p.y));
    }
    private void spawnBarrel(){
        Vector2 p = randPos();
        game.getBarrels().add(factory.createBarrelBomber(
                game.getPlayer(), game.getBarrels(), p.x, p.y));
    }

    private Vector2 randPos(){
        float m = 2f;
        float w = game.getMap().getMapWidth()  - m*2;
        float h = game.getMap().getMapHeight() - m*2;
        return new Vector2(m + rng.nextFloat()*w,
                m + rng.nextFloat()*h);
    }
}
