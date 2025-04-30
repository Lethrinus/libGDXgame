package io.github.ballsofsteel.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import io.github.ballsofsteel.entity.*;
import io.github.ballsofsteel.events.*;
import io.github.ballsofsteel.factory.GameEntityFactory;

import java.util.*;

public final class WaveManager implements GameEventListener {

    /* ---------- durum ---------- */
    private enum State { PRE_WAVE, IN_WAVE, INTERVAL, COMPLETED }
    private State state = State.PRE_WAVE;

    private static boolean intervalActive = false;
    public  static boolean isIntervalActive() { return intervalActive; }
    private final SpawnManager spawnManager = new SpawnManager();
    /* ---------- dalga şablonu ---------- */
    private static final class WaveSpec {
        final int gob, dyn, barrel;
        WaveSpec(int g,int d,int b){ gob=g; dyn=d; barrel=b; }
    }

    private final List<WaveSpec> waves = Arrays.asList(
        new WaveSpec( 24,0, 0),
        new WaveSpec(0, 1, 0),
        new WaveSpec(0, 0, 1),
        new WaveSpec(18,10, 6)
    );

    /* ---------- referans / sayaç ---------- */
    private final CoreGame           game;
    private final GameEntityFactory  factory;
    private final Random rng = new Random();



    private static final int   MAX_ALIVE    = 12;
    private static final float INTERVAL_LEN = 30f;
    private static final float SPAWN_GAP    = 1.3f;
    private static final float COUNTDOWN_GAP= 1f;


    private int   currentWave  = -1;
    private float intervalTimer= 0f;
    private float spawnTimer   = 0f;

    private final Deque<Runnable> queue = new ArrayDeque<>();

    /* ----- geri sayım ----- */
    private int   countNum   = 0;     // 3-2-1-0
    private float countTimer = 0f;
    private boolean counting = false;

    public WaveManager(CoreGame g, GameEntityFactory f){
        game = g; factory = f;
        EventBus.register(this);

        spawnManager.addSpawnPoint(44, 25);

    }


    /* ======================== update ======================== */
    public void update(float dt) {

        /* 1) geri sayım */
        if (counting){
            countTimer += dt;
            if (countTimer >= COUNTDOWN_GAP){
                countTimer = 0f;
                countNum--;
                EventBus.post(new GameEvent(GameEventType.WAVE_COUNTDOWN, countNum));
                if (countNum < 0) counting = false;  // GO!
            }
        }

        /* 2) dalga durumu */
        switch (state){

            case IN_WAVE:
                handleInWave(dt);
                break;

            case INTERVAL:
                handleInterval(dt);
                break;

            default:
                break;
        }
    }

    /* =========== IN_WAVE =========== */
    private void handleInWave(float dt){

        if (counting) return;      // Sayım bitene kadar spawn yok.

        spawnTimer -= dt;
        int alive = game.getGoblins().size()
            + game.getDynaList().size()
            + game.getBarrels().size();

        if (spawnTimer <= 0f && !queue.isEmpty() && alive < MAX_ALIVE) {
            queue.pollFirst().run();
            spawnTimer = SPAWN_GAP;
        }

        /* Dalga bitti mi? */
        if (queue.isEmpty()
            && game.getGoblins().isEmpty()
            && game.getDynaList().isEmpty()
            && game.getBarrels().isEmpty()) {

            if (currentWave >= waves.size()-1){
                state = State.COMPLETED;
                intervalActive = false;
                EventBus.post(new GameEvent(GameEventType.GAME_COMPLETED,null));
            } else {
                state = State.INTERVAL;
                intervalActive = true;
                intervalTimer  = INTERVAL_LEN;
                game.getNpc().setPointerVisible(true);
            }
        }
    }

    /* ========== INTERVAL ========== */
    private void handleInterval(float dt){
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            intervalTimer = 0f;
        intervalTimer -= dt;
        if (intervalTimer <= 0f) startNextWave();
    }

    /* ========== EventBus ========== */
    @Override
    public void onEvent(GameEvent e){
        if (e.getType()==GameEventType.WAVE_START_REQUEST && state==State.PRE_WAVE)
            startNextWave();
    }

    /* ========== yeni dalga ========== */
    public void startNextWave(){

        currentWave++;
        if (currentWave >= waves.size()) return;

        WaveSpec s = waves.get(currentWave);
        queue.clear();
        enqueue(s.gob   , this::spawnGoblin);
        enqueue(s.dyn   , this::spawnDyna);
        enqueue(s.barrel, this::spawnBarrel);

        counting   = true;     // 3-2-1-GO
        countNum   = 3;
        countTimer = 0f;
        EventBus.post(new GameEvent(GameEventType.WAVE_COUNTDOWN,3));

        state          = State.IN_WAVE;
        intervalActive = false;
        spawnTimer     = SPAWN_GAP;  //

        game.getNpc().setPointerVisible(false);
    }

    private void enqueue(int n, Runnable r){ for(int i=0;i<n;i++) queue.add(r); }

    /* ----- spawn helpers ----- */
    private void spawnGoblin(){
        Vector2 spawn = spawnManager.getRandomSpawnPoint();
        game.getGoblins().add(factory.createGoblin(
            game.getPlayer(), game, game.getGoblins(), spawn.x, spawn.y, game.getLoot()));
    }

    private void spawnDyna(){
        Vector2 spawn = spawnManager.getRandomSpawnPoint();
        game.getDynaList().add(factory.createDynamiteGoblin(
            game.getPlayer(), game, game.getDynaList(), game.getLoot(), spawn.x, spawn.y));
    }

    private void spawnBarrel(){
        Vector2 spawn = spawnManager.getRandomSpawnPoint();
        game.getBarrels().add(factory.createBarrelBomber(
            game.getPlayer(), game, game.getBarrels(), spawn.x, spawn.y));
    }

}

