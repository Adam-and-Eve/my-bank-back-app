package ru.yandex.practicum.bank.frontui.viewmodels;

/**
 * <summary>
 * Модель ответа с информацией о получателе перевода.
 * Содержит логин и имя пользователя, доступного для выполнения перевода.
 * </summary>
 **/
public record RecipientResponseViewModel (
        String login,
        String name
) {
}