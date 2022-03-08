package Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic.Impl;


import Ru.IVT.JWT_REST_Dispatcher.DTO.NewTaskDto;
import Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic.DispathcerEnginge;
import Ru.IVT.JWT_REST_Dispatcher.Model.Constanta;
import Ru.IVT.JWT_REST_Dispatcher.Model.InsideTaskStatusEnum;
import Ru.IVT.JWT_REST_Dispatcher.Model.Task;
import Ru.IVT.JWT_REST_Dispatcher.Model.TaskStatusEnum;
import Ru.IVT.JWT_REST_Dispatcher.Service.TaskService;
import Ru.IVT.JWT_REST_Dispatcher.Tools.BashTools;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Диспетчеризация, алгоритмы приоритетов
 *
 * @author Меньшиков Артём
 * <p>
 * Соглашения: UUID - по папке исходников
 */
/*
Все дейстия в task_in_run делает этот DispatcherEngine класс
 */

@Slf4j
@Component
public class DispatcherEngineImpl implements DispathcerEnginge {

    private TaskService taskService;

    private Long dispatcherQuantumPeriodMS;
    private Integer quantumsAtRaundRobin;
    private Integer curQuantumAtRaundRobin;
    private LinkedList<Task> roundRobinTaskQueue;
    private HashMap<Long, LinkedList<Task>> UserQueues;

    /** Список millTaskList хранит выполняющиеся задачи. */
    private LinkedList<Task> millTaskList;
    private Task roundRobinCurrenTask;

    private final Timer myTimer; // Создаем таймер

    @Value("${local.paths.save.taskSources}")
    private String taskUploadPath;

    //    @Value("${local.paths.dockerTmpDir}")
    private String dockerDirPath;

    @Autowired
    public DispatcherEngineImpl(TaskService taskService) {
        this.dispatcherQuantumPeriodMS = 10000L;
        this.quantumsAtRaundRobin = 7;
        this.curQuantumAtRaundRobin = 0;
        this.roundRobinTaskQueue = new LinkedList<>();
        this.UserQueues = new HashMap<>();

        this.millTaskList = new LinkedList<>();
        this.myTimer = new Timer();
        startMainTimer();

        this.taskService = taskService;

        // Отображение внешнего состояния на внутреннее с разрешением противоречий.

        mappingOutside2InsideTaskState();
        initUserTaskQueues();

        dockerDirPath = "/home/artem/Dispatcher_files/DockerTmp/";

    }

    private void initUserTaskQueues() {


        // На случай остановки сервера
        // TODO Подумать какие ещё состояния надо добавлять в очереди
        ArrayList<Task> taskQueueRunning = taskService.getTasksByInsideStatus(InsideTaskStatusEnum.ВЫПОЛНЕНИЕ);
        ArrayList<Task> taskQueue = taskService.getTasksByInsideStatus(InsideTaskStatusEnum.В_ОЧЕРЕДИ);

        taskQueue.addAll(taskQueueRunning);

        for (Task task : taskQueue) {
            try {
                LinkedList<Task> UserQueue = new LinkedList<>();

                if (UserQueues.get(task.getUser_id()) != null) {
                    UserQueue = UserQueues.get(task.getUser_id());
                }

                UserQueue.add(task);
                UserQueues.put(task.getUser_id(), (LinkedList<Task>) UserQueue.clone());

            } catch (Exception e) {
                e.printStackTrace();
                log.warn(e.getMessage());
            }
        }

    }

    // Отображение внешнего состояния на внутреннее с разрешением противоречий.
    private void mappingOutside2InsideTaskState() {
        /*По умолчанию - не определено, если не определено то отобразить, иначе - главенство внутреннего состояния
         * */

        ArrayList<Task> allTasks = taskService.getAllTasks();

        for (Task task : allTasks) {
            try {

                NewTaskDto newTaskDto = new NewTaskDto();
                newTaskDto.setId(task.getId());


                if (task.getStatus() == TaskStatusEnum.ОЖИДАНИЕ_ЗАПУСКА ||
                        task.getStatus() == TaskStatusEnum.ОЖИДАНИЕ_ДАННЫХ ||
                        task.getStatus() == TaskStatusEnum.ОЖИДАНИЕ_ИСХОДНИКОВ) {
                    newTaskDto.setInside_status(InsideTaskStatusEnum.НЕ_ОПРЕДЕЛЕНО);
                    taskService.updateInsideTaskStatus(newTaskDto);
                }

                if (task.getStatus() == TaskStatusEnum.В_ОЧЕРЕДИ) {
                    newTaskDto.setInside_status(InsideTaskStatusEnum.В_ОЧЕРЕДИ);
                    taskService.updateInsideTaskStatus(newTaskDto);
                }

                if (task.getStatus() == TaskStatusEnum.УДАЛЕНА) {
                    newTaskDto.setInside_status(InsideTaskStatusEnum.УДАЛЕНА);
                    taskService.updateInsideTaskStatus(newTaskDto);
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }


    }

    private void mappingInside2OutsideTaskState() {
        /*По умолчанию - не определено, если не определено то отобразить, иначе - главенство внутреннего состояния
         * */

        ArrayList<Task> allTasks = taskService.getAllTasks();

        for (Task task : allTasks) {
            try {

                NewTaskDto newTaskDto = new NewTaskDto();
                newTaskDto.setId(task.getId());

                if (task.getInside_status() == InsideTaskStatusEnum.ВЫПОЛНЕНИЕ ||
                        task.getInside_status() == InsideTaskStatusEnum.ПРИОСТАНОВЛЕНА ||
                        task.getInside_status() == InsideTaskStatusEnum.СОЗДАН_ОБРАЗ) {
                    newTaskDto.setStatus(TaskStatusEnum.ВЫПОЛНЕНИЕ);
                    taskService.updateTaskStatusByTaskId(newTaskDto);
                }

                if (task.getInside_status() == InsideTaskStatusEnum.ЗАВЕРШЕНА) {
                    newTaskDto.setStatus(TaskStatusEnum.ЗАВЕРШЕНА);
                    taskService.updateTaskStatusByTaskId(newTaskDto);
                }

                if (task.getInside_status() == InsideTaskStatusEnum.УДАЛЕНА) {
                    newTaskDto.setStatus(TaskStatusEnum.УДАЛЕНА);
                    taskService.updateTaskStatusByTaskId(newTaskDto);
                }

                if (task.getInside_status() == InsideTaskStatusEnum.ОШИБКА_ВЫПОЛНЕНИЯ) {
                    newTaskDto.setStatus(TaskStatusEnum.ОШИБКА_ВЫПОЛНЕНИЯ);
                    taskService.updateTaskStatusByTaskId(newTaskDto);
                }

                if (task.getInside_status() == InsideTaskStatusEnum.ОШИБКА_КОМПИЛЯЦИИ) {
                    newTaskDto.setStatus(TaskStatusEnum.ОШИБКА_КОМПИЛЯЦИИ);
                    taskService.updateTaskStatusByTaskId(newTaskDto);
                }

            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }

    }

    public void setTaskService(TaskService taskService) {
        this.taskService = taskService;
    }


    private Long counter = Long.valueOf(0);

    private void startMainTimer() {
        myTimer.schedule(new TimerTask() { // Определяем задачу
            @Override
            public void run() {
                log.info("Квант диспетчера");
//               taskService.setTest((counter=counter+1).toString());
                try {
                    dispatcherQuantum();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            ;
        }, 10000L, dispatcherQuantumPeriodMS);
    }

    /**Удаляет все архивы и папки, на которые не ссылаются задачи из БД */
    private void brushTrash(){}



    private boolean isFolderExist(String folderPath) {

        Path path = Paths.get(folderPath);

        return Files.exists(path);
    }

    private String getTaskUnZipDir(Long taskId) throws Exception {

        String str = taskService.getTaskById(taskId).getSource_file_name().replace(".zip", "");

        return taskService.getTaskById(taskId).getSource_file_name().replace(".zip", "");
    }

    private void checkAndPrepareFolders(Long taskId) throws Exception {
        Task task = taskService.getTaskById(taskId);

        String sourcesPath = task.getSource_file_name();
        String dataPath = task.getData_file_name();

        String dir2UpZip = getTaskUnZipDir(taskId);

        if (!isFolderExist(dir2UpZip)) {

            File file = new File(dir2UpZip);
            file.mkdir();


            file = new File(dir2UpZip + "/Выход");
            file.mkdir();

            unzip(sourcesPath, dir2UpZip);
            unzip(dataPath, dir2UpZip/*+"/Input"*/);

            int a = 1;
        }


    }

    private String getUUIDFromFileName(String fileName) {

        fileName = fileName.replace(".zip", "");

        Pattern pattern = Pattern.compile("\\..*$");
        Matcher matcher = pattern.matcher(fileName);
        String str = null;
        if (matcher.find()) {
            str = fileName.substring(matcher.start(), matcher.end());
        }

//        assert str != null;
        str = str.replace(".", "");

        return str;
    }

    private void unzip(final String zipFilePath, final String unzipLocation) throws IOException {
        Process proc = Runtime.getRuntime().exec("unzip " + zipFilePath + " -d " + unzipLocation);
    }


    // Создаётся автоматически, при заверешении задачи
    private void zip(final String zipFilePath, final String folderPath) throws IOException {
        Process proc = Runtime.getRuntime().exec("zip -r " + zipFilePath + " " + folderPath);
    }


    private void dockerCreateImage(String dir2UpZip, Long taskId) throws Exception {


        String taskSourceFile = taskService.getTaskById(taskId).getSource_file_name();


        try (FileWriter writer = new FileWriter(dir2UpZip + "/Dockerfile", false)) {
            writer.write("FROM python\n");
            writer.write("WORKDIR /code\n");
            writer.write("COPY . .\n");
            writer.write("CMD [\"python3\",\"Main.py\"]\n");

            writer.flush();
        } catch (Exception e) {
            log.error(e.getMessage());
        }


        String dockerCommand = "echo 'q' | sudo -S docker build -t " +
                getUUIDFromFileName(taskSourceFile) + " " + dir2UpZip;

        try {
            ArrayList<String> bashRes = BashTools.bashCommand(dockerCommand, dir2UpZip);
        } catch (Exception e) {
            throw e;
        }

        int a = 1;

    }



    private boolean isTaskRunInDocker(Long taskId) throws Exception{

        //        sudo docker ps -aqf "ancestor=2da48882-61a9-4a26-8399-39d4f5d97a60" контейнер по образу
//        sudo docker ps  -aqf "id=c8ebce292681"  - данные по контейнеру
//        sudo docker inspect --format='{{json .State }}' <container_id>

        Task task = taskService.getTaskById(taskId);
        String fileUUID = getUUIDFromFileName(task.getSource_file_name());

        ArrayList<String> containersFromImage =
                BashTools.bashCommand("echo 'q'|sudo -S docker ps -aqf 'ancestor='"+fileUUID ,"");

        if(containersFromImage.size()==1){

            ArrayList<String> commandResult =
                    BashTools.bashCommand("echo 'q'|sudo -S docker inspect --format='{{json .State }}' "+
                            containersFromImage.get(0),"");

            JSONObject taskState = new JSONObject(commandResult.get(0));

            return taskState.get("Status")=="running";


        }
        else{
            // Должен быть 1 контейнер на 1 образ
            ArrayList<String> commandResult =
                    BashTools.bashCommand("echo 'q'|sudo -S docker inspect --format='{{json .State }}' "+
                            containersFromImage.get(0),"");

            JSONObject taskState = new JSONObject(commandResult.get(0));

            return taskState.get("Status")=="running";
        }

//        return true;
    }


    private boolean isDockerImageExist(Long taskId) throws Exception {
        Task task = taskService.getTaskById(taskId);
        String fileUUID = getUUIDFromFileName(task.getSource_file_name());


        ArrayList<String> commandResult = BashTools.bashCommand("echo 'q'|sudo -S docker inspect --format='{{json .Config}}' $INSTANCE_ID " +
                getUUIDFromFileName(task.getSource_file_name()), "");

        return !commandResult.get(0).equals("");

    }

    private boolean isTaskRun(Long taskId) throws Exception {

        Task task = taskService.getTaskById(taskId);

        ArrayList<String> commandResult = BashTools.bashCommand("echo 'q'|sudo -S docker inspect --format='{{json .Config}}' $INSTANCE_ID " +
                getUUIDFromFileName(task.getSource_file_name()), "");

        return false;
    }


    /**
     * @author Меньшиков Артём
     * Запускает задачу #runTask
     */
    private void runTask(Long taskId) throws Exception {
        /*
         * Запустить в докере
         * или снять с паузы
         * */


        try {
            Task task = taskService.getTaskById(taskId);

            ArrayList<String> commandResult = BashTools.bashCommand("echo 'q'|sudo -S docker run " +
                    getUUIDFromFileName(task.getSource_file_name()), "");

            NewTaskDto newTaskDto = new NewTaskDto();
            newTaskDto.setId(taskId);
            newTaskDto.setInside_status(InsideTaskStatusEnum.ВЫПОЛНЕНИЕ);
            taskService.updateInsideTaskStatus(newTaskDto);

            millTaskList.add(task);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void checkTaskCompleteOrHaveTheErrors() {
        /* Удаление из очереди, если завершена или ошибки
        изменение внутреннего состояния
        если завершена добавить в бд путь к выходному файлу
        */
    }

    private void pauseTask(Long taskId) {
    }


    // Основная логика диспетчера
    private void dispatcherQuantum() throws Exception {


        // Полное соответсвие с БД на момент обновления
        prepareFolders();
        createImages();

        updateUserQueues();


        updateMillTaskList();

        sendUserTaskQueuesToMill();

        // Переделать по нормальному: как каждые 10 секунд не доставать все задачи?


//        checkTaskCompleteOrHaveTheErrors();

//        // Первый запуск или обошли круг
//        if (this.curQuantumAtRaundRobin == 0/*&&UserQueues.size()!=0*/) {
//            //Сердце диспетчера!
//
//            if (millTaskList.size() < Constanta.serverTasksLimit) {
//                // Место на запуск есть
//                runAllNeededTaskToRunningTaskList();
//            }
//
//            Task firstTask = roundRobinTaskQueue.getFirst();
//
////            roundRobinTaskQueue.addLast(firstTask);
//
//            if (isDockerImageExist(firstTask.getId())) {
//                if (!isTaskRun(firstTask.getId())) {
//                    if (roundRobinCurrenTask == null) {
//                        roundRobinCurrenTask = firstTask;
//                    } else {
//                        pauseTask(roundRobinCurrenTask.getId());
//                        roundRobinTaskQueue.addLast(roundRobinCurrenTask);
//                        roundRobinCurrenTask = firstTask;
//                    }
//                    roundRobinTaskQueue.removeFirst();
//                    runTask(firstTask.getId());
//                } else {
//                    log.error("Задача уже запущена");
//                }
//
//            } else {
//                log.error("Образа задачи не существует");
//            }
//
//        }
//
//        // Каждые quantumsAtRaundRobin квантов задачи сменяются
//        this.curQuantumAtRaundRobin = this.curQuantumAtRaundRobin % this.quantumsAtRaundRobin;
//
//
////        if (taskQueue.size()!=0){
////            log.info("Задача {} в очереди",taskQueue.get(0).getName());
////        }

        mappingInside2OutsideTaskState();
        mappingOutside2InsideTaskState();


    }




    /** Проверяет список millTaskList на завершённые или ошибочные программы(в докере).
     * По окончании работы метода список millTaskList содержит только выполняющиеся задачи*/

    private void updateMillTaskList() {

//        docker ps -aqf "ancestor=<image_name>"
//        docker ps -aqf "ancestor=<image_name>"
//        sudo docker ps -af "ancestor=2da48882-61a9-4a26-8399-39d4f5d97a60" контейнер по образу
//        sudo docker ps  -af "id=c8ebce292681"  - данные по контейнеру
//        sudo docker inspect --format='{{json .State }}' <container_id>

        // TODO функцию проверки состояния контейнера по id задачи
        //  цикл по задачам в списке мельницы с проверкой

        LinkedList<Task> taskToRemove = new LinkedList<>();

        millTaskList.forEach(task -> {
            try {
                if (!isTaskRunInDocker(task.getId())) taskToRemove.add(task);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        taskToRemove.forEach(task -> {
            millTaskList.remove(task);
            removeTaskWithSaveResult( task.getId());
        });



    }


    /**Удаляет задачу: образ, контейнер, но сохраняет исходники, данные, итог. Итогом может быть
     * как логи, так и файлы, а также и первое и второе. Всё это хранится в отдельной папке. Задача может
     * быть удалена из-за ошибок, поэтому пользователь скорее всего перезагрузит исходники или данные. При
     * перезагрузке новых данных
     * */
    private void removeTaskWithSaveResult(Long id) {


    }

    private boolean canRunTaskOfThisUser(Long UserId) {
        return true;
    }

    private void sendUserTaskQueuesToMill2() throws Exception {
        /* Привязка к пользователям */
    }

    private void roundRobinUsers() {
        /* Проверка на повторения: если в мельнице пользователи повторяются и есть ожидающие пользователи,
         * задач которых нет в мельнице, то поставить на выполнение ожидающих пользователей
         * */
    }

    private void sendUserTaskQueuesToMill() throws Exception {

        // Любое действие с мельницей сопровождается изменением в докере состояний образов

        /* Из множества очередей пользователей вычесть множество пользователей на мельнице.
         * Получится количество пользователей, у которых задач не запущено.
         * Проверка на повторения: если в мельнице пользователи повторяются и есть ожидающие пользователи,
         * задач которых нет в мельнице, то поставить на выполнение ожидающих пользователей - сделать в отдельной
         * функции
         * */


        // TODO Проверка на повторение - если есть ожидающие пользователи, при этом кто-то из пользователей запустил
        //  более одной задачи, то снять с выполнения более раннюю задачу и поместить задачу ожидающего пользователя

        Set<Long> runningUsersSet = new HashSet<>();


        millTaskList.forEach((t) -> {
            runningUsersSet.add(t.getUser_id());
        });

        Set<Long> newWaitingUsers = UserQueues.keySet();

        newWaitingUsers.removeAll(runningUsersSet);


        int freePositionCounter = Constanta.serverTasksLimit - millTaskList.size();
        if (freePositionCounter > 0) {

            LinkedList<Task> firstTasks = new LinkedList<>();
            UserQueues.forEach((k, v) -> {
                firstTasks.add(v.getFirst());
            });

            if (freePositionCounter < newWaitingUsers.size()) {
                /* Создаем список из всех первых задач очередей и вибираем самые раниие задачи,
                 * добавляем в мельницу, извлекаем из очереди, меняем внутреннее состояние
                 * */

                // В порядке возрастания новизны, чем меньше индекс тем раньше пришла задача
                firstTasks.sort((task1, task2) -> {

                    if (task1.getCreated().before(task2.getCreated()))
                        return -1;
                    else if (task1.getCreated().after(task2.getCreated()))
                        return 1;
                    return 0;
                });

                /* Удаляем из очереди, и запускаем(докер + мельница + смена состояния)
                 * Не весь список первых задач, а только то количество, кторое = свободным местам в мельнице*/

//                int i = 0;
//                while(i<freePositionCounter){
//                    if(canRunTaskOfThisUser(firstTasks.get(i).getUser_id())){
//                        UserQueues.get(firstTasks.get(i).getUser_id()).removeFirst();
//                        runTask(firstTasks.get(i).getId());
//                        ++i;
//                    }
//                }

                for (int i = 0; i < freePositionCounter; ++i) {
                    runTask(UserQueues.get(firstTasks.get(i).getUser_id()).removeFirst().getId());
                }

            } else if (freePositionCounter == newWaitingUsers.size()) {
                // из каждой очереди в место на мельнице, состояние

//                int i = 0;
//                while(i<freePositionCounter){
//                    if(canRunTaskOfThisUser(firstTasks.get(i).getUser_id())){
//                        UserQueues.get(firstTasks.get(i).getUser_id()).removeFirst();
//                        runTask(firstTasks.get(i).getId());
//                    }
//                    ++i;
//                }

                for (int i = 0; i < freePositionCounter; ++i) {
//                    UserQueues.get(firstTasks.get(i).getUser_id()).removeFirst();
                    runTask(UserQueues.get(firstTasks.get(i).getUser_id()).removeFirst().getId());
                }
            } else {
                /* freePositionCounter>newWaitingUsers.size()
                 * Тасование: берём по одной первой задаче из каждой очереди и добавляем в мельницу, до тех пор
                 * пока вся мельница не заполнится или не закончатся задачи, состояние
                 * */

                ArrayList<Task> allTheQueueTasks = taskService.getTasksByStatus(TaskStatusEnum.В_ОЧЕРЕДИ);

                if (allTheQueueTasks.size() > freePositionCounter) {


                    AtomicBoolean canSendToMill = new AtomicBoolean(true);
                    while (canSendToMill.get()) {

                        UserQueues.forEach((k, v) -> {

                            try {
                                runTask(v.removeFirst().getId());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (millTaskList.size() == Constanta.serverTasksLimit) {
                                canSendToMill.set(false);
                            }

//                            firstTasks.add( v.getFirst());
                        });
                    }

                } else {
                    allTheQueueTasks.forEach((t) -> {
                        try {
                            runTask(UserQueues.get(t.getUser_id()).removeFirst().getId());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        }
    }


    private void prepareFolders() {
        ArrayList<Task> taskQueue = taskService.getTasksByInsideStatus(InsideTaskStatusEnum.В_ОЧЕРЕДИ);

        // Подготовка папок
        for (Task task : taskQueue) {
            try {
                checkAndPrepareFolders(task.getId());
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
        }
    }

    private void createImages() {

        ArrayList<Task> taskQueue = taskService.getTasksByInsideStatus(InsideTaskStatusEnum.В_ОЧЕРЕДИ);

        // И создание образов
        for (Task task : taskQueue) {
            try {

                if (isFolderExist(getTaskUnZipDir(task.getId()))) {
                    dockerCreateImage(getTaskUnZipDir(task.getId()), task.getId());

                }

            } catch (Exception e) {
                log.warn(e.getMessage());
            }
        }
    }

    private void updateUserQueues() {

        // За время работы задачи могли удалить, поэтому каждый раз приводим в соответсвие с состоянием из БД

        //TODO сделать правильную проверку удалена ли задача
        initUserTaskQueues();

//        ArrayList<Task> taskQueue = taskService.getTasksByInsideStatus(InsideTaskStatusEnum.В_ОЧЕРЕДИ);
//
//        UserQueues = new HashMap<>();
//
//        for (Task task:taskQueue) {
//            try {
//
//                LinkedList<Task> UserQueue = new LinkedList<>();
//                UserQueue.add(task);
//
//                UserQueues.put(task.getUser_id(), (LinkedList<Task>) UserQueue.clone());
//
//            }catch (Exception e){
//                log.warn(e.getMessage());
//            }
//        }

    }

    private void runAllNeededTaskToRunningTaskList() {

    }


    @Override
    public Timer getMyTimer() {
        return null;
    }

    @Override
    public void init() {

    }

    @Override
    public String getConsoleOutput(Long taskId) throws Exception {

        Task task = taskService.getTaskById(taskId);

        ArrayList<String> commandResult = BashTools.bashCommand("echo 'q'|sudo -S docker logs " +
                getUUIDFromFileName(task.getSource_file_name()), "");

        return null;
    }

    @Override
    public boolean delTask(Long UserId, Long taskId) throws Exception {

        try {
            NewTaskDto newTaskDto = new NewTaskDto();

            newTaskDto.setId(taskId);
            newTaskDto.setStatus(TaskStatusEnum.УДАЛЕНА);


            // Удалить все папки и файлы данной задачи

            taskService.updateTaskStatus(newTaskDto, UserId);

            Task task = taskService.getTaskById(taskId, UserId);


            String taskFolder = taskService.getUnzipDirById(taskId, UserId);
            String taskUUID = getUUIDFromFileName(taskFolder);


            BashTools.bashCommand("echo 'q' |  sudo -S rm -R " + taskFolder, "");

            BashTools.bashCommand("echo 'q' |  sudo -S rm " + task.getSource_file_name(), "");
            BashTools.bashCommand("echo 'q' |  sudo -S rm " + task.getData_file_name(), "");

            BashTools.bashCommand("echo 'q'|sudo -S docker rmi " + taskUUID, "");

            // TODO Удаление контейнеров по задаче

            taskService.deleteTask(newTaskDto, UserId);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
