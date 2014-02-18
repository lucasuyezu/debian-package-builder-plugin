package ru.yandex.jenkins.plugins.debuilder.dupload.control;

import java.util.ArrayList;

import ru.yandex.jenkins.plugins.debuilder.dupload.DuploadException;

/**
 * Stores data from every control entry
 * 
 * @author caiocezar
 * 
 */
public class ControlEntry {
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