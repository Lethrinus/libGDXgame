package io.github.theknightscrusade.core;

import com.badlogic.gdx.math.Vector2;
import io.github.theknightscrusade.events.*;
import io.github.theknightscrusade.factory.GameEntityFactory;

import java.util.*;

public final class WaveManager implements GameEventListener {

    //states
    public enum State { PRE_WAVE, IN_WAVE, INTERVAL, COMPLETED }
    private State state = State.PRE_WAVE;

    private static boolean intervalActive = false;

    private final SpawnManager spawnManager = new SpawnManager();

    public int getCurrentWave() { return currentWave;}
    public boolean isNpcInteractable() {return state == State.PRE_WAVE || state == State.INTERVAL;}
    public  static boolean isIntervalActive() { return intervalActive; }

    // wave  blueprint
    private static final class WaveSpec {
        final int gob, dyn, barrel;
        WaveSpec(int g,int d,int b){ gob=g; dyn=d; barrel=b; }
    }

    private final List<WaveSpec> waves = Arrays.asList(
        new WaveSpec(0, 0, 0), // pre-start
        new WaveSpec(7, 0, 0), // wave 0: Goblins
        new WaveSpec(3, 5, 0), // wave 1: DynamiteGoblin
        new WaveSpec(1, 3, 4), // wave 2: BarrelBomber
        new WaveSpec(5, 5, 5)  // wave 3: mixed, if needed
    );

    // references and counters
    private final CoreGame           game;
    private final GameEntityFactory  factory;


    private static final int   MAX_ALIVE    = 7;
    private static final float SPAWN_GAP    = 1.5f;
    private static final float COUNTDOWN_GAP= 1f;


    private int   currentWave  = -1;
    private float spawnTimer   = 0f;

    private final Deque<Runnable> queue = new ArrayDeque<>();

    // countdown
    private int   countNum   = 0;     // 3-2-1-0
    private float countTimer = 0f;
    private boolean counting = false;

    public WaveManager(CoreGame g, GameEntityFactory f){
        game = g; factory = f;
        EventBus.register(this);
        spawnManager.addSpawnPoint(44, 5);
        spawnManager.addSpawnPoint(15, 5);
        spawnManager.addSpawnPoint(28, 5);
        spawnManager.addSpawnPoint(16, 2);
        spawnManager.addSpawnPoint(4, 10);
        spawnManager.addSpawnPoint(50, 8);

    }


    // update //
    public void update(float dt) {
        switch (state) {
            case PRE_WAVE:
                handlePreWave(dt);
                break;
            case IN_WAVE:
                handleInWave(dt);
                break;
            case INTERVAL:
                break;
            case COMPLETED:
                // No-op
                break;
        }
        // Handle countdown for all states if needed
        if (counting) {
            countTimer += dt;
            if (countTimer >= COUNTDOWN_GAP) {
                countTimer = 0f;
                countNum--;
                EventBus.post(new GameEvent(GameEventType.WAVE_COUNTDOWN, countNum));
                if (countNum < 0) counting = false;
            }
        }
    }

    // pre - wave
    private void handlePreWave(float dt) {

    }

    // in - wave
    private void handleInWave(float dt) {
        if (counting) return;

        spawnTimer -= dt;
        int alive = game.getGoblins().size() + game.getDynaList().size() + game.getBarrels().size();

        if (spawnTimer <= 0f && !queue.isEmpty() && alive < MAX_ALIVE) {
            Runnable spawn = queue.poll();
            if (spawn != null) spawn.run();
            spawnTimer = SPAWN_GAP;
        }

        // check if all enemies are dead or game complete
        if (queue.isEmpty() && alive == 0) {
            if (currentWave + 1 >= waves.size()) {
                state = State.COMPLETED;
                intervalActive = false;
                EventBus.post(new GameEvent(GameEventType.GAME_COMPLETED, null));
            } else {
                state = State.INTERVAL;
                intervalActive = true;
                game.getNpc().setPointerVisible(true);
            }
        }
    }


    // eventbus
    @Override
    public void onEvent(GameEvent e) {
        if (e.getType() == GameEventType.WAVE_START_REQUEST) {
            if (state == State.PRE_WAVE || state == State.INTERVAL) {
                startNextWave();
            }
        }
    }

    // new wave start
    public void startNextWave() {
        currentWave++;
        if (currentWave >= waves.size()) {
            state = State.COMPLETED;
            intervalActive = false;
            return;
        }

        WaveSpec s = waves.get(currentWave);
        queue.clear();
        enqueue(s.gob, this::spawnGoblin);
        enqueue(s.dyn, this::spawnDyna);
        enqueue(s.barrel, this::spawnBarrel);

        counting = true;
        countNum = 3;
        countTimer = 0f;
        EventBus.post(new GameEvent(GameEventType.WAVE_COUNTDOWN, 3));

        state = State.IN_WAVE;
        intervalActive = false;
        spawnTimer = SPAWN_GAP;

        game.getNpc().setPointerVisible(false);
    }

    private void enqueue(int n, Runnable r) { for (int i = 0; i < n; i++) queue.add(r); }


    // spawn helpers //
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

