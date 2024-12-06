package ru.netology;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class HandlerRegistry {

    private final Server server;
    private final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    public HandlerRegistry(Server server) {
        this.server = server;
    }

    public void registerHandlers() {
        registerFiles();
    }

    private void registerFiles() {
        for(var path: validPaths) {
            server.addHandler("GET", path, (request, responseStream) -> {
                try {
                    Path filePath = Path.of(".", "public", path);
                    var mimeType = Files.probeContentType(filePath);
                    sendResponse(responseStream, filePath, mimeType, path);
                }catch (IOException e){
                    e.printStackTrace();
                }
            });
        }

    }

    private void sendResponse(BufferedOutputStream out, Path filePath, String mimeType, String path) throws IOException {
        if (path.equals("/classic.html")) {
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            prepareResponse(out, mimeType, content.length, content);
        } else {
            final var length = Files.size(filePath);
            prepareResponse(out, mimeType, length, filePath);
        }
    }

    private void sendInvalidPathError(OutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private String prepareHeaders(String contentType, long contentLength) {
        return String.format(
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: %s\r\n" +
                        "Content-Length: %d\r\n" +
                        "Connection: close\r\n" +
                        "\r\n",
                contentType, contentLength
        );
    }

    private void prepareResponse(BufferedOutputStream out, String contentType, long contentLength, Path filePath) throws IOException {
        out.write(prepareHeaders(contentType, contentLength).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }

    private void prepareResponse(BufferedOutputStream out, String contentType, long contentLength, byte[] content) throws IOException {
        out.write(prepareHeaders(contentType, contentLength).getBytes());
        out.write(content);
        out.flush();
    }
}
