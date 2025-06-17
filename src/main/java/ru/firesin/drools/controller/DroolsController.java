package ru.firesin.drools.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.firesin.drools.model.Person;
import ru.firesin.drools.service.DroolsService;

import java.util.List;
import java.util.Map;

/**
 * @author Kubantsev Artur
 * @since 17.06.2025
 *
 * REST контроллер для демонстрации различных подходов работы с Drools
 */
@RestController
@RequestMapping("/api/drools")
public class DroolsController {

    private final DroolsService droolsService;

    @Autowired
    public DroolsController(DroolsService droolsService) {
        this.droolsService = droolsService;
    }

    /**
     * Обработка с использованием статически загруженных правил
     * Самый быстрый подход для production
     */
    @PostMapping("/static")
    public ResponseEntity<Person> processWithStaticRules(@RequestBody Person person) {
        try {
            Person result = droolsService.processWithStaticRules(person);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Демонстрация пакетной обработки
     */
    @PostMapping("/static/batch")
    public ResponseEntity<List<Person>> processBatch(@RequestBody List<Person> persons) {
        try {
            List<Person> results = droolsService.processMultiple(persons);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Обработка с динамической загрузкой правил из тела запроса
     * Полезно для тестирования новых правил или A/B тестов
     */
    @PostMapping("/dynamic")
    public ResponseEntity<Person> processWithDynamicRules(
            @RequestBody Map<String, Object> request) {
        try {
            // Извлекаем данные из запроса
            Map<String, Object> personData = (Map<String, Object>) request.get("person");
            String rules = (String) request.get("rules");

            Person person = new Person();
            person.setName((String) personData.get("name"));
            person.setAge(((Number) personData.get("age")).intValue());

            Person result = droolsService.processWithDynamicRules(person, rules);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Обработка с использованием KieHelper
     */
    @PostMapping("/kiehelper")
    public ResponseEntity<Person> processWithKieHelper(@RequestBody Person person) {
        try {
            Person result = droolsService.processWithKieHelper(person);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Загрузка и применение правил из файла
     * Промежуточный подход между статическим и полностью динамическим
     */
    @PostMapping("/file/{fileName}")
    public ResponseEntity<Person> processWithFileRules(
            @PathVariable String fileName,
            @RequestBody Person person) {
        try {
            String rulesContent = droolsService.loadRulesFromFile(fileName);
            Person result = droolsService.processWithDynamicRules(person, rulesContent);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Получение информации о доступных правилах
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        return ResponseEntity.ok(Map.of(
                "approaches", List.of(
                        Map.of(
                                "name", "Static Rules",
                                "description", "Правила загружаются при старте приложения",
                                "endpoint", "/api/drools/static",
                                "performance", "Высокая",
                                "flexibility", "Низкая"
                        ),
                        Map.of(
                                "name", "Dynamic Rules",
                                "description", "Правила передаются в теле запроса",
                                "endpoint", "/api/drools/dynamic",
                                "performance", "Низкая",
                                "flexibility", "Высокая"
                        ),
                        Map.of(
                                "name", "KieHelper",
                                "description", "Упрощенный API для работы с правилами",
                                "endpoint", "/api/drools/kiehelper",
                                "performance", "Средняя",
                                "flexibility", "Средняя"
                        )
                ),
                "sample_person", Map.of(
                        "name", "John Doe",
                        "age", 25
                )
        ));
    }

    /**
     * Тестовые данные для быстрой проверки
     */
    @GetMapping("/test/{age}")
    public ResponseEntity<Person> testWithAge(@PathVariable int age) {
        Person testPerson = new Person("Test User", age);
        Person result = droolsService.processWithStaticRules(testPerson);
        return ResponseEntity.ok(result);
    }
}
