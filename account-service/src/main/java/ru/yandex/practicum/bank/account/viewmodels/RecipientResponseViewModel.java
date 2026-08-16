package ru.yandex.practicum.bank.account.viewmodels;

/**
 * <summary>
 * Модель ответа с базовой информацией о получателе (RecipientResponseViewModel).
 * Используется для отображения списка доступных адресатов при переводах.
 * </summary>
 * @param login Логин получателя.
 * @param name Имя (или полное имя) получателя.
 **/
public record RecipientResponseViewModel (
        String login,
        String name
) {
}