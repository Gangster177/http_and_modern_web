package homework;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final ExecutorService es;
    private static Socket socket;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> handlers;
    public static final String GET = "GET";
    public static final String POST = "POST";
    final List<String> allowedMethods = List.of(GET, POST);

    public Server(int poolSize) {
        this.es = Executors.newFixedThreadPool(poolSize);
        this.handlers = new ConcurrentHashMap<>();
    }

    public void start(int port) {
        try (final ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                socket = serverSocket.accept();
                es.submit(() -> newConnect(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            es.shutdown();
        }
    }

    private void newConnect(Socket socket) {
        try (
                final var in = new BufferedInputStream(socket.getInputStream());
                final var out = new BufferedOutputStream(socket.getOutputStream());) {
            // лимит на request line + заголовки
            final var limit = 4096;
            in.mark(limit); // устанавливаем максимальную отметку
            final var buffer = new byte[limit];
            final var read = in.read(buffer);

            // ищем request line
            final var requestLineDelimiter = new byte[]{'\r', '\n'};
            final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            if (requestLineEnd == -1) {
                badRequest(out);
                socket.close();
                return;
            }

            // читаем request line
            final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
            if (requestLine.length != 3) {
                badRequest(out);
                socket.close();
                return;
            }

            final var method = requestLine[0];
            if (!allowedMethods.contains(method)) {
                badRequest(out);
                socket.close();
                return;
            }
//TODO
            System.out.println("@method => " + method);

            final var path = requestLine[1];
            if (!path.startsWith("/")) {
                badRequest(out);
                return;
            }
//TODO
            System.out.println("@path  => " + path);

            // ищем заголовки
            final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
            final var headersStart = requestLineEnd + requestLineDelimiter.length;
            final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
            if (headersEnd == -1) {
                badRequest(out);
                socket.close();
                return;
            }

            // отматываем на начало буфера
            in.reset();
            // пропускаем requestLine
            in.skip(headersStart);
            //тк мы не знаем, где начинает заголовок и где заканчивается то мы можем прочитать заголовок
            final var headersBytes = in.readNBytes(headersEnd - headersStart);
            //парсим по переносу строки
            final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
//TODO
            System.out.println("@headers => " + headers);

            // для GET тела нет
            List<NameValuePair> paramsBody = new ArrayList<>();
            if (!method.equals(GET)) {
                String body;
                in.skip(headersDelimiter.length);
                // вычитываем Content-Length, чтобы прочитать body
                final var contentLength = extractHeader(headers, "Content-Length");
                if (contentLength.isPresent()) {
                    final var length = Integer.parseInt(contentLength.get());//получаем значение,парсим значение в int
                    final var bodyBytes = in.readNBytes(length);//читаем нужное количество байт
                    body = new String(bodyBytes);//из этого массива байт мы создаем строку
                    paramsBody = URLEncodedUtils.parse(body, StandardCharsets.UTF_8);
//TODO
                    System.out.println("@body => " + body);
                }
            }
            List<NameValuePair> paramsQuery = URLEncodedUtils.parse(URI.create(requestLine[1]), StandardCharsets.UTF_8);

            Request request = new Request(method, path, paramsQuery, paramsBody);
            System.out.println("@request");
            Map<String, Handler> handlerMap = handlers.get(request.getMethod());
            String requestPath = request.getPath().split("\\?")[0];
            if (handlerMap != null && handlerMap.containsKey(requestPath)) {
                Handler handler = handlerMap.get(requestPath);
                handler.handle(request, out);
                System.out.println("@default handlers started");
            } else {
                notFound(out);
            }
            System.out.println("@requestPath => " + requestPath);
            System.out.println(request.printQueryParams());
            System.out.println(request.getQueryParam("value"));
            System.out.println(request.getQueryParam("title"));

            System.out.println("@socket.close()");
            socket.close();
        } catch (
                IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void notFound(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public void addHandler(String method, String path, Handler handler) {
        if (!handlers.containsKey(method)) {
            handlers.put(method, new ConcurrentHashMap<>());
        }
        handlers.get(method).put(path, handler);
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }
}