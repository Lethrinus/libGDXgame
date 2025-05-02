package io.github.theknightscrusade.events;
public class GameEvent {
    private final GameEventType type;
    private final Object payload;
    public GameEvent(GameEventType type, Object payload) {
        this.type = type; this.payload = payload;
    }
    public GameEventType getType() { return type; }
    public <T> T getPayload() { @SuppressWarnings("unchecked") T t = (T) payload; return t; }
}
