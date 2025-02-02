Сервис для сокращения URL.

## Установка
1. Убедитесь, что у вас установлена Java 8 или выше.
2. Склонируйте репозиторий:
   ```bash
   git clone <repository-url>
Перейдите в каталог проекта:
cd <project-directory>
Запустите сервер:
java -cp . Main

Использование
Создание короткой ссылки: Отправьте POST-запрос на http://localhost:8000/ с JSON-телом.
Переход по короткой ссылке: Отправьте GET-запрос на https://promo-z.ru/unique-id

Как пользоваться сервисом
Запуск сервера:

Запустите главный класс Main. Сервер начнет работать на порту 8000.
Создание короткой ссылки:

Отправьте POST-запрос на http://localhost:8000/ с JSON-телом, содержащим длинный URL. Пример:
{
  "longUrl": "https://example.com/long-url"
}
В ответ вы получите JSON с короткой ссылкой:
{
  "shortUrl": "https://promo-z.ru/unique-id"
}
Переход по короткой ссылке:

Отправьте GET-запрос на https://promo-z.ru/unique-id. Если ссылка активна, вы будете перенаправлены на длинный URL.
Какие команды поддерживаются
POST / - Создание короткой ссылки.
GET /unique-id - Переход по короткой ссылке.
Как протестировать ваш код
Тестирование с помощью Postman или curl:

Для создания короткой ссылки:
curl -X POST http://localhost:8000/ -H "Content-Type: application/json" -d '{"longUrl":"https://example.com/long-url"}'
Для перехода по короткой ссылке:
curl -X GET https://promo-z.ru/unique-id
