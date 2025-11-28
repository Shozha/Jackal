package ru.kpfu.itis.jackal.server;

import lombok.Getter;
import lombok.Setter;
import ru.kpfu.itis.jackal.game.GameEngine;
import ru.kpfu.itis.jackal.network.protocol.GameMessage;
import ru.kpfu.itis.jackal.network.protocol.MessageParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private GameEngine gameEngine;
    private PrintWriter out;
    private BufferedReader in;
    @Setter
    @Getter
    private String playerId;

    public ClientHandler(Socket socket, GameEngine gameEngine) {
        this.clientSocket = socket;
        this.gameEngine = gameEngine;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                // Парсим сообщение
                GameMessage message = MessageParser.fromJson(inputLine);
                // Обрабатываем сообщение в игровом движке
                gameEngine.processMessage(message, this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(GameMessage message) {
        String json = MessageParser.toJson(message);
        out.println(json);
    }

}