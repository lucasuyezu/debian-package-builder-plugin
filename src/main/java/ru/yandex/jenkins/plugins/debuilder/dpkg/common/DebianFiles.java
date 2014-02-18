package ru.yandex.jenkins.plugins.debuilder.dpkg.common;

import java.util.ArrayList;

public class DebianFiles {
    private ArrayList<DebianFileEntry> fileEntries;

    /**
     * @return the files
     */
    public ArrayList<DebianFileEntry> getFileEntries() {
        return fileEntries;
    }

    /**
     * @param files
     *            the files to set
     */
    public void setFileEntries(ArrayList<DebianFileEntry> files) {
        this.fileEntries = files;
    }

    /**
     * Add a file to the entries
     * 
     * @param file
     *            The file
     */
    public void addFile(DebianFileEntry file) {
        if (getFileEntries() == null)
            setFileEntries(new ArrayList<DebianFileEntry>());
        getFileEntries().add(file);
    }

    /**
     * If there is an entry with the same name all non empty fields are
     * imported, if the name is not present add the new entry.
     * 
     * @param fileEntry
     *            The file entry
     */
    public void mergeEntryByName(DebianFileEntry fileEntry) {

        if (getFileEntries() == null)
            addFile(fileEntry);

        for (DebianFileEntry entry : getFileEntries()) {
            if (entry.getName().equals(fileEntry.getName())) {
                entry.mergeByName(fileEntry);
                return;
            }
        }
        addFile(fileEntry);
    }
}
