package Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Tes {

    private static final String BASE_URL = "http://localhost:8000/";

    public static void main(String[] args) throws IOException, InterruptedException {
        testPostShortLink();
        testGetLongLink();
        testGetNonExistentLink();
        testLimitExceeded();
        testExpiredLinkDeletion();
    }

    private static void testPostShortLink() throws IOException {
        String longUrl = "https://promo-z.ru/";
        String jsonInputString = "{\"longUrl\":\"" + longUrl + "\"}";

        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + "shorten").openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonInputString.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        System.out.println("POST Response Code: " + responseCode);
    }

    private static void testGetLongLink() throws IOException {
        String shortUrl = "/abc123"; // Замените на реальный короткий URL
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + shortUrl).openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        System.out.println("GET Response Code: " + responseCode);

        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            System.out.println("Long URL: " + content.toString());
        }
    }

    private static void testGetNonExistentLink() throws IOException {
        String shortUrl = "/nonexistent"; // Замените на несуществующий короткий URL
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + shortUrl).openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        System.out.println("GET Non-existent Response Code: " + responseCode);
    }

    private static void testLimitExceeded() throws IOException, InterruptedException {
        String longUrl = "https://promo-z.ru/";
        String jsonInputString = "{\"longUrl\":\"" + longUrl + "\"}";

        // Создаем короткую ссылку
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + "shorten").openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonInputString.getBytes());
            os.flush();
        }

        // Получаем короткую ссылку из ответа
        String shortUrl = "/abc123"; // Замените на реальный короткий URL

        // Переходим по ссылке 5 раз
        for (int i = 0; i < 5; i++) {
            connection = (HttpURLConnection) new URL(BASE_URL + shortUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.getResponseCode(); // Просто выполняем запрос
        }

        // Пытаемся перейти по ссылке в шестой раз
        connection = (HttpURLConnection) new URL(BASE_URL + shortUrl).openConnection();
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();
        System.out.println("GET Response Code after limit exceeded: " + responseCode);
    }

    private static void testExpiredLinkDeletion() throws IOException, InterruptedException {
        String longUrl = "https://promo-z.ru/";
        String jsonInputString = "{\"longUrl\":\"" + longUrl + "\"}";

        // Создаем короткую ссылку
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + "shorten").openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonInputString.getBytes());
            os.flush();
        }

        // Получаем короткую ссылку из ответа
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // Предположим, что короткая ссылка возвращается в формате JSON и содержит поле "shortUrl"
        String shortUrl = response.toString(); // Замените на способ извлечения короткой ссылки из ответа

        // Ждем, пока ссылка истечет (например, 2 секунды)
        Thread.sleep(2000);

        // Пытаемся перейти по истекшей ссылке
        connection = (HttpURLConnection) new URL(BASE_URL + shortUrl).openConnection();
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();
        System.out.println("GET Response Code after expiration: " + responseCode);

        // Проверяем, что ссылка была удалена (ожидаем, что ответ будет 404 или другой код ошибки)
        if (responseCode == 404) {
            System.out.println("Ссылка успешно удалена после истечения срока действия.");
        } else {
            System.out.println("Ссылка все еще доступна, код ответа: " + responseCode);
        }
    }
}
