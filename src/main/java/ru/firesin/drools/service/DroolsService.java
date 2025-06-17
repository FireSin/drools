package ru.firesin.drools.service;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.utils.KieHelper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import ru.firesin.drools.model.Person;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Kubantsev Artur
 * @since 17.06.2025
 */

/**
 * Сервис для работы с Drools правилами
 * Демонстрирует различные подходы: статическая загрузка, динамическая загрузка, KieHelper
 */
@Service
public class DroolsService {

    private final KieServices kieServices;
    private KieContainer staticKieContainer; // Для статической загрузки

    public DroolsService() {
        this.kieServices = KieServices.Factory.get();
        initializeStaticRules();
    }

    /**
     * 1. СТАТИЧЕСКАЯ ЗАГРУЗКА ПРАВИЛ
     * Правила загружаются один раз при инициализации сервиса
     * Преимущества: быстрая работа, правила компилируются заранее
     * Недостатки: нельзя изменить правила без перезапуска приложения
     */
    private void initializeStaticRules() {
        try {
            // KieFileSystem - это виртуальная файловая система для Drools
            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

            // Загружаем правила из classpath
            kieFileSystem.write(ResourceFactory.newClassPathResource("rules/discount-rules.drl"));
            kieFileSystem.write(ResourceFactory.newClassPathResource("rules/validation-rules.drl"));

            // KieBuilder компилирует правила
            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
            kieBuilder.buildAll();

            // Проверяем ошибки компиляции
            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                System.err.println("Ошибки при компиляции правил:");
                kieBuilder.getResults().getMessages().forEach(System.err::println);
                throw new RuntimeException("Не удалось скомпилировать правила");
            }

            // KieContainer содержит скомпилированные правила
            this.staticKieContainer = kieServices.newKieContainer(
                    kieServices.getRepository().getDefaultReleaseId()
            );

            System.out.println("Статические правила успешно загружены");

        } catch (Exception e) {
            System.err.println("Ошибка инициализации статических правил: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Обработка с использованием статически загруженных правил
     */
    public Person processWithStaticRules(Person person) {
        // KieSession - это рабочая сессия для выполнения правил
        // Создаем новую сессию для каждого запроса (stateless подход)
        try (KieSession kieSession = staticKieContainer.newKieSession()) {
            // Вставляем факт в рабочую память
            kieSession.insert(person);

            // Запускаем выполнение всех подходящих правил
            int firedRules = kieSession.fireAllRules();
            System.out.println("Выполнено правил: " + firedRules);

            return person;
        }
    }

    /**
     * 2. ДИНАМИЧЕСКАЯ ЗАГРУЗКА ПРАВИЛ
     * Правила загружаются и компилируются для каждого запроса
     * Преимущества: можно изменять правила динамически
     * Недостатки: медленнее из-за компиляции при каждом вызове
     */
    public Person processWithDynamicRules(Person person, String rulesContent) {
        try {
            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

            // Добавляем правила из строки (можно получить из БД, файла и т.д.)
            kieFileSystem.write("src/main/resources/dynamic-rules.drl", rulesContent);

            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
            kieBuilder.buildAll();

            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                throw new RuntimeException("Ошибки в динамических правилах: " +
                        kieBuilder.getResults().getMessages());
            }

            KieContainer kieContainer = kieServices.newKieContainer(
                    kieServices.getRepository().getDefaultReleaseId()
            );

            try (KieSession kieSession = kieContainer.newKieSession()) {
                kieSession.insert(person);
                kieSession.fireAllRules();
                return person;
            }
        } catch (Exception e) {
            System.err.println("Ошибка при динамической загрузке правил: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 3. ИСПОЛЬЗОВАНИЕ KieHelper
     * KieHelper - упрощенный API для работы с правилами
     * Преимущества: более простой код, удобно для простых случаев
     * Недостатки: менее гибкий, чем полный KIE API
     */
    public Person processWithKieHelper(Person person) {
        try {
            // KieHelper упрощает создание KieContainer
            KieHelper kieHelper = new KieHelper();

            // Добавляем ресурсы через KieHelper
            kieHelper.addResource(ResourceFactory.newClassPathResource("rules/discount-rules.drl"),
                    ResourceType.DRL);
            kieHelper.addResource(ResourceFactory.newClassPathResource("rules/validation-rules.drl"),
                    ResourceType.DRL);

            // Получаем KieContainer
            KieContainer kieContainer = kieHelper.getKieContainer();

            try (KieSession kieSession = kieContainer.newKieSession()) {
                kieSession.insert(person);
                kieSession.fireAllRules();
                return person;
            }
        } catch (Exception e) {
            System.err.println("Ошибка при использовании KieHelper: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Загрузка правил из файловой системы для демонстрации динамической загрузки
     */
    public String loadRulesFromFile(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource("rules/" + fileName);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Ошибка загрузки файла правил: " + fileName);
            throw new RuntimeException(e);
        }
    }

    /**
     * Обработка списка объектов
     */
    public List<Person> processMultiple(List<Person> persons) {
        try (KieSession kieSession = staticKieContainer.newKieSession()) {
            // Вставляем все объекты в рабочую память
            persons.forEach(kieSession::insert);

            // Выполняем правила для всех объектов
            int firedRules = kieSession.fireAllRules();
            System.out.println("Обработано объектов: " + persons.size() +
                    ", выполнено правил: " + firedRules);

            return persons;
        }
    }
}
