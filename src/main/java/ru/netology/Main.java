package ru.netology;

import java.util.List;

public class Main {

    private static final List<String> PATH_LIST = List.of(
            "/index.html",
            "/spring.svg",
            "/spring.png",
            "/resources.html",
            "/styles.css",
            "/app.js",
            "/links.html",
            "/forms.html",
            "/classic.html",
            "/events.html",
            "/events.js");

    public static void main(String[] args) {

        final var server = new Server(64);
        final var registry = new HandlerRegistry(server, PATH_LIST);

        registry.registerHandlers();
        server.listen(9999);
    }
}

