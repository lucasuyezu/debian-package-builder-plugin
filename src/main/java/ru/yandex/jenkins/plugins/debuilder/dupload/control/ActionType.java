package ru.yandex.jenkins.plugins.debuilder.dupload.control;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ru.yandex.jenkins.plugins.debuilder.dupload.DuploadException;

/**
 * Stores every action controlled
 * 
 * @author caiocezar
 * 
 */
public class ActionType {
    private Character actionType;
    public static DateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.US);
    private Date executedDate;
    private String destination;

    /**
     * Used when a upload is a success
     */
    public static Character UPLOAD_ACTION = 'u';

    /**
     * Used when the announcement of an upload is a success
     */
    public static Character ANNOUNCE_ACTION = 'a';

    /**
     * Used when all upload and announcements are done, is used for
     * '.changes' files
     */
    public static Character SUCCESS_ACTION = 's';

    public Character getActionType() {
        return actionType;
    }

    /**
     * Set the action using the current date
     * 
     * @param action
     *            A valid action
     * @throws DuploadException
     *             If some data is invalid
     */
    public ActionType(Character action, String destination) throws DuploadException {
        setActionType(action);
        setExecutedDate(new Date());
        setDestination(destination);
    }

    /**
     * Set action and the date that the action happened
     * 
     * @param action
     *            A valid action
     * @param executedDate
     *            A valid date
     * @throws DuploadException
     *             If Some data is invalid
     */
    public ActionType(Character action, String destination, Date executedDate) throws DuploadException {
        setActionType(action);
        setExecutedDate(executedDate);
        setDestination(destination);
    }

    /**
     * Set a valid action type
     * 
     * @param actionType
     *            The action type, always used a valid action
     * @throws DuploadException
     *             If the action is invalid
     */
    public void setActionType(Character actionType) throws DuploadException {
        if (actionType == null
                || (actionType != UPLOAD_ACTION && actionType != ANNOUNCE_ACTION && actionType != SUCCESS_ACTION))
            throw new DuploadException("Invalid action Type");
        this.actionType = actionType;
    }

    public Date getExecutedDate() {
        return executedDate;
    }

    /**
     * Set the date that the action happened
     * 
     * @param dateExecuted
     *            The date
     * @throws DuploadException
     *             If he date is invalid
     */
    public void setExecutedDate(Date executedDate) throws DuploadException {
        if (executedDate == null)
            throw new DuploadException("Empty date");
        this.executedDate = executedDate;
    }

    /**
     * Set the date that the action happened
     * 
     * @param executedDateText
     *            The date text
     * @throws DuploadException
     *             Same as {@link ActionType#setExecutedDate(Date)}
     * @throws ParseException
     *             Same as {@link DateFormat#parse(String)}
     */
    public void setExecutedDate(String executedDateText) throws DuploadException, ParseException {
        Date executedDate = ActionType.DATE_FORMAT.parse(executedDateText);
        setExecutedDate(executedDate);
    }

    public String getDestination() {
        return destination;
    }

    /**
     * Set the destination server
     * 
     * @param destination
     *            A not empty destination server
     * @throws DuploadException
     *             If the destination is invalid
     */
    public void setDestination(String destination) throws DuploadException {
        if (destination == null || destination.isEmpty())
            throw new DuploadException("Empty destination.");
        this.destination = destination;
    }
}