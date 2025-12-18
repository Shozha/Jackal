package ru.kpfu.itis.jackal.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import ru.kpfu.itis.jackal.game.GameEngine;
import ru.kpfu.itis.jackal.network.protocol.GameMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private GameEngine gameEngine;
    private PrintWriter out;
    private BufferedReader in;
    private static final Gson gson = new GsonBuilder().create();
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
            out = new PrintWriter(new java.io.OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8),
                    true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                try {
                    GameMessage message = gson.fromJson(inputLine, GameMessage.class);
                    gameEngine.processMessage(message, this);
                } catch (Exception e) {
                    System.err.println("[ClientHandler] Ошибка парсинга: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
                clientSocket.close();
                gameEngine.onClientDisconnect(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(GameMessage message) {
        String json = gson.toJson(message);
        out.println(json);
        out.flush();
    }
}