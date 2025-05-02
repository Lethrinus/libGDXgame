package io.github.theknightscrusade.events;
import java.util.concurrent.CopyOnWriteArrayList;
public final class EventBus {
    private static final EventBus INSTANCE = new EventBus();
    private static final CopyOnWriteArrayList<GameEventListener> listeners = new CopyOnWriteArrayList<>();
    private EventBus() {}
    public static EventBus get() { return INSTANCE; }
    public static void register(GameEventListener l){ listeners.addIfAbsent(l); }
    public static void unregister(GameEventListener l){ listeners.remove(l); }
    public static void post(GameEvent e){ listeners.forEach(l -> l.onEvent(e)); }

}
