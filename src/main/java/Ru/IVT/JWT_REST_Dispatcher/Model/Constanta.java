package Ru.IVT.JWT_REST_Dispatcher.Model;

import lombok.Data;

import javax.persistence.criteria.CriteriaBuilder;

public class Constanta {
    public static final Integer maxLimitTaskAtTokenTime = 4;
    public static final Integer taskWindowLimitInMilliseconds = 3600000;
//    public static final Integer maxUserWindowRequests = 10;
    public static final Integer serverTasksLimit = 5;
}
