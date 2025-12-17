import ru.kpfu.itis.jackal.server.GameServer;

public class TestServer {
    public static void main(String[] args) {
        System.out.println("=== ЗАПУСК ТЕСТОВОГО СЕРВЕРА ===");
        GameServer server = new GameServer(8888);
        server.start();
    }
}