package Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic.Impl;


import Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic.DispathcerEnginge;
import lombok.extern.slf4j.Slf4j;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Диспетчеризация, алгоритмы приоритетов
 * @author Меньшиков Артём
 * */
/*
Все дейстия в task_in_run делает этот DispatcherEngine класс
 */

@Slf4j
public class DispatcherEngineImpl /*implements DispathcerEnginge*/ {

    private Long dispatcherPeriodMS; /*= 60000L;*/

    private final Timer myTimer/* = new Timer()*/; // Создаем таймер
//        final Handler uiHandler = new Handler();


    public DispatcherEngineImpl(){
        this.dispatcherPeriodMS = 1000L;
        this.myTimer = new Timer();
        startMainTimer();
    }



    private void startMainTimer() {
        myTimer.schedule(new TimerTask() { // Определяем задачу
            @Override
            public void run() {
               log.info("Квант диспетчера");
                dispatcherQuantum();
            };
        }, 0L, dispatcherPeriodMS);
    }

    private void scanDataBase(){

    }

    // Основная логика диспетчера
    private void dispatcherQuantum(){

    }

//    @Override
    public Timer getMyTimer() {
        return myTimer;
    }

//    @Override
//    public void init() {
//        startMainTimer();
//    }
}
