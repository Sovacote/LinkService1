package Main;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

// Класс для загрузки конфигураций
class ConfigLoader {
    private Properties properties;

    public ConfigLoader(String filePath) throws IOException {
        properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream(filePath)) {
            properties.load(inputStream);
        }
    }

    public int getLinkLifetime() {
        return Integer.parseInt(properties.getProperty("link.lifetime", "3600")); // Значение по умолчанию 3600 секунд
    }

    public int getLinkClickLimit() {
        return Integer.parseInt(properties.getProperty("link.click.limit", "10")); // Значение по умолчанию 10 переходов
    }
}

// Класс для представления ссылки
class UrlLink {
    private final String uuid;
    private final String originalUrl;
    private final String userId;
    private final int lifetime;
    private int clickLimit;
    private int clickCount;
    private final long creationTime;

    public UrlLink(String originalUrl, String userId, int lifetime, int clickLimit) {
        this.uuid = UUID.randomUUID().toString();
        this.originalUrl = originalUrl;
        this.userId = userId;
        this.lifetime = lifetime;
        this.clickLimit = clickLimit;
        this.clickCount = 0;
        this.creationTime = System.currentTimeMillis();
    }

    public String getUuid() {
        return uuid;
    }

    public String getUserId() { // Исправлено имя метода
        return userId;
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - creationTime) > (lifetime * 1000);
    }

    public boolean canClick() {
        return clickCount < clickLimit && !isExpired();
    }

    public void incrementClick() {
        if (canClick()) {
            clickCount++;
        }
    }

    public void setClickLimit(int limit) {
        this.clickLimit = limit;
    }

    public int getClickCount() {
        return clickCount;
    }

    public String getOriginalUrl() {
        return originalUrl; // Добавлен метод для получения оригинального URL
    }
}

// Класс для управления ссылками
class LinkRepository {
    private final Map<String, UrlLink> links = new HashMap<>();

    public void addLink(UrlLink link) {
        links.put(link.getUuid(), link);
    }

    public boolean deleteLink(String uuid, String userId) {
        UrlLink link = links.get(uuid);
        if (link != null && link.getUserId().equals(userId)) { // Исправлено имя метода
            links.remove(uuid);
            return true;
        }
        notifyUser ("Вы не имеете прав для удаления этой ссылки.");
        return false;
    }

    public boolean editLink(String uuid, String userId, int newClickLimit) {
        UrlLink link = links.get(uuid);
        if (link != null && link.getUserId().equals(userId)) { // Исправлено имя метода
            link.setClickLimit(newClickLimit);
            return true;
        }
        notifyUser ("Вы не имеете прав для редактирования этой ссылки.");
        return false;
    }

    public void notifyUser (String message) {
        // Здесь можно реализовать логику уведомления пользователя
        System.out.println("Уведомление: " + message);
    }

    public UrlLink getLink(String uuid) {
        return links.get(uuid);
    }
}

// Основной класс
public class Main {
    public static void main(String[] args) {
        try {
            ConfigLoader configLoader = new ConfigLoader("config.properties");
            int linkLifetime = configLoader.getLinkLifetime();
            int linkClickLimit = configLoader.getLinkClickLimit();

            // Инициализация userId
            String userId = "user123"; // Пример значения userId

            // Пример использования
            LinkRepository linkRepository = new LinkRepository();

            // Создание ссылки
            UrlLink newLink = new UrlLink("http://example.com", userId, linkLifetime, linkClickLimit);
            linkRepository.addLink(newLink);
            System.out.println("Ссылка создана: " + newLink.getUuid());

            // Проверка, можно ли кликнуть по ссылке
            UrlLink retrievedLink = linkRepository.getLink(newLink.getUuid());
            if (retrievedLink != null && retrievedLink.canClick()) {
                retrievedLink.incrementClick();
                System.out.println("Клик по ссылке: " + retrievedLink.getOriginalUrl());
            } else {
                System.out.println("Ссылка недоступна для клика или истекла.");
            }

            // Повторное редактирование лимита переходов
            linkRepository.editLink(newLink.getUuid(), userId, 3);
            System.out.println("Лимит переходов изменен на: " + 3);

            // Проверка количества кликов
            System.out.println("Количество кликов по ссылке: " + retrievedLink.getClickCount());

            // Удаление ссылки
            if (linkRepository.deleteLink(newLink.getUuid(), userId)) {
                System.out.println("Ссылка успешно удалена.");
            } else {
                System.out.println("Не удалось удалить ссылку.");
            }

        } catch (IOException e) {
            System.err.println("Ошибка загрузки конфигурации: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Произошла ошибка: " + e.getMessage());
        }
    }
}