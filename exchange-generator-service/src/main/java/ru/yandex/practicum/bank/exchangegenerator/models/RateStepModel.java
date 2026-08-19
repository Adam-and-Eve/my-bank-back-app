package ru.yandex.practicum.bank.exchangegenerator.models;

/**
 * <summary>
 * Модель шага изменения курса обмена валюты.
 * Содержит значения курса покупки и курса продажи.
 * </summary>
 * @param buyRate Курс покупки валюты.
 * @param sellRate Курс продажи валюты.
 */
public record RateStepModel (
        String buyRate,
        String sellRate
) {
}