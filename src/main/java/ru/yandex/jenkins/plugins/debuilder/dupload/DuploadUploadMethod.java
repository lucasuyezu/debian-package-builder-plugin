package ru.yandex.jenkins.plugins.debuilder.dupload;

import java.util.Map;

import ru.yandex.jenkins.plugins.debuilder.DebUtils.Runner;

/**
 * Every upload method should implement this interface
 * 
 * @author caiocezar
 * 
 */
public interface DuploadUploadMethod {
    /**
     * Should store all files in the remote server and return the result
     * 
     * @param files
     *            Every key is a local file and the value is the path to store
     *            in the server
     * @param runner
     *            Used to log
     * @return Success (true) or fail (false)
     */
    public Boolean storeFile(Map<String, String> files, Runner runner);

    /**
     * As {@link DuploadUploadMethod#storeFile(Map, Runner)} but for only one
     * file
     * 
     * @param local
     *            The local file
     * @param remote
     *            The remote destination path
     * @param runner
     *            To log
     * @return Success or fail
     */
    public Boolean storeFile(String local, String remote, Runner runner);

    /**
     * Should return the server url
     * 
     * @return The server url
     */
    public String getServer();

}
