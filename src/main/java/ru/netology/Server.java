package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final ExecutorService threadPool;
    private final Map<String, Map<String, Handler>> handlers;

    public Server(int threadPoolSize) {
        this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
        this.handlers = new ConcurrentHashMap<>();
    }

    public void listen(int port) {
        try (final var socket = new ServerSocket(port)) {
            while (true) {
                final var clientRequest = socket.accept();
                threadPool.submit(() -> executeRequest(clientRequest));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addHandler(String methodType, String path, Handler handler) {
        handlers.computeIfAbsent(methodType.toUpperCase(), k -> new ConcurrentHashMap<>())
                .put(path, handler);
    }

    private void executeRequest(Socket socket) {
        try (final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            Request request = parseRequest(in);
            if (request == null) {
                sendInvalidPathError(out);
                return;
            }

            Handler handler = handlers.getOrDefault(request.getMethod(), Collections.emptyMap())
                    .get(request.getPath());
            if (handler == null) {
                sendInvalidPathError(out);
                return;
            }

            handler.handle(request, out);
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

    private Request parseRequest(BufferedReader in) throws IOException {
        final var requestParts = processRequest(in);

        if (requestParts.length != 3) {
            return null;
        }

        final var method = requestParts[0];
        final var path = requestParts[1];

        Map<String, String> headers = new HashMap<>();
        String line;
        while ((line = in.readLine()) != null && !line.isBlank()) {
            final var headerParts = line.split(": ", 2);
            if (headerParts.length == 2) {
                headers.put(headerParts[0], headerParts[1]);
            }
        }

        InputStream body = new ByteArrayInputStream(new byte[0]);
        if (headers.containsKey("Content-Length")) {
            int contentLength = Integer.parseInt(headers.get("Content-Length"));
            char[] bodyChars = new char[contentLength];
            in.read(bodyChars, 0, contentLength);
            body = new ByteArrayInputStream(new String(bodyChars).getBytes());
        }

        return new Request(method, path, headers, body);
    }

    private String[] processRequest(BufferedReader in) throws IOException {
        return in.readLine().split(" ");
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
}

