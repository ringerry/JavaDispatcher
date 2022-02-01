package Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic;

public interface DispathcerProvider {
    boolean isDispatcherComputing();
    void run(Long taskId);
}
