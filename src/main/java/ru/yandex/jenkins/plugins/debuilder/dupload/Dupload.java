package ru.yandex.jenkins.plugins.debuilder.dupload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import ru.yandex.jenkins.plugins.debuilder.DebUtils.Runner;
import ru.yandex.jenkins.plugins.debuilder.dpkg.DebianChanges;
import ru.yandex.jenkins.plugins.debuilder.dpkg.DpkgException;
import ru.yandex.jenkins.plugins.debuilder.dupload.DuploadControl.ActionType;
import ru.yandex.jenkins.plugins.debuilder.dupload.DuploadControl.ControlEntry;
import hudson.FilePath;

public class Dupload {

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
     * @throws DpkgException
     */
    public static void start(FilePath dotChangesFolder, DuploadUploadMethod uploadMethod, String incomingPath,
            String distributionPattern, Runner runner) throws DuploadException, IOException, InterruptedException, DpkgException {

        if (uploadMethod == null || dotChangesFolder == null || incomingPath == null || runner == null)
            throw new DuploadException("Dupload failed, invalid parameters");

        FilePath[] dotChangeFiles = dotChangesFolder.list("*.changes");

        if (dotChangeFiles.length <= 0)
            throw new DuploadException("None '.changes' file found!");

        for (FilePath dotChangeFile : dotChangeFiles) {
            if (dotChangeFile.isDirectory())
                continue;

            runner.announce("Processing '" + dotChangeFile.getRemote() + "'");
            DebianChanges debianChanges = new DebianChanges();

            if (!debianChanges.readDotChangesFile(dotChangeFile, runner))
                throw new DuploadException("Dupload failed validating '" + dotChangeFile.getRemote() + "'");

            FilePath dotUploadFile = new FilePath(dotChangeFile.getParent(), dotChangeFile.getBaseName() + ".upload");

            // Remove from upload queue files that has already been uploaded
            ArrayList<String> filesToUpload = getFilesToUpload(dotUploadFile, dotChangeFile, debianChanges, runner);

            if (filesToUpload == null || filesToUpload.isEmpty()) {
                runner.announce("All files has already been uploaded.");
            } else {
                HashMap<String, String> localAndRemoteFilesDestination = generateUploadPaths(dotChangeFile.getParent()
                        .getRemote(), incomingPath, filesToUpload, distributionPattern, debianChanges.getDistributions());

                if (!uploadMethod.storeFile(localAndRemoteFilesDestination, runner))
                    throw new DuploadException("Dupload failed uploading files");
            }

            try {
                // TODO register uploaded files even though some has failures
                registerControlFile(dotUploadFile, dotChangeFile.getName(), filesToUpload, uploadMethod.getServer());
            } catch (DuploadException e) {
                e.printStackTrace();
                throw new DuploadException("Fail registering de control: " + e.getLocalizedMessage());
            }

        }
        // TODO test if this distribution is valid
        // TODO search for extra announce files *.announce
    }

    /**
     * The all file names that was not already uploaded
     * 
     * @param dotUploadFile
     *            The .upload control file
     * @param dotChangesFile
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
     * @throws DuploadException
     */
    private static ArrayList<String> getFilesToUpload(FilePath dotUploadFile, FilePath dotChangesFile,
            DebianChanges debianChanges, Runner runner) throws IOException, InterruptedException, DuploadException {

        ArrayList<String> fileUploadQueue = new ArrayList<String>();
        ArrayList<String> filesInControl = new ArrayList<String>();

        if (dotUploadFile != null && dotUploadFile.exists() && !dotUploadFile.isDirectory()) {
            // Read the control file
            DuploadControl control = new DuploadControl(dotUploadFile, runner);

            // Get all uploaded files in the control
            for (DuploadControl.ControlEntry fileControlEntry : control.getEntries()) {
                for (DuploadControl.ActionType action : fileControlEntry.getActions()) {
                    if (action.getActionType() == DuploadControl.ActionType.UPLOAD_ACTION) {
                        filesInControl.add(fileControlEntry.getFileName());
                        continue;
                    }
                }
            }

        }

        for (String changesFileEntry : debianChanges.getFiles().keySet())
            // If the file was not already uploaded add to upload queue
            if (!filesInControl.contains(changesFileEntry))
                fileUploadQueue.add(changesFileEntry);

        // If the .changes was not uploaded add to queue
        if (!filesInControl.contains(dotChangesFile.getName()))
            fileUploadQueue.add(dotChangesFile.getName());

        return fileUploadQueue;
    }

    /**
     * Generate the HashMap with the origin and destination files
     * 
     * @param localBasePath
     *            The local base path
     * @param remoteBasePath
     *            The remote base path.
     * @param fileNames
     *            The file name relative to the base paths
     * @param distributionPattern
     *            If not empty
     * @param distributions
     * @return
     */
    private static HashMap<String, String> generateUploadPaths(String localBasePath, String remoteBasePath,
            ArrayList<String> fileNames, String distributionPattern, String[] distributions) {

        localBasePath = localBasePath == null ? "" : localBasePath + "/";
        remoteBasePath = remoteBasePath == null ? "" : remoteBasePath + "/";

        HashMap<String, String> localAndRemoteFilesDestination = new HashMap<String, String>();

        if (fileNames == null || fileNames.isEmpty())
            return localAndRemoteFilesDestination;

        ArrayList<String> dists = new ArrayList<String>();
        if ((distributionPattern != null && !distributionPattern.isEmpty())
                && (distributions != null && distributions.length > 0)) {
            for (String dist : distributions)
                if (!dist.isEmpty())
                    dists.add(dist);
        } else {
            // Dummy dist
            dists.add("");
        }

        for (String dist : dists) {
            String remotePath;
            if (!dist.isEmpty())
                remotePath = remoteBasePath.replace(distributionPattern, dist);
            else
                remotePath = remoteBasePath;

            for (String file : fileNames)
                localAndRemoteFilesDestination.put(localBasePath + file, remotePath + file);

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
            ArrayList<String> filesUploaded, String destinationServer) throws DuploadException {
        Date now = new Date();

        DuploadControl control = new DuploadControl();
        try {
            if (filesUploaded != null && !filesUploaded.isEmpty()) {
                for (String changeFileEntry : filesUploaded) {

                    ControlEntry fileEntry;

                    fileEntry = new ControlEntry(changeFileEntry, new ActionType(DuploadControl.ActionType.UPLOAD_ACTION,
                            destinationServer, now));

                    control.mergeEntry(fileEntry);
                }
            }

            ControlEntry changesFileEntry = new ControlEntry(debianChangesFileName, new ActionType(
                    DuploadControl.ActionType.SUCCESS_ACTION, destinationServer, now));
            control.mergeEntry(changesFileEntry);

            DuploadControl.addControlLines(dotUploadFile, control);
        } catch (DuploadException e) {
            e.printStackTrace();
            throw new DuploadException("Error create a control object: " + e.getLocalizedMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new DuploadException("Error writing control: " + e.getLocalizedMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new DuploadException("Error writing control: " + e.getLocalizedMessage());
        }
    }
}
