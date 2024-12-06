package ru.netology;

public class Main {
    public static void main(String[] args) {

        final var server = new Server(64);
        final var registry = new HandlerRegistry(server);

        registry.registerHandlers();
        server.listen(9999);
    }
}

