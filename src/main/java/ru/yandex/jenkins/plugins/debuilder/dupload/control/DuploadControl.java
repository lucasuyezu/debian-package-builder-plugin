package ru.yandex.jenkins.plugins.debuilder.dupload.control;

import hudson.FilePath;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import ru.yandex.jenkins.plugins.debuilder.dupload.DuploadException;

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
     * Read a file and use {@link #parse(String)} to parse the content
     * 
     * @param file
     *            The .upload file
     * @return The same as {@link #parse(String)} or an empty list if the file
     *         not exists or null if there is same problem reading the file
     */
    public List<Pair<String, String>> read(FilePath file) {
        if (file == null)
            return null;

        try {
            if (file.exists() && !file.isDirectory())
                return parse(file.readToString());
            else
                return new ArrayList<Pair<String, String>>();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Read the content o a .upload file and populate the Control Object
     * 
     * @param file
     *            The file to read
     * 
     * @return A {@link List} with a {@link Pair} that holds the message and the
     *         rejected line, if all lines are valid the List is empty. Never
     *         return null;
     */
    public List<Pair<String, String>> parse(String content) {

        if (content == null)
            content = "";

        List<Pair<String, String>> rejected = new ArrayList<Pair<String, String>>();

        ArrayList<String> lines = new ArrayList<String>(Arrays.asList(content.split("\n")));
        ListIterator<String> iterator = lines.listIterator();

        Pattern pattern = Pattern.compile("^([a-z])\\s+([^\\s]+)\\s+([^\\s]+)\\s+(.+)$");

        while (iterator.hasNext()) {
            String row = iterator.next();

            Matcher match = pattern.matcher(row);

            if (match.matches()) {
                Character actionType = match.group(1).charAt(0);
                String fileName = match.group(2);
                String destination = match.group(3);
                String dateText = match.group(4);
                Date executedDate;

                try {
                    executedDate = ActionType.DATE_FORMAT.parse(dateText);
                } catch (ParseException e) {
                    e.printStackTrace();
                    rejected.add(new ImmutablePair<String, String>("Invalid date format", row));
                    continue;
                }

                try {
                    mergeEntry(new ControlEntry(fileName, new ActionType(actionType, destination, executedDate)));
                } catch (DuploadException e) {
                    e.printStackTrace();
                    rejected.add(new ImmutablePair<String, String>("Invalid upload line (" + e.getLocalizedMessage() + ")", row));
                    continue;
                }
            } else {
                rejected.add(new ImmutablePair<String, String>("Invalid line format", row));
            }
        }

        return rejected;
    }

    /**
     * Add all actions from a entry to the .upload file
     * 
     * @param file
     *            The destination file
     * @param entry
     *            The entry
     * @return An empty string if the line is added, the error message if not.
     * 
     */
    public static String addControlLines(FilePath file, ControlEntry entry) {

        if (file == null)
            return "The .upload file is null.";

        try {
            if (file.isDirectory())
                return ("The .upload (" + file.getName() + ") is a directory.");
        } catch (IOException e) {
            e.printStackTrace();
            return ("Error validating the file: " + e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
            return ("Error validating the file: " + e.getMessage());
        }

        if (entry == null)
            return ("The provided entry is empty");

        if (entry.getActions().size() <= 0)
            return ("The provided entry has no actions");

        String lines = "";
        for (ActionType action : entry.getActions()) {
            lines += action.getActionType() + " " + entry.getFileName() + " " + action.getDestination() + " "
                    + ActionType.DATE_FORMAT.format(action.getExecutedDate()) + "\n";
        }

        String content = "";
        try {
            if (file.exists())
                content = file.readToString();

            if (!content.isEmpty() && !content.endsWith("\n"))
                content += "\n";
            content += lines;

            file.write(content, null);
        } catch (IOException e) {
            e.printStackTrace();
            return "Error writing the entry: " + e.getMessage();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return "Error writing the entry: " + e.getMessage();
        }

        return "";
    }

    /**
     * Add all actions of all entries to a .upload file
     * 
     * @param dotUploadFile
     *            The control File
     * @param control
     *            The controlObject
     * @return The error message or the returned by
     *         {@link #addControlLines(FilePath, ControlEntry)}
     */
    public static String addControlLines(FilePath dotUploadFile, DuploadControl control) {
        if (control == null || control.getEntries() == null || control.getEntries().size() <= 0)
            return "The control has no entries.";

        for (ControlEntry entry : control.getEntries()) {
            String error = addControlLines(dotUploadFile, entry);
            if (!error.isEmpty())
                return error;
        }

        return "";
    }
}
