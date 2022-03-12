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
 * <br>
 * Соглашения: UUID - по папке исходников
 */
/*
Все дейстия в task_in_run делает этот DispatcherEngine класс
 */


//TODO передавать аргументы на запуск файла

@Slf4j
@Component
public class DispatcherEngineImpl implements DispathcerEnginge {

    private TaskService taskService;

    private Long dispatcherQuantumPeriodMS;
    private Integer quantumsAtRaundRobin;
    private Integer curQuantumAtRaundRobin;
    private LinkedList<Task> roundRobinTaskQueue;
    private HashMap<Long, LinkedList<Task>> UserQueues;

    private String argsFileName;

    /**
     * Список millTaskList хранит выполняющиеся задачи.
     */
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

        this.argsFileName = "Аргументы.txt";

        this.millTaskList = new LinkedList<>();
        this.myTimer = new Timer();
        startMainTimer();

        this.taskService = taskService;

        // Отображение внешнего состояния на внутреннее с разрешением противоречий.

        initMillTask();
        mappingOutside2InsideTaskState();
        initUserTaskQueues();

        dockerDirPath = "/home/artem/Dispatcher_files/DockerTmp/";

    }

    private boolean isListContainsTask(ArrayList<Task> taskArray, Task task) {

        AtomicBoolean hasDuplicates = new AtomicBoolean(false);

        taskArray.forEach(task1 -> {
            if (Objects.equals(task1.getId(), task.getId())) hasDuplicates.set(true);
        });

        return hasDuplicates.get();
    }

    private void clearDocker() throws IOException, InterruptedException {
        BashTools.bashCommand("echo 'q' | sudo -S docker stop $(sudo docker ps -aq)","");
        BashTools.bashCommand("echo 'q' | sudo -S sudo docker rm $(sudo docker ps -aq)","");
        BashTools.bashCommand("echo 'q' | sudo -S docker volume rm $(sudo docker volume  ls)","");
    }


    private void initUserTaskQueues() {


        // На случай остановки сервера
        // TODO Подумать какие ещё состояния надо добавлять в очереди
        // TODO сортировка задач в очереди по дате добавления
//        ArrayList<Task> taskQueueRunning = taskService.getTasksByInsideStatus(InsideTaskStatusEnum.ВЫПОЛНЕНИЕ);
        ArrayList<Task> taskQueue = taskService.getTasksByInsideStatus(InsideTaskStatusEnum.В_ОЧЕРЕДИ);

//        taskQueue.addAll(taskQueueRunning);

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

        UserQueues.forEach((UserId, taskList) -> {
            taskList.sort(this::taskComparator);
        });

    }

    private void initMillTask() {

        mappingDocker2InsideStatus();

        ArrayList<Task> runningTasks = taskService.getTasksByInsideStatus(InsideTaskStatusEnum.ВЫПОЛНЕНИЕ);

        millTaskList = new LinkedList<>();

//        int millCounter = 0;
        for (Task task : runningTasks) {
            millTaskList.add(task);
            if (millTaskList.size() == Constanta.serverTasksLimit) break;
        }


    }

    private void mappingDocker2InsideStatus() {

        ArrayList<Task> allTasks = taskService.getAllTasks();

        allTasks.forEach(task -> {

            try {

                String fileUUID = getUUIDFromFileName(task.getSource_file_name());


                ArrayList<String> commandResult =
                        BashTools.bashCommand("echo 'q'|sudo -S docker inspect --format='{{json .State }}' " +
                                "container_" + fileUUID, "");
//
                if ((!commandResult.isEmpty())&&!commandResult.get(0).isEmpty()) {
                    JSONObject taskState = new JSONObject(commandResult.get(0));

                    NewTaskDto newTaskDto = new NewTaskDto();
                    newTaskDto.setId(task.getId());

                    if ("running".equals(taskState.get("Status"))) {
                        newTaskDto.setInside_status(InsideTaskStatusEnum.ВЫПОЛНЕНИЕ);
                        taskService.updateInsideTaskStatus(newTaskDto);
                    } else if ("paused".equals(taskState.get("Status"))) {
                        newTaskDto.setInside_status(InsideTaskStatusEnum.ПРИОСТАНОВЛЕНА);
                        taskService.updateInsideTaskStatus(newTaskDto);
                    } else if ("exited".equals(taskState.get("Status")) && ((Objects.equals(taskState.get("ExitCode"), 0)))) {
                        newTaskDto.setInside_status(InsideTaskStatusEnum.ЗАВЕРШЕНА);
                        taskService.updateInsideTaskStatus(newTaskDto);
                    } else if ("exited".equals(taskState.get("Status")) && (!(Objects.equals(taskState.get("ExitCode"), 0)))) {
                        newTaskDto.setInside_status(InsideTaskStatusEnum.ОШИБКА_ВЫПОЛНЕНИЯ);
                        taskService.updateInsideTaskStatus(newTaskDto);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

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

    /**
     * Удаляет все архивы и папки, на которые не ссылаются задачи из БД
     */
    private void brushTrash() {
    }


    private boolean isFolderExist(String folderPath) {

        Path path = Paths.get(folderPath);

        return Files.exists(path);
    }

    private String getTaskUnZipDir(Long taskId) throws Exception {

        String str = taskService.getTaskById(taskId).getSource_file_name().replace(".zip", "");

        return taskService.getTaskById(taskId).getSource_file_name().replace(".zip", "") + "/";
    }

    private void checkAndPrepareFolders(Long taskId) throws Exception {
        Task task = taskService.getTaskById(taskId);

        String sourcesPath = task.getSource_file_name();
        String dataPath = task.getData_file_name();

        String dir2UpZip = getTaskUnZipDir(taskId);

        if (!isFolderExist(dir2UpZip)) {

            File file = new File(dir2UpZip);
            file.mkdir();


            file = new File(dir2UpZip + "Выход");
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
    private void zip(final String relZipFilePath, final String folderPath) throws IOException, InterruptedException {
//        BashTools.bashCommand("echo 'q' | sudo -S zip -r "+ relZipFilePath + " " + folderPath,"");
        BashTools.bashCommand("cd "+folderPath+" && (echo 'q' | sudo -S zip -r ./"+relZipFilePath+" ./"+relZipFilePath+")","");
//        Process proc = Runtime.getRuntime().exec("zip -r " + zipFilePath + " " + folderPath);
    }

    private ArrayList<String> getCmdParams(Long taskId) throws Exception {
        ArrayList<String> params = new ArrayList<>();

        try {

//        String workDir = getTaskUnZipDir(taskId);

            Path argsPath = Paths.get(getTaskUnZipDir(taskId) + this.argsFileName);
            params = (ArrayList<String>) Files.readAllLines(argsPath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return params;
    }


    private void dockerCreateImage(String dir2UpZip, Long taskId) throws Exception {


        String taskSourceFile = taskService.getTaskById(taskId).getSource_file_name();

        StringBuilder params = new StringBuilder();

        if (hasTaskCmdParams(taskId)) {

            ArrayList<String> arrParams = getCmdParams(taskId);
            arrParams.forEach(param -> {
                params.append(param);
                params.append(" ");
            });
        }


        try (FileWriter writer = new FileWriter(dir2UpZip + "/Dockerfile", false)) {
            writer.write("FROM python\n");
            writer.write("WORKDIR /code\n");
            writer.write("COPY . .\n");
            writer.write("CMD python3 Main.py " + params + " \n");

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

    private boolean hasTaskCmdParams(Long taskId) throws Exception {
        Path argsPath = Paths.get(getTaskUnZipDir(taskId) + this.argsFileName);
        return Files.exists(argsPath);
    }


    private boolean isTaskRunInDocker(Long taskId) throws Exception {

        //        sudo docker ps -aqf "ancestor=2da48882-61a9-4a26-8399-39d4f5d97a60" контейнер по образу
//        sudo docker ps  -aqf "id=c8ebce292681"  - данные по контейнеру
//        sudo docker inspect --format='{{json .State }}' <container_id>

        Task task = taskService.getTaskById(taskId);
        String fileUUID = getUUIDFromFileName(task.getSource_file_name());

//        ArrayList<String> containersFromImage =
//                BashTools.bashCommand("echo 'q'|sudo -S docker ps -aqf 'ancestor='"+fileUUID ,"");

//        if(containersFromImage.size()>=1){

        ArrayList<String> commandResult =
                BashTools.bashCommand("echo 'q'|sudo -S docker inspect --format='{{json .State }}' " +
                        "container_" + fileUUID, "");

        if (!commandResult.get(0).isEmpty()) {

            JSONObject taskState = new JSONObject(commandResult.get(0));

            return "running".equals(taskState.get("Status"));
        }

        return false;


//        }
//        else {
//            // Должен быть 1 контейнер на 1 образ
//            ArrayList<String> commandResult =
//                    BashTools.bashCommand("echo 'q'|sudo -S docker inspect --format='{{json .State }}' "+
//                            containersFromImage.get(0),"");
//
//            JSONObject taskState = new JSONObject(commandResult.get(0));

//            return false;
//        }


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
            String taskUUID = getUUIDFromFileName(task.getSource_file_name());

            // DONE_TODO  -d добавить
            ArrayList<String> commandResult = BashTools.bashCommand("echo 'q'|sudo -S docker run -d " +
                    "--mount source=vol_" + taskUUID + ",target=/code " +
//                    "--log-driver syslog "+
                    " --name container_" + taskUUID + " " +
                    taskUUID, "");

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

//        NewTaskDto newTaskDto = new NewTaskDto();
//        newTaskDto.setId(taskId);
//        newTaskDto.setInside_status(InsideTaskStatusEnum.ПРИОСТАНОВЛЕНА);
//        newTaskDto.set
//
    }


    // Основная логика диспетчера
    private void dispatcherQuantum() throws Exception {

        mappingOutside2InsideTaskState();


        // Полное соответсвие с БД на момент обновления
        prepareFolders();
        createImages();

        mappingDocker2InsideStatus();

        updateUserQueues();


        updateMillTaskList();

        sendUserTaskQueuesToMill();

        log.info("Задач на выполнении {}", millTaskList.size());

        // Переделать по нормальному: как каждые 10 секунд не доставать все задачи?


        mappingInside2OutsideTaskState();



    }


    /**
     * Проверяет список millTaskList на завершённые или ошибочные программы(в докере).
     * По окончании работы метода список millTaskList содержит только выполняющиеся задачи
     */

    private void updateMillTaskList() {

//        mappingDocker2InsideStatus();

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
//            removeTaskWithSaveResult(task.getId());
        });


        int a = 0;

    }


    /**
     * Удаляет задачу: образ, контейнер, но сохраняет исходники, данные, итог. Итогом может быть
     * как логи, так и файлы, а также и первое и второе. Всё это хранится в отдельной папке. Задача может
     * быть удалена из-за ошибок, поэтому пользователь, скорее всего, перезагрузит исходники или данные. При
     * перезагрузке новых данных
     */
    private void removeTaskWithSaveResult(Long taskId) {
        try {

            Task task = taskService.getTaskById(taskId);
            String taskUUID = getUUIDFromFileName(task.getSource_file_name());


            ArrayList<String> containersFromImage =
                    BashTools.bashCommand("echo 'q'|sudo -S docker ps -aqf 'ancestor='" + taskUUID, "");

            containersFromImage.forEach(container -> {
                try {
                    BashTools.bashCommand("echo 'q'|sudo -S docker rm " + container, "");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            BashTools.bashCommand("echo 'q'|sudo -S docker rmi " + taskUUID, "");


//            taskService.deleteTask(newTaskDto, UserId);

//            return true;
        } catch (Exception e) {
            e.printStackTrace();
//            return false;
        }
    }

    private boolean canRunTaskOfThisUser(Long UserId) {
        return true;
    }

    private void sendUserTaskQueuesToMill2() throws Exception {
        /* Привязка к пользователям */
    }

    /** Проверка на повторения: если в мельнице пользователи повторяются и есть ожидающие пользователи,
     * задач которых нет в мельнице, то поставить на выполнение ожидающих пользователей
     * */
    private void roundRobinTasks() throws Exception {
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

        Set<Long> newWaitingUsersSet = new HashSet<>(UserQueues.keySet());

        newWaitingUsersSet.removeAll(runningUsersSet);

        ArrayList<Long> newWaitingUsersList = new ArrayList<>(newWaitingUsersSet);


        // Сортировка пользователей по порядку поступления задач
        newWaitingUsersList.sort((User1, User2) -> {

            if (UserQueues.get(User1).getFirst().getCreated().before(UserQueues.get(User2).getFirst().getCreated()))
                return -1;
            else if (UserQueues.get(User1).getFirst().getCreated().after(UserQueues.get(User2).getFirst().getCreated()))
                return 1;
            return 0;
        });


        int freePositionCounter = Constanta.serverTasksLimit - millTaskList.size();
        if ( ( freePositionCounter > 0 ) && ( UserQueues.size() > 0 ) ) {

            if (freePositionCounter <= newWaitingUsersSet.size()) {
                // из каждой очереди в место на мельнице, состояние

                for (Long UserId : newWaitingUsersList) {

                    if (millTaskList.size() < Constanta.serverTasksLimit) {
                        runTask(UserQueues.get(UserId).removeFirst().getId());
                    } else {
                        break;
                    }

                }

            } else {
                /* freePositionCounter>newWaitingUsersSet.size()
                 * Тасование: берём по одной первой задаче из каждой очереди и добавляем в мельницу, до тех пор
                 * пока вся мельница не заполнится или не закончатся задачи, состояние
                 * */

//                ArrayList<Task> allTheQueueTasks = taskService.getTasksByStatus(TaskStatusEnum.В_ОЧЕРЕДИ);
//
//
//                if (allTheQueueTasks.size() > freePositionCounter) {


                AtomicBoolean canSendToMill = new AtomicBoolean(true);

                // TODO возможно зацикливание из-за пустой очереди
                while (canSendToMill.get()) {


                    updateUserQueues();
                    ArrayList<Long> UsersList = new ArrayList<>(UserQueues.keySet());

                    // Сортировка пользователей по дате создания первой задачи в очереди
                    UsersList.sort((User1, User2) -> {

                        if (UserQueues.get(User1).getFirst().getCreated().before(UserQueues.get(User2).getFirst().getCreated()))
                            return -1;
                        else if (UserQueues.get(User1).getFirst().getCreated().after(UserQueues.get(User2).getFirst().getCreated()))
                            return 1;
                        return 0;
                    });

                    for (Long UserId : UsersList) {

                        if (millTaskList.size() < Constanta.serverTasksLimit) {
                            runTask(UserQueues.get(UserId).removeFirst().getId());
                        } else {
                            canSendToMill.set(false);
                            break;
                        }
                    }

                    if(UserQueues.size()==0){
                        canSendToMill.set(false);
                    }

                }

                /*} else {
                    allTheQueueTasks.forEach((t) -> {
                        try {
                            runTask(UserQueues.get(t.getUser_id()).removeFirst().getId());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }*/
            }
        }
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

        Set<Long> newWaitingUsersSet = new HashSet<>(UserQueues.keySet());

        newWaitingUsersSet.removeAll(runningUsersSet);

        ArrayList<Long> newWaitingUsersList = new ArrayList<>(newWaitingUsersSet);


        // Сортировка пользователей по порядку поступления задач
        newWaitingUsersList.sort((User1, User2) -> {

            if (UserQueues.get(User1).getFirst().getCreated().before(UserQueues.get(User2).getFirst().getCreated()))
                return -1;
            else if (UserQueues.get(User1).getFirst().getCreated().after(UserQueues.get(User2).getFirst().getCreated()))
                return 1;
            return 0;
        });


        int freePositionCounter = Constanta.serverTasksLimit - millTaskList.size();
        if ( ( freePositionCounter > 0 ) && ( UserQueues.size() > 0 ) ) {

            if (freePositionCounter <= newWaitingUsersSet.size()) {
                // из каждой очереди в место на мельнице, состояние

                for (Long UserId : newWaitingUsersList) {

                    if (millTaskList.size() < Constanta.serverTasksLimit) {
                        runTask(UserQueues.get(UserId).removeFirst().getId());
                    } else {
                        break;
                    }

                }

            } else {
                /* freePositionCounter>newWaitingUsersSet.size()
                 * Тасование: берём по одной первой задаче из каждой очереди и добавляем в мельницу, до тех пор
                 * пока вся мельница не заполнится или не закончатся задачи, состояние
                 * */

//                ArrayList<Task> allTheQueueTasks = taskService.getTasksByStatus(TaskStatusEnum.В_ОЧЕРЕДИ);
//
//
//                if (allTheQueueTasks.size() > freePositionCounter) {


                AtomicBoolean canSendToMill = new AtomicBoolean(true);

                // TODO возможно зацикливание из-за пустой очереди
                while (canSendToMill.get()) {


                    updateUserQueues();
                    ArrayList<Long> UsersList = new ArrayList<>(UserQueues.keySet());

                    // Сортировка пользователей по дате создания первой задачи в очереди
                    UsersList.sort((User1, User2) -> {

                        if (UserQueues.get(User1).getFirst().getCreated().before(UserQueues.get(User2).getFirst().getCreated()))
                            return -1;
                        else if (UserQueues.get(User1).getFirst().getCreated().after(UserQueues.get(User2).getFirst().getCreated()))
                            return 1;
                        return 0;
                    });

                    for (Long UserId : UsersList) {

                        if (millTaskList.size() < Constanta.serverTasksLimit) {
                            runTask(UserQueues.get(UserId).removeFirst().getId());
                        } else {
                            canSendToMill.set(false);
                            break;
                        }
                    }

                    if(UserQueues.size()==0){
                        canSendToMill.set(false);
                    }

                }

                /*} else {
                    allTheQueueTasks.forEach((t) -> {
                        try {
                            runTask(UserQueues.get(t.getUser_id()).removeFirst().getId());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }*/
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



    /**  Для каждой задачи, которая завершена успешно или по ошибке,
     * сохраняет содержимое папки ./Выход и консольный вывод в архив, который расположен по пути
     * getTaskUnZipDir(taskId). Таким образом, каждая завершённая задача имеет архив с папкой ./Выход и файлом
     * Консольный_вывод.txt. Данная функция вызывается при запросе клиента на возвращение задачи
     * */

    public String getTaskOutputPack(Long taskId) throws Exception {
        Task task = taskService.getTaskById(taskId);

        if(task.getInside_status().equals(InsideTaskStatusEnum.ЗАВЕРШЕНА)||
                task.getInside_status().equals(InsideTaskStatusEnum.ОШИБКА_ВЫПОЛНЕНИЯ)){
            String currentFolder = getTaskUnZipDir(task.getId());
            Path outDir =  Paths.get( getTaskUnZipDir(task.getId())+"Выход/"+"Выход");
            Path outLogs =  Paths.get( getTaskUnZipDir(task.getId())+"Выход/"+"Консольный_вывод.txt");
            Path outPack =  Paths.get( getTaskUnZipDir(task.getId())+"Выход.zip");
            String taskUUID = getUUIDFromFileName(task.getSource_file_name());

            if(!Files.exists(outDir)){
                ArrayList<String> commandResult = BashTools.bashCommand("echo 'q' |  sudo -S " +
                        "docker inspect --format='{{json .Mountpoint}}' "+"vol_"+taskUUID,"");

                if (!commandResult.get(0).isEmpty()) {
                    BashTools.bashCommand("echo 'q' |  sudo -S cp -r "+commandResult.get(0) + "/Выход"+
                            " "+outDir,"");
                }
            }

            if(!Files.exists(outLogs)){

                FileWriter writer = new FileWriter(outLogs.toString(), false);
                writer.write(getConsoleOutput(task.getId())+"\n");
                writer.flush();
                writer.close();
            }

            if(!Files.exists(outPack)){
                zip("Выход",currentFolder);
            }

            return outPack.toString();
        }
        return null;
    }


    private void prepareOutputFilesIfExited() throws Exception {

        ArrayList<Task> exitedTasks = taskService.getTasksByInsideStatus(InsideTaskStatusEnum.ЗАВЕРШЕНА);
        ArrayList<Task> exitedTasksErr = taskService.getTasksByInsideStatus(InsideTaskStatusEnum.ОШИБКА_ВЫПОЛНЕНИЯ);

        exitedTasks.addAll(exitedTasksErr);

        for(Task task: exitedTasks){

            getTaskOutputPack(task.getId());
        }
    }



    private int taskComparator(Task task1, Task task2){
        if(task1.getInside_status().equals(InsideTaskStatusEnum.В_ОЧЕРЕДИ)
                && task2.getInside_status().equals(InsideTaskStatusEnum.В_ОЧЕРЕДИ)){

            if (task1.getUpdated().before(task2.getUpdated()))
                return -1;
            else if (task1.getUpdated().after(task2.getUpdated()))
                return 1;
            return 0;
        }
        else if(task1.getInside_status().equals(InsideTaskStatusEnum.ПРИОСТАНОВЛЕНА)
                && task2.getInside_status().equals(InsideTaskStatusEnum.ПРИОСТАНОВЛЕНА)){
            if (task1.getUpdated().before(task2.getUpdated()))
                return -1;
            else if (task1.getUpdated().after(task2.getUpdated()))
                return 1;
            return 0;
        }

        else if(task1.getInside_status().equals(InsideTaskStatusEnum.В_ОЧЕРЕДИ)
                && task2.getInside_status().equals(InsideTaskStatusEnum.ПРИОСТАНОВЛЕНА)){
            return -1;

        }

        else if(task1.getInside_status().equals(InsideTaskStatusEnum.ПРИОСТАНОВЛЕНА)
                && task2.getInside_status().equals(InsideTaskStatusEnum.В_ОЧЕРЕДИ)){
            return 1;
        }

        return 0;
    }

    /**
     * Удаляет из очереди задачи, помеченных как удаленные, удаляет пустые очереди,
     * сортирует очереди задач пользователей
     */

    private void updateUserQueues() {

        // За время работы задачи могли удалить, поэтому каждый раз приводим в соответсвие с состоянием из БД


        ArrayList<Task> taskQueue = taskService.getTasksByStatus(TaskStatusEnum.УДАЛЕНА);

        if(!UserQueues.isEmpty()){

            try {
                taskQueue.forEach(task -> {
                    if(UserQueues.containsKey(task.getUser_id())&&UserQueues.get(task.getUser_id()).size() != 0)
//                    if (UserQueues.get(task.getUser_id()).size() != 0) {
                        UserQueues.get(task.getUser_id()).removeIf(t -> t.getId() == task.getId());

//                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                int a = 1;
            }
            // Удаление пустых очередей
            Set<Long> emptyQueues = new HashSet<>();

            UserQueues.forEach((User, taskList) -> {
                if (taskList.size() == 0) {
                    emptyQueues.add(User);
                }
            });

            UserQueues.keySet().removeAll(emptyQueues);
        }


        // Добавление новых задач

        taskQueue = taskService.getTasksByInsideStatus(InsideTaskStatusEnum.В_ОЧЕРЕДИ);

//        taskQueue.addAll(taskQueueRunning);

        for (Task task : taskQueue) {
            try {
                if(UserQueues.containsKey(task.getUser_id())){

                    boolean contains = false;
                    for(Task task1:UserQueues.get(task.getUser_id())){
                        if(task1.getId().equals(task.getId())){
                            contains = true;
                            break;
                        }
                    }
                    if (!contains) {
                        UserQueues.get(task.getUser_id()).add(task);
                    }
                }
                else{
                    LinkedList<Task> UserQueue = new LinkedList<>();
                    UserQueue.add(task);
                    UserQueues.put(task.getUser_id(), (LinkedList<Task>) UserQueue.clone());
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.warn(e.getMessage());
            }
        }

        if (!UserQueues.isEmpty()){
            UserQueues.forEach((UserId, taskList) -> {
                taskList.sort(this::taskComparator);
            });
        }

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

        // TODO пустые логи при ошибках в программе, если ошибок нет логи выводятся. Почему не все логи работают?

        Task task = taskService.getTaskById(taskId);

        ArrayList<String> commandResult = BashTools.bashCommand("echo 'q' | sudo -S docker logs " +
                "container_"+getUUIDFromFileName(task.getSource_file_name()),"");

        StringBuilder res = new StringBuilder();
        commandResult.forEach(res::append);

        return res.toString();
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


            ArrayList<String> containersFromImage =
                    BashTools.bashCommand("echo 'q'|sudo -S docker ps -aqf 'ancestor='" + taskUUID, "");

            containersFromImage.forEach(container -> {
                try {
                    BashTools.bashCommand("echo 'q'|sudo -S docker rm " + container, "");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            BashTools.bashCommand("echo 'q'|sudo -S docker rmi " + taskUUID, "");

//             DONE_TODO Удаление контейнеров по задаче

//            taskService.deleteTask(newTaskDto, UserId);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
