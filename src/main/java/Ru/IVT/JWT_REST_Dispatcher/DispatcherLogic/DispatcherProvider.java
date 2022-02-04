package Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic;

import java.util.Timer;
import Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic.Impl.DispatcherEngineImpl;

public class DispatcherProvider {

    private DispatcherEngineImpl dispatcher;

    public DispatcherProvider(){
        this.dispatcher = new DispatcherEngineImpl() ;

    }

    public DispatcherEngineImpl getDispatcher() {
        return this.dispatcher;
    }
}
