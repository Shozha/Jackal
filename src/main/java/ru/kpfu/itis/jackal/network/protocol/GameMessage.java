package ru.kpfu.itis.jackal.network.protocol;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameMessage {
    private MessageType type;
    private String playerId;
    private String data; // JSON строка с данными
    private long timestamp;

    public GameMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public GameMessage(MessageType type, String playerId, String data) {
        this();
        this.type = type;
        this.playerId = playerId;
        this.data = data;
    }
}
