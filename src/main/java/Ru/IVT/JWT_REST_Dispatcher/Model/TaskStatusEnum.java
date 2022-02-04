package Ru.IVT.JWT_REST_Dispatcher.Model;

public enum TaskStatusEnum {
    В_ОЧЕРЕДИ, ОЖИДАНИЕ_ИСХОДНИКОВ, ОЖИДАНИЕ_ДАННЫХ, ОЖИДАНИЕ_ЗАПУСКА,
    ВЫПОЛНЕНИЕ, ОШИБКА_ВЫПОЛНЕНИЯ, ОШИБКА_КОМПИЛЯЦИИ, ЗАВЕРШЕНА
}
