package ru.firesin.drools.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Kubantsev Artur
 * @since 17.06.2025
 *
 * Простая модель Person для демонстрации работы с Drools.
 * Используется как факт в рабочей памяти (Working Memory)
 */
@Setter
@Getter
@NoArgsConstructor
public class Person {

    private String name;
    private int age;
    private String category;
    private double discount;
    private boolean eligible;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
        this.eligible = false;
        this.discount = 0.0;
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", category='" + category + '\'' +
                ", discount=" + discount +
                ", eligible=" + eligible +
                '}';
    }
}
