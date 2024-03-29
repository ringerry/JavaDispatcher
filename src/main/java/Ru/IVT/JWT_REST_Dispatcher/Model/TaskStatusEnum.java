package Ru.IVT.JWT_REST_Dispatcher.Model;

// Состояния задачи для пользователя
public enum TaskStatusEnum {
    ОЖИДАНИЕ_ИСХОДНИКОВ,
    ОЖИДАНИЕ_ДАННЫХ,
    ОЖИДАНИЕ_ЗАПУСКА,

    // Задача запущена пользователем, сервер её ещё не обработал
    В_ОЧЕРЕДИ,
    // Задача может выполнятся, может ожидать кванта времени
    ВЫПОЛНЕНИЕ,
//    // Задача выполняется, но сейчас отдан приоритет выполнения другой задаче
//    ВЫПОЛНЕНИЕ_ПРИОСТАНОВЛЕНО,
    // Успешное естественное завершение задачи, имеется выходной файл а также исходные
    ЗАВЕРШЕНА,
    // Задача удалена пользователем и все соответвующие файлы
    УДАЛЕНА,



    ОШИБКА_ВЫПОЛНЕНИЯ,
    ОШИБКА_КОМПИЛЯЦИИ

}
