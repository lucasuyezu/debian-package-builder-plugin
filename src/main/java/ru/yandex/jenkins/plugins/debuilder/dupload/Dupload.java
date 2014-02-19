package ru.yandex.jenkins.plugins.debuilder.dupload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

import ru.yandex.jenkins.plugins.debuilder.DebianizingException;
import ru.yandex.jenkins.plugins.debuilder.DebUtils.Runner;
import ru.yandex.jenkins.plugins.debuilder.dpkg.DebianChanges;
import ru.yandex.jenkins.plugins.debuilder.dpkg.common.DebianDistributions;
import ru.yandex.jenkins.plugins.debuilder.dpkg.common.DebianFileEntry;
import ru.yandex.jenkins.plugins.debuilder.dupload.control.ActionType;
import ru.yandex.jenkins.plugins.debuilder.dupload.control.ControlEntry;
import ru.yandex.jenkins.plugins.debuilder.dupload.control.DuploadControl;
import hudson.FilePath;

public class Dupload {

    public static void start(FilePath changelog, FilePath dotChangesBasePath, DuploadUploadMethod uploadMethod,
            String incomingPath, String distributionPattern, Runner runner) throws DuploadException, IOException,
            InterruptedException {
        // TODO parse change log
        // TODO validate architectures type 'one arch', source, multiarch

        Map<String, String> changelogFields = parseChangelog(runner, changelog.getParent().getRemote());

        String dotChangesFilePattern = changelogFields.get("Source") + "_" + changelogFields.get("Version") + "_*.changes";
        boolean found = false;
        for (FilePath dotChangesFile : dotChangesBasePath.list(dotChangesFilePattern)) {
            found = true;
            start(dotChangesFile, uploadMethod, incomingPath, distributionPattern, runner);
        }
        if (!found)
            runner.announce("None .changes file found to publish");
    }

    private static Map<String, String> parseChangelog(Runner runner, String remoteDebian) throws DuploadException {
        String changelogOutput;
        try {
            changelogOutput = runner.runCommandForOutput("cd \"{0}\" && dpkg-parsechangelog -lchangelog", remoteDebian);
        } catch (DebianizingException e) {
            e.printStackTrace();
            throw new DuploadException("Fail parsing changelog.");
        }

        Map<String, String> changelog = new HashMap<String, String>();
        Pattern changelogFormat = Pattern.compile("(\\w+):\\s*(.*)");

        for (String row : changelogOutput.split("\n")) {
            Matcher matcher = changelogFormat.matcher(row);
            if (matcher.matches()) {
                changelog.put(matcher.group(1), matcher.group(2));
            }
        }

        if (changelog.isEmpty())
            throw new DuploadException("Fail parsing changelog. Original input: " + changelogOutput);

        String[] fields = { "Source", "Version", "Distribution", "Urgency", "Maintainer", "Date", "Changes" };

        for (String field : fields) {
            if (changelog.get(field) == null)
                throw new DuploadException("Fail parsing changelog, '" + field + "' not found. Original input: "
                        + changelogOutput);
        }

        return changelog;
    }

    /**
     * Initialize the dupload process
     * 
     * @param uploadMethod
     *            The method to upload the files
     * @param dotChangesFolder
     *            The folder to find for the .changes files
     * @param incomingPath
     *            The destination folder to upload
     * @param distributionPattern
     *            If not null will replace the pattern with the distribution
     *            informed in the .changes file
     * @param runner
     *            Used to log
     * @throws DuploadException
     *             If some goes wrong in the process
     * @throws IOException
     *             For errors with file operations
     * @throws InterruptedException
     *             If file operations where interrupted
     * @throws DebianChangesParseException
     * @throws DebianChangesValidateException
     */
    public static void start(FilePath changesFile, DuploadUploadMethod uploadMethod, String incomingPath,
            String distributionPattern, Runner runner) throws DuploadException, IOException, InterruptedException {

        if (uploadMethod == null || changesFile == null || incomingPath == null || runner == null)
            throw new DuploadException("Dupload failed, invalid parameters");

        if (!changesFile.exists())
            throw new DuploadException("The required '.changes' file (" + changesFile.getName() + ") does not exists");
        else if (changesFile.isDirectory())
            throw new DuploadException("The required '.changes' file (" + changesFile.getName() + ") is a directory");

        runner.announce("Validating '" + changesFile.getRemote() + "'");
        DebianChanges debianChanges = new DebianChanges();

        String error = debianChanges.read(changesFile);

        if (!error.isEmpty())
            throw new DuploadException("Dupload failed validating '" + changesFile.getRemote() + "'\n" + error);

        FilePath dotUploadFile = new FilePath(changesFile.getParent(), changesFile.getBaseName() + ".upload");

        // Remove from upload queue files that has already been uploaded
        ArrayList<FilePath> filesToUpload = getFilesToUpload(dotUploadFile, changesFile, debianChanges, runner);

        if (filesToUpload == null || filesToUpload.isEmpty()) {
            runner.announce("All files has already been uploaded.");
        } else {
            HashMap<FilePath, String> localAndRemoteFilesDestination = generateUploadPaths(filesToUpload, incomingPath,
                    distributionPattern, debianChanges.getDistributions());

            if (!uploadMethod.storeFile(localAndRemoteFilesDestination, runner))
                throw new DuploadException("Dupload failed uploading files");
        }

        try {
            // TODO register uploaded files even though some has failures
            registerControlFile(dotUploadFile, changesFile.getName(), filesToUpload, uploadMethod.getServer());
        } catch (DuploadException e) {
            e.printStackTrace();
            throw new DuploadException("Fail registering de control: " + e.getLocalizedMessage());
        }

        // TODO test if this distribution and architecture is valid
        // TODO search for extra announce files *.announce
    }

    /**
     * The all file names that was not already uploaded
     * 
     * @param uploadFile
     *            The .upload control file
     * @param changesFile
     *            The .changes file
     * @param debianChanges
     *            The debian changes
     * @param runner
     *            The runner to log
     * @return All files to upload
     * @throws IOException
     *             If some file operation fail
     * @throws InterruptedException
     *             If Some file operation is interrupted
     */
    private static ArrayList<FilePath> getFilesToUpload(FilePath uploadFile, FilePath changesFile, DebianChanges debianChanges,
            Runner runner) throws IOException, InterruptedException {

        ArrayList<FilePath> fileUploadQueue = new ArrayList<FilePath>();
        ArrayList<String> filesInControl = new ArrayList<String>();

        // Read the control file
        DuploadControl control = new DuploadControl();
        List<Pair<String, String>> rejected = control.read(uploadFile);

        if (rejected == null) {
            runner.announce("Error reading .upload file");
        } else {
            if (rejected.size() > 0) {
                runner.announce("Some lines from .upload file were rejected");
                for (Pair<String, String> pair : rejected)
                    runner.announce(pair.getLeft() + ": " + pair.getRight());
            }

            // Get all uploaded files in the control
            for (ControlEntry controlEntry : control.getEntries())
                for (ActionType action : controlEntry.getActions())
                    if (action.getActionType() == ActionType.UPLOAD_ACTION)
                        filesInControl.add(controlEntry.getFileName());
        }

        FilePath basePath = changesFile.getParent();
        for (DebianFileEntry changesEntry : debianChanges.getFiles().getFileEntries())
            // If the file was not already uploaded add to upload queue
            if (!filesInControl.contains(changesEntry.getName()))
                fileUploadQueue.add(basePath.child(changesEntry.getName()));

        // If the .changes was not uploaded add to queue
        if (!filesInControl.contains(changesFile.getName()))
            fileUploadQueue.add(changesFile);

        return fileUploadQueue;
    }

    /**
     * Generate the HashMap with the origin and destination files
     * 
     * @param localBasePath
     *            The local base path
     * @param remoteBasePath
     *            The remote base path.
     * @param files
     *            The file name relative to the base paths
     * @param distributionPattern
     *            If not empty
     * @param distributions
     * @return
     */
    private static HashMap<FilePath, String> generateUploadPaths(ArrayList<FilePath> files, String remoteBasePath,
            String distributionPattern, DebianDistributions distributions) {

        remoteBasePath = remoteBasePath == null ? "" : remoteBasePath + "/";

        HashMap<FilePath, String> localAndRemoteFilesDestination = new HashMap<FilePath, String>();

        if (files == null || files.isEmpty())
            return localAndRemoteFilesDestination;

        ArrayList<String> dists = new ArrayList<String>();
        if ((distributionPattern != null && !distributionPattern.isEmpty())
                && (distributions != null && distributions.size() > 0)) {
            for (String dist : distributions.getCopy())
                if (!dist.isEmpty())
                    dists.add(dist);
        } else {
            // Dummy distribution
            dists.add("");
        }

        for (String dist : dists) {
            String remotePath;
            if (!dist.isEmpty())
                remotePath = remoteBasePath.replace(distributionPattern, dist);
            else
                remotePath = remoteBasePath;

            for (FilePath file : files)
                localAndRemoteFilesDestination.put(file, remotePath + file.getName());

        }

        return localAndRemoteFilesDestination;
    }

    /**
     * Register the uploaded files and the success of the process
     * 
     * @param dotUploadFile
     *            The .upload file
     * @param debianChangesFileName
     *            The .changes file name. This name is used to register the
     *            process success
     * @param filesUploaded
     *            The files uploaded, or null/empty if no file was uploaded
     * @param destinationServer
     *            The destination server
     * @throws DuploadException
     *             If some problem create the control fail
     */
    private static void registerControlFile(FilePath dotUploadFile, String debianChangesFileName,
            ArrayList<FilePath> filesUploaded, String destinationServer) throws DuploadException {
        Date now = new Date();

        DuploadControl control = new DuploadControl();
        try {
            if (filesUploaded != null && !filesUploaded.isEmpty()) {
                for (FilePath changeFileEntry : filesUploaded) {

                    ControlEntry fileEntry;

                    fileEntry = new ControlEntry(changeFileEntry.getName(), new ActionType(ActionType.UPLOAD_ACTION,
                            destinationServer, now));

                    control.mergeEntry(fileEntry);
                }
            }

            ControlEntry changesFileEntry = new ControlEntry(debianChangesFileName, new ActionType(ActionType.SUCCESS_ACTION,
                    destinationServer, now));
            control.mergeEntry(changesFileEntry);

            DuploadControl.addControlLines(dotUploadFile, control);
        } catch (DuploadException e) {
            e.printStackTrace();
            throw new DuploadException("Error create a control object: " + e.getLocalizedMessage());
        }
    }
}
