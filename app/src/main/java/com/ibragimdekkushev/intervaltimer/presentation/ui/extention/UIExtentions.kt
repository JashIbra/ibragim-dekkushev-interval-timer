package com.ibragimdekkushev.intervaltimer.presentation.ui.extention

fun Throwable.toReadableMessage(): String = when (this) {
    is java.net.UnknownHostException -> "Нет подключения к интернету"
    is retrofit2.HttpException -> when (code()) {
        404 -> "Тренировка не найдена. Проверьте ID."
        else -> "Ошибка сервера (${code()})"
    }

    else -> "Что-то пошло не так. Попробуйте снова."
}
