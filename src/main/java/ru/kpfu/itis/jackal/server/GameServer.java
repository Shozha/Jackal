package ru.kpfu.itis.jackal.server;

import ru.kpfu.itis.jackal.game.GameEngine;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameServer {

    private final int port;
    private ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private final GameEngine gameEngine;

    public GameServer(int port) {
        this.port = port;
        this.threadPool = Executors.newCachedThreadPool();
        this.gameEngine = new GameEngine();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Сервер запущен на порту " + port);

            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, gameEngine);
                threadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                e.printStackTrace();
            }
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                shutdown();
            }
        }
    }

    public void shutdown() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        threadPool.shutdownNow();
        System.out.println("Сервер остановлен");
    }

    public static void main(String[] args) {
        int port = 8888;
        GameServer server = new GameServer(port);
        server.start();
    }
}
