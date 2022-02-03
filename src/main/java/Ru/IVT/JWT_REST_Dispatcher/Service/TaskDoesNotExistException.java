package Ru.IVT.JWT_REST_Dispatcher.Service;

public class TaskDoesNotExistException extends Exception{
    public TaskDoesNotExistException(String msg) {
        super(msg);
    }
}
