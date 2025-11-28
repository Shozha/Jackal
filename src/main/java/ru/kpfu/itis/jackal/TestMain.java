package ru.kpfu.itis.jackal;

import ru.kpfu.itis.jackal.network.protocol.GameMessage;
import ru.kpfu.itis.jackal.network.protocol.MessageParser;
import ru.kpfu.itis.jackal.network.protocol.MessageType;
import ru.kpfu.itis.jackal.network.protocol.PlayerJoinData;

public class TestMain {

    public static void main(String[] args) {

        PlayerJoinData joinData = new PlayerJoinData("Сергей", "RED");
        GameMessage message = new GameMessage(MessageType.PLAYER_JOIN, "player1",
                MessageParser.dataToJson(joinData));

        String json = MessageParser.toJson(message);
        System.out.println("Отправляем: " + json);

        GameMessage received = MessageParser.fromJson(json);
        PlayerJoinData receivedData = MessageParser.dataFromJson(received.getData(), PlayerJoinData.class);
        System.out.println("Получили: " + receivedData.getPlayerName());

    }

}
