package Main;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;



public class Main {

    // Модель
    static class UrlLink {
        private String longUrl;
        private String shortUrl;
        private int visitLimit;
        private int visitCount;
        private long expirationTime;
        private UUID userId;

        public UrlLink(String longUrl, String shortUrl, int visitLimit, UUID userId) {
            this.longUrl = longUrl;
            this.shortUrl = shortUrl;
            this.visitLimit = visitLimit;
            this.visitCount = 0;
            this.expirationTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1); // 1 день
            this.userId = userId;
        }

        
        public String getLongUrl() {
            return longUrl;
        }

        public String getShortUrl() {
            return shortUrl;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }

        public boolean canBeVisited() {
            return visitCount < visitLimit && !isExpired();
        }

        public void incrementVisitCount() {
            visitCount++;
        }

        public UUID getUsI() { // Переименованный метод
            return userId;
        }
    }

    // Репозиторий
    static class LinkRepository {
        private final Map<String, UrlLink> links = new ConcurrentHashMap<>();

        public String save(String longUrl, UUID userId) {
            String shortUrl = "https://promo-z.ru/" + UUID.randomUUID().toString(); // уникальный короткий URL
            links.put(shortUrl, new UrlLink(longUrl, shortUrl, 5, userId)); // лимит переходов 5
            return shortUrl;
        }

        public UrlLink getUrlLink(String shortUrl) {
            return links.get(shortUrl);
        }

        public void cleanExpiredLinks() {
            links.values().removeIf(UrlLink::isExpired);
        }
    }

    // Контроллер
    static class LinkController implements HttpHandler {
        private final LinkRepository linkRepository = new LinkRepository();


        public void handle(HttpExchange exchange) throws IOException {
            String response;
            int statusCode;

            try {
                if ("POST".equals(exchange.getRequestMethod())) {
                    // Чтение содержимого InputStream
                    InputStream inputStream = exchange.getRequestBody();
                    String requestBody = new BufferedReader(new InputStreamReader(inputStream))
                            .lines().collect(Collectors.joining("\n"));

                    String longUrl = extractLongUrl(requestBody);
                    if (longUrl == null) { // Проверка на null
                        response = "{\"error\": true, \"message\": \"longUrl не найден\"}";
                        statusCode = 400; // Bad Request
                    } else {
                        UUID userId = UUID.randomUUID(); // Генерация UUID для пользователя
                        String shortUrl = linkRepository.save(longUrl, userId);
                        response = "{\"shortUrl\":\"" + shortUrl + "\"}";
                        statusCode = 200;
                    }
                } else if ("GET".equals(exchange.getRequestMethod())) {
                    String shortUrl = "https://promo-z.ru" + exchange.getRequestURI().getPath();
                    UrlLink link = linkRepository.getUrlLink(shortUrl);
                    if (link != null) {
                        if (link.canBeVisited()) {
                            link.incrementVisitCount();
                            response = link.getLongUrl();
                            statusCode = 302; // Перенаправление
                            exchange.getResponseHeaders().set("Location", link.getLongUrl());
                        } else {
                            response = "Ссылка недоступна.";
                            statusCode = 404;
                        }
                    } else {
                        response = "Не найдено";
                        statusCode = 404;
                    }
                } else {
                    response = "Неподдерживаемый метод";
                    statusCode = 405; // Method Not Allowed
                }
            } catch (Exception e) {
                response = "Ошибка обработки запроса: " + e.getMessage();
                statusCode = 500; // Internal Server Error
            }

            exchange.sendResponseHeaders(statusCode, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }


        private String extractLongUrl(String requestBody) {
            // Простейшая обработка JSON, извлечение longUrl
            String longUrlKey = "\"longUrl\":\"";
            int startIndex = requestBody.indexOf(longUrlKey);
            if (startIndex == -1) {
                return null; // Если ключ не найден, возвращаем null
            }
            startIndex += longUrlKey.length();
            int endIndex = requestBody.indexOf("\"", startIndex);
            if (endIndex == -1) {
                return null; // Если закрывающая кавычка не найдена, возвращаем null
            }
            return requestBody.substring(startIndex, endIndex);
        }
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/", new LinkController());
        server.setExecutor(null); // создает стандартный исполнитель
        server.start();
        System.out.println("Сервер запущен на порту 8000");

        // Запуск фонового потока для очистки истекших ссылок
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            // Используем ссылку на существующий репозиторий
            // Важно: создаем новый объект LinkController, чтобы получить доступ к репозиторию
            // Это может быть улучшено, если сделать репозиторий статическим или передать его в контроллер
            LinkController linkController = new LinkController();
            linkController.linkRepository.cleanExpiredLinks();
            System.out.println("Очистка истекших ссылок выполнена.");
        }, 1, 1, TimeUnit.HOURS); // Очистка раз в час
    }
}