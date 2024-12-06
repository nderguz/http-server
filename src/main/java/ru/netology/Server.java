package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final int port;
    private final ExecutorService threadPool;
    private final List<String> validPaths;

    public Server(int threadPoolSize, List<String> validPaths, int port) {
        this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
        this.validPaths = validPaths;
        this.port = port;
    }

    public void run() {
        try (final var socket = new ServerSocket(port)) {
            while (true) {
                final var clientRequest = socket.accept();
                threadPool.submit(() -> executeRequest(clientRequest));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void executeRequest(Socket socket) {
        try (final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            final var requestParts = processRequest(in);

            if (requestParts.length != 3) {
                return;
            }

            final var path = requestParts[1];

            if (!validPaths.contains(path)) {
                sendInvalidPathError(out);
                return;
            }

            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);

            sendResponse(out, filePath, mimeType, path);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String[] processRequest(BufferedReader in) throws IOException {
        return in.readLine().split(" ");
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

    private String prepareHeaders(String contentType, long contentLength){
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

