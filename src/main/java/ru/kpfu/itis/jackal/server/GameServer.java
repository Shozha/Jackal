package ru.kpfu.itis.jackal.server;

import ru.kpfu.itis.jackal.game.GameEngine;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameServer {
    private static final int PORT = 8888;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private GameEngine gameEngine;

    public GameServer() {
        this.threadPool = Executors.newCachedThreadPool();
        this.gameEngine = new GameEngine();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Сервер запущен на порту " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, gameEngine);
                threadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new GameServer().start();
    }
}