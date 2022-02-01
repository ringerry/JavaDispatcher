package Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic.Impl;

import Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic.DispathcerProvider;

public class DispathcerProviderImpl implements DispathcerProvider {

    @Override
    public boolean isDispatcherComputing() {
        return false;
    }

    @Override
    public void run(Long taskId) {

    }
}
