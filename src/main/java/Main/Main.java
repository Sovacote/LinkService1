package Main;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main{

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

        public UUID getUserId() {
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
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response;
            int statusCode;

            try {
                if ("POST".equals(exchange.getRequestMethod())) {
                    String requestBody = new String(exchange.getRequestBody().readAllBytes());
                    String longUrl = objectMapper.readTree(requestBody).get("longUrl").asText();
                    UUID userId = UUID.randomUUID(); // Генерация UUID для пользователя
                    String shortUrl = linkRepository.save(longUrl, userId);
                    response = objectMapper.writeValueAsString(Map.of("shortUrl", shortUrl));
                    statusCode = 200;
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
            LinkController linkController = new LinkController();
            linkController.linkRepository.cleanExpiredLinks();
            System.out.println("Очистка истекших ссылок выполнена.");
        }, 1, 1, TimeUnit.HOURS); // Очистка раз в час
    }
}