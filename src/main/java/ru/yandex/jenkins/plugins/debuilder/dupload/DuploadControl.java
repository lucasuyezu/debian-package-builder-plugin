package ru.yandex.jenkins.plugins.debuilder.dupload;

import hudson.FilePath;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.ListIterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yandex.jenkins.plugins.debuilder.DebUtils.Runner;

/**
 * Stores all data from '.upload' files data. This files control the upload,
 * announcements and others actions
 * 
 * @author caiocezar
 * 
 */
public class DuploadControl {
    private ArrayList<ControlEntry> entries;

    public DuploadControl() {

    }

    /**
     * Create a control from o .upload file
     * 
     * @param dotUploadFile
     *            The file
     * @param runner
     *            The runner to log
     * @throws IOException
     *             Same as
     *             {@link DuploadControl#readDotUploadFile(FilePath, Runner)}
     * @throws InterruptedException
     *             Same as
     *             {@link DuploadControl#readDotUploadFile(FilePath, Runner)}
     * @throws DuploadException
     *             Same as
     *             {@link DuploadControl#readDotUploadFile(FilePath, Runner)}
     */
    public DuploadControl(FilePath dotUploadFile, Runner runner) throws IOException, InterruptedException, DuploadException {
        readDotUploadFile(dotUploadFile, runner);
    }

    /**
     * Stores data from every control entry
     * 
     * @author caiocezar
     * 
     */
    public static class ControlEntry {
        private String fileName;
        private ArrayList<ActionType> actions;

        /**
         * Create an empty entry
         */
        public ControlEntry() {
        }

        /**
         * Create a new control entry with an action
         * 
         * @param fileName
         *            The file name
         * @param action
         *            The action
         * @throws DuploadException
         *             Same as {@link ControlEntry#setFileName(String)}
         */
        public ControlEntry(String fileName, ActionType action) throws DuploadException {
            setFileName(fileName);
            addAction(action);
        }

        public String getFileName() {
            if (fileName == null)
                fileName = "";
            return fileName;
        }

        /**
         * Set the file name
         * 
         * @param fileName
         *            A not null and not empty name
         * @throws DuploadException
         *             If the name is invalid
         */
        public void setFileName(String fileName) throws DuploadException {
            if (fileName == null || fileName == "")
                throw new DuploadException("Not a valid file name");
            this.fileName = fileName;
        }

        /**
         * @return the actions
         */
        public ArrayList<ActionType> getActions() {
            if (actions == null)
                actions = new ArrayList<ActionType>();
            return actions;
        }

        /**
         * Add a action that happened to the file
         * 
         * @param action
         *            the action to set
         */
        public void addAction(ActionType action) {
            if (action == null)
                return;
            getActions().add(action);
        }
    }

    /**
     * Stores every action controlled
     * 
     * @author caiocezar
     * 
     */
    public static class ActionType {
        private Character actionType;
        public static DateFormat executedDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.US);
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
            Date executedDate = ActionType.executedDateFormat.parse(executedDateText);
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

    /**
     * @return the entries
     */
    public ArrayList<ControlEntry> getEntries() {
        if (entries == null)
            entries = new ArrayList<ControlEntry>();
        return entries;
    }

    /**
     * @param entry
     *            the entry to add
     */
    public void mergeEntry(ControlEntry entry) {

        boolean found = false;
        for (ControlEntry e : getEntries()) {
            if (e.getFileName().equals(entry.getFileName())) {
                found = true;
                for (ActionType action : entry.getActions())
                    e.addAction(action);
            }
        }
        if (!found)
            getEntries().add(entry);
    }

    /**
     * Read a .upload file and populate the Control object
     * 
     * @param dotUploadFile
     *            The file to read
     * @param runner
     *            The runner to log
     * @throws IOException
     *             If some file operation fail
     * @throws InterruptedException
     *             If some file operation is interrupted
     * @throws DuploadException
     *             If some parameter is invalid
     * 
     */
    public void readDotUploadFile(FilePath dotUploadFile, Runner runner) throws IOException, InterruptedException,
            DuploadException {

        String msgPrefix = "Parsing .upload file: ";

        Pattern dotUploadPattern = Pattern.compile("^([a-z])\\s+([^\\s]+)\\s+([^\\s]+)\\s+(.+)$");

        if (dotUploadFile == null || !dotUploadFile.exists() || dotUploadFile.isDirectory())
            throw new DuploadException("The file is invalid");

        ArrayList<String> dotUploadLines = new ArrayList<String>(Arrays.asList(dotUploadFile.readToString().split("\n")));

        ListIterator<String> dotChangesLinesIterator = dotUploadLines.listIterator();

        while (dotChangesLinesIterator.hasNext()) {
            String row = dotChangesLinesIterator.next();

            Matcher dotUploadMatch = dotUploadPattern.matcher(row);

            if (dotUploadMatch.matches()) {
                Character actionType = dotUploadMatch.group(1).charAt(0);
                String fileName = dotUploadMatch.group(2);
                String destination = dotUploadMatch.group(3);
                String dateText = dotUploadMatch.group(4);
                Date executedDate;

                try {
                    executedDate = ActionType.executedDateFormat.parse(dateText);
                } catch (ParseException e) {
                    executedDate = new Date();
                    runner.announce("{0} Invalid date format({1}), using the current date.", msgPrefix, e.getLocalizedMessage());
                    e.printStackTrace();
                }

                try {
                    mergeEntry(new ControlEntry(fileName, new ActionType(actionType, destination, executedDate)));
                } catch (DuploadException e) {
                    runner.announce("{0} Invalid upload line: {1}. Mensage error: {2}", msgPrefix, row, e.getLocalizedMessage());
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    /**
     * Add all actions from a entry to the .upload file
     * 
     * @param dotUploadFile
     *            The destination file
     * @param entry
     *            The entry
     * @throws IOException
     *             If some file operation fail
     * @throws InterruptedException
     *             If some file operation is interrupted
     * @throws DuploadException
     *             If some parameter is invalid
     * 
     */
    public static void addControlLines(FilePath dotUploadFile, ControlEntry entry) throws IOException, DuploadException,
            InterruptedException {

        if (dotUploadFile == null)
            throw new DuploadException("The .upload file not informed.");

        if (dotUploadFile.isDirectory())
            throw new DuploadException("The .upload (" + dotUploadFile.getName() + ") is a directory.");

        if (entry == null)
            throw new DuploadException("The provided entry is empty");

        if (entry.getActions().size() <= 0)
            throw new DuploadException("The provided entry has no actions");

        String lines = "";
        for (ActionType action : entry.getActions()) {
            lines += action.getActionType() + " " + entry.getFileName() + " " + action.getDestination() + " "
                    + ActionType.executedDateFormat.format(action.getExecutedDate()) + "\n";
        }

        String content = "";
        if (dotUploadFile.exists())
            content = dotUploadFile.readToString();

        if (!content.isEmpty() && !content.endsWith("\n"))
            content += "\n";
        content += lines;

        dotUploadFile.write(content, null);
    }

    /**
     * Add all actions of all entries to a .upload file
     * 
     * @param dotUploadFile
     *            The control File
     * @param control
     *            The controlObject
     * @throws IOException
     *             Same as
     *             {@link DuploadControl#addControlLines(FilePath, ControlEntry)}
     * @throws DuploadException
     *             Same as
     *             {@link DuploadControl#addControlLines(FilePath, ControlEntry)}
     * @throws InterruptedException
     *             Same as
     *             {@link DuploadControl#addControlLines(FilePath, ControlEntry)}
     */
    public static void addControlLines(FilePath dotUploadFile, DuploadControl control) throws IOException, DuploadException,
            InterruptedException {
        for (ControlEntry entry : control.getEntries()) {
            addControlLines(dotUploadFile, entry);
        }
    }
}
