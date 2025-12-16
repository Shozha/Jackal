package ru.kpfu.itis.jackal.test;

import ru.kpfu.itis.jackal.server.GameServer;

public class TestServer {
    public static void main(String[] args) {
        System.out.println("=== ЗАПУСК ТЕСТОВОГО СЕРВЕРА ===");
        GameServer server = new GameServer();
        server.start();
    }
}