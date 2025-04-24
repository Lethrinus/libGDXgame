// src/io/github/some_example_name/events/EventBus.java
package io.github.ballsofsteel.events;
import java.util.concurrent.CopyOnWriteArrayList;
public final class EventBus {
    private static final EventBus INSTANCE = new EventBus();
    private final CopyOnWriteArrayList<GameEventListener> listeners = new CopyOnWriteArrayList<>();
    private EventBus() {}
    public static EventBus get() { return INSTANCE; }
    public void register(GameEventListener l){ listeners.addIfAbsent(l); }
    public void unregister(GameEventListener l){ listeners.remove(l); }
    public void post(GameEvent e){ listeners.forEach(l -> l.onEvent(e)); }
}
