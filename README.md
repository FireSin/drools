# Drools 10.0.0 + Spring Boot 3 + Java 17 - Полное руководство

## Обзор проекта

Этот проект демонстрирует различные подходы к интеграции Drools 10.0.0 с Spring Boot 3.5.0 и Java 17. Рассматриваются статическая и динамическая загрузка правил, использование KieHelper, и лучшие практики. В папке postman есть json для импорта в Postman. Так же в проекте подключен Swagger.В

## Архитектура решения

### Основные компоненты

1. **KieServices** - главная точка входа в KIE API, Factory для создания других компонентов
2. **KieFileSystem** - виртуальная файловая система для хранения ресурсов правил
3. **KieBuilder** - компилятор правил в байт-код
4. **KieContainer** - контейнер со скомпилированными правилами
5. **KieSession** - рабочая сессия для выполнения правил

### Структура проекта

```
src/main/
├── java/com/example/
│   ├── DroolsApplication.java          # Главный класс Spring Boot
│   ├── controller/DroolsController.java # REST API
│   ├── service/DroolsService.java      # Бизнес-логика Drools
│   └── model/Person.java               # Модель данных
└── resources/
    ├── rules/                          # Папка с правилами
    │   ├── discount-rules.drl          # Основные правила скидок
    │   └── validation-rules.drl        # Правила валидации
    └── application.yml                 # Конфигурация
```

## Три подхода к работе с правилами

### 1. Статическая загрузка (Рекомендуется для Production)

**Принцип**: Правила загружаются и компилируются один раз при старте приложения.

**Преимущества**:
- Максимальная производительность
- Ошибки компиляции обнаруживаются на старте
- Подходит для стабильных правил

**Недостатки**:
- Нельзя изменить правила без перезапуска
- Больше памяти занимает при старте

**Когда использовать**: В production среде со стабильными бизнес-правилами.

```java
// Инициализация один раз
KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
kieFileSystem.write(ResourceFactory.newClassPathResource("rules/discount-rules.drl"));
KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
kieBuilder.buildAll();
this.staticKieContainer = kieServices.newKieContainer(
    kieServices.getRepository().getDefaultReleaseId()
);

// Использование
KieSession kieSession = staticKieContainer.newKieSession();
```

### 2. Динамическая загрузка

**Принцип**: Правила загружаются и компилируются для каждого запроса или периодически.

**Преимущества**:
- Можно изменять правила на лету
- Подходит для A/B тестирования
- Гибкость в управлении правилами

**Недостатки**:
- Низкая производительность из-за компиляции
- Сложнее отлаживать ошибки

**Когда использовать**: Для экспериментов, тестирования новых правил, когда правила часто меняются.

```java
// Каждый раз создается новый контейнер
KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
kieFileSystem.write("src/main/resources/dynamic-rules.drl", rulesContent);
KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
kieBuilder.buildAll();
KieContainer kieContainer = kieServices.newKieContainer(
    kieServices.getRepository().getDefaultReleaseId()
);
```

### 3. KieHelper (Упрощенный подход)

**Принцип**: Использует упрощенный API для работы с правилами.

**Преимущества**:
- Простота использования
- Меньше кода
- Подходит для простых сценариев

**Недостатки**:
- Менее гибкий
- Меньше контроля над процессом

**Когда использовать**: Для прототипирования, простых случаев, когда не нужна сложная логика.

```java
KieHelper kieHelper = new KieHelper();
kieHelper.addResource(ResourceFactory.newClassPathResource("rules/discount-rules.drl"), 
                     ResourceType.DRL);
KieContainer kieContainer = kieHelper.getKieContainer();
```

## Рекомендации по выбору подхода

### Для Production (Высоконагруженные системы)
- **Используйте статическую загрузку**
- Правила тестируются заранее
- Максимальная производительность
- Стабильность работы

### Для систем с частым изменением правил
- **Комбинируйте подходы**
- Основные правила - статически
- Экспериментальные - динамически
- Кэширование скомпилированных правил

### Для разработки и тестирования
- **KieHelper или динамическая загрузка**
- Быстрое прототипирование
- Легкость отладки

## Управление ресурсами

### Важные моменты:

1. **Всегда закрывайте KieSession**: `kieSession.destroy()`
2. **Используйте try-with-resources** для автоматического управления
3. **KieContainer** можно переиспользовать, **KieSession** - нет
4. **Не создавайте KieSession в статических блоках**

```java
// Правильно
KieSession kieSession = kieContainer.newKieSession();
try {
    // работа с сессией
} finally {
    kieSession.destroy(); // Обязательно!
}
```
или 
```java
// Правильно
try(KieSession kieSession = kieContainer.newKieSession()) {
    // работа с сессией
}
```

## Оптимизация производительности

### 1. Пул сессий для высоконагруженных систем

```java
@Component
public class KieSessionPool {
    private final Queue<KieSession> pool = new ConcurrentLinkedQueue<>();
    private final KieContainer kieContainer;
    
    public KieSession borrowSession() {
        KieSession session = pool.poll();
        return session != null ? session : kieContainer.newKieSession();
    }
    
    public void returnSession(KieSession session) {
        // Очистить рабочую память
        session.getFactHandles().forEach(session::delete);
        pool.offer(session);
    }
}
```

### 2. Кэширование скомпилированных правил

```java
@Component
public class CompiledRulesCache {
    private final Map<String, KieContainer> cache = new ConcurrentHashMap<>();
    
    public KieContainer getOrCompile(String rulesKey, String rulesContent) {
        return cache.computeIfAbsent(rulesKey, key -> compileRules(rulesContent));
    }
}
```

## Обработка ошибок

### Типичные ошибки и их решения:

1. **Ошибки компиляции правил**
```java
if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
    kieBuilder.getResults().getMessages().forEach(message -> {
        log.error("Drools compilation error: {}", message);
    });
    throw new RuntimeException("Failed to compile rules");
}
```

2. **Проблемы с ClassPath**
- Убедитесь, что файлы .drl находятся в `src/main/resources`
- Проверьте правильность путей в коде

3. **Утечки памяти**
- Всегда вызывайте `kieSession.destroy()`
- Не храните ссылки на KieSession дольше необходимого

## Запуск и тестирование

### 1. Сборка проекта
```bash
mvn clean compile
mvn spring-boot:run
```

### 2. Тестирование API
```bash
# Запуск всех тестов
chmod +x test-requests.sh
./test-requests.sh

# Или отдельные запросы
curl -X GET "http://localhost:8080/api/drools/test/25"
```

### 3. Примеры ответов

**Ребенок (10 лет)**:
```json
{
  "name": "Alice",
  "age": 10,
  "category": "CHILD",
  "discount": 0.20,
  "eligible": true
}
```

**Пенсионер (70 лет)**:
```json
{
  "name": "Bob",
  "age": 70,
  "category": "SENIOR",
  "discount": 0.25,
  "eligible": true
}
```

## Мониторинг и метрики

### Рекомендуемые метрики для мониторинга:

1. **Время выполнения правил**
2. **Количество срабатываний правил**
3. **Ошибки компиляции**
4. **Использование памяти KieSession**

```java
@Component
public class DroolsMetrics {
    private final MeterRegistry meterRegistry;
    
    public void recordRuleExecution(String ruleName, Duration duration) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("drools.rule.execution")
                   .tag("rule", ruleName)
                   .register(meterRegistry));
    }
}
```

## Заключение

Этот пример демонстрирует три основных подхода к работе с Drools:

1. **Статическая загрузка** - для production систем
2. **Динамическая загрузка** - для гибких систем
3. **KieHelper** - для простых случаев

Выбор подхода зависит от ваших требований к производительности, гибкости и сложности системы. В большинстве случаев рекомендуется начать со статической загрузки и добавлять динамические возможности по мере необходимости.