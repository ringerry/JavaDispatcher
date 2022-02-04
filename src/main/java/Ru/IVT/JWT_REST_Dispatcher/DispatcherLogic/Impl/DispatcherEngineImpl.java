package Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic.Impl;


import Ru.IVT.JWT_REST_Dispatcher.Model.Task;
import Ru.IVT.JWT_REST_Dispatcher.Model.TaskStatusEnum;
import Ru.IVT.JWT_REST_Dispatcher.Repository.TaskRepositoryNT;
import Ru.IVT.JWT_REST_Dispatcher.Service.TaskService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
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

    private TaskRepositoryNT taskRepository;
    private TaskService taskService;

    private Long dispatcherPeriodMS; /*= 60000L;*/

    private final Timer myTimer/* = new Timer()*/; // Создаем таймер
//        final Handler uiHandler = new Handler();


    public DispatcherEngineImpl(){
        this.dispatcherPeriodMS = 1000L;
        this.myTimer = new Timer();
        startMainTimer();


//        taskRepository

    }



    private void startMainTimer() {
        myTimer.schedule(new TimerTask() { // Определяем задачу
            @Override
            public void run() {
               log.info("Квант диспетчера");
                dispatcherQuantum();
            };
        }, 10000L, dispatcherPeriodMS);
    }

    private void scanDataBase(){

    }

    // Основная логика диспетчера
    private void dispatcherQuantum(){


        ArrayList<Task> taskQueue = taskService.getTasksByStatus(TaskStatusEnum.В_ОЧЕРЕДИ);

        if (taskQueue.size()!=0){
            log.info("Задача {} в очереди",taskQueue.get(0).getName());
        }
    }

    public void setTaskService(TaskService taskService){
        this.taskService = taskService;
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
