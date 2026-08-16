package ru.yandex.practicum.bank.shared.models;

/**
 * <summary>
 * Перечисление возможных состояний паттерна Circuit Breaker (Предохранитель).
 * </summary>
 **/
public enum StateModel {
    /**
     * <summary>
     * Цепь замкнута. Запросы выполняются в обычном режиме.
     * </summary>
     **/
    CLOSED,

    /**
     * <summary>
     * Цепь разомкнута. Запросы блокируются и сразу перенаправляются в fallback.
     * </summary>
     **/
    OPEN,

    /**
     * <summary>
     * Полуоткрытое (пробное) состояние. Выполняется пробный запрос для проверки восстановления целевой системы.
     * </summary>
     **/
    HALF_OPEN
}