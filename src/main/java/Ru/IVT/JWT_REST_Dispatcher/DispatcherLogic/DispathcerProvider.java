package Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
public interface DispathcerProvider {
    boolean isDispatcherComputing();
    void run(Long taskId);
}
