package Ru.IVT.JWT_REST_Dispatcher.Model;

public enum InsideTaskStatusEnum {

    // Нет исходников, данных, или не запущена
    НЕ_ОПРЕДЕЛЕНО,


    СОЗДАН_ОБРАЗ,
    ВЫПОЛНЕНИЕ,
    ПРИОСТАНОВЛЕНА,
    ЗАВЕРШЕНА,

    ОШИБКА_КОМПЛИЛЯЦИИ,
    ОШИБКА_ВЫПОЛНЕНИЯ


}
