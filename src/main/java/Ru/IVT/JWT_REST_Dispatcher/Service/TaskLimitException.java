package Ru.IVT.JWT_REST_Dispatcher.Service;

public class TaskLimitException extends Exception {
    public TaskLimitException(String msg) {
        super(msg);
    }
}
