package Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic;


import java.util.Timer;

public interface DispathcerEnginge {

    Timer getMyTimer();

    void init();

    String getConsoleOutput(Long taskId) throws Exception;

    boolean delTask(Long UserId, Long taskId) throws Exception;

    String getTaskOutputPack(Long taskId) throws Exception;
}
