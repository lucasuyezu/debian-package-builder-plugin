package ru.yandex.jenkins.plugins.debuilder;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Cause.UserIdCause;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.model.Run;
import hudson.plugins.git.GitSCM;
import hudson.scm.SubversionHack;
import hudson.scm.SvnClientManager;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.scm.SCM;
import hudson.scm.SubversionSCM;
import hudson.scm.SubversionSCM.ModuleLocation;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.DescribableList;
import hudson.util.VariableResolver;

import java.io.IOException;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jedi.functional.FunctionalPrimitives;
import jedi.functional.Functor;
import net.sf.json.JSONObject;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;

import ru.yandex.jenkins.plugins.debuilder.DebUtils.Runner;
import ru.yandex.jenkins.plugins.debuilder.dpkg.common.DebianVersion;

public class DebianPackageBuilder extends Builder {
    public static final String DEBIAN_SOURCE_PACKAGE = "DEBIAN_SOURCE_PACKAGE";
    public static final String DEBIAN_PACKAGE_VERSION = "DEBIAN_PACKAGE_VERSION";
    public static final String ABORT_MESSAGE = "[{0}] Aborting: {1} ";
    private static final String PREFIX = "debian-package-builder";

    // location of debian catalog relative to the workspace root
    private final String pathToDebian;
    private final boolean generateChangelog;
    private final boolean buildEvenWhenThereAreNoChanges;
    private final boolean useTagAndBuild;
    private final String distribution;
    private final String buildCommand;
    private final String dchDist;

    @DataBoundConstructor
    public DebianPackageBuilder(String pathToDebian, Boolean generateChangelog, Boolean buildEvenWhenThereAreNoChanges,
            Boolean useTagAndBuild, String distribution, String buildCommand, String dchDist) {
        this.pathToDebian = pathToDebian;
        this.generateChangelog = generateChangelog;
        this.buildEvenWhenThereAreNoChanges = buildEvenWhenThereAreNoChanges;
        this.useTagAndBuild = useTagAndBuild;
        this.distribution = distribution;
        this.buildCommand = buildCommand;
        this.dchDist = dchDist;
    }

    @Override
    public boolean perform(@SuppressWarnings("rawtypes") AbstractBuild build, Launcher launcher, BuildListener listener) {
        PrintStream logger = listener.getLogger();

        FilePath workspace = build.getWorkspace();
        String remoteDebian = getRemoteDebian(workspace);

        Runner runner = new DebUtils.Runner(build, launcher, listener, PREFIX);

        try {
            if (!getDescriptor().isDontInstallTools()) {
                runner.runCommand("sudo apt-get update");
                runner.runCommand("sudo apt-get install aptitude pbuilder");
            }

            importKeys(workspace, runner);

            FilePath changelogFile = workspace.child(remoteDebian).child("changelog");
            if (!changelogFile.exists()) {
                createChangelog(runner, remoteDebian, getSourceName(changelogFile.getParent().child("control")));
            }

            Map<String, String> changelog = parseChangelog(runner, remoteDebian);

            String source = changelog.get("Source");
            String latestVersion = changelog.get("Version");
            runner.announce("Determined latest version to be {0}", latestVersion);

            if (generateChangelog) {
                Pair<VersionHelper, List<Change>> changes = generateChangelog(latestVersion, useTagAndBuild, runner, build,
                        launcher, listener, remoteDebian);

                if (isTriggeredAutomatically(build) && changes.getRight().isEmpty() && !buildEvenWhenThereAreNoChanges) {
                    runner.announce("There are no creditable changes for this build - not building package.");
                    return true;
                }

                latestVersion = changes.getLeft().toString();
                writeChangelog(build, listener, remoteDebian, runner, changes, distribution);
            }

            if (buildCommand != null && !buildCommand.isEmpty()) {
                runner.announce("Using user build command");
                if (!runner.runCommandForResult(buildCommand, true))
                    throw new DebianizingException("Fail executing user build command");

            } else {
                if (!getDescriptor().isIgnoreDeps())
                    runner.runCommand("cd ''{0}'' && sudo /usr/lib/pbuilder/pbuilder-satisfydepends --control control",
                            remoteDebian);

                runner.runCommand(
                        "cd ''{0}'' && debuild --check-dirname-level 0 --no-tgz-check -k{1} -p''gpg --no-tty --passphrase {2}''",
                        remoteDebian, getDescriptor().getAccountName(), getDescriptor().getPassphrase());
            }

            archiveArtifacts(build, launcher, listener, runner, latestVersion);

            // TODO add the source name of the package to use as a key when
            // getting versions
            build.addAction(new DebianBadge(latestVersion, remoteDebian));
            EnvVars envVars = new EnvVars(DEBIAN_SOURCE_PACKAGE, source, DEBIAN_PACKAGE_VERSION, latestVersion);
            build.getEnvironments().add(Environment.create(envVars));
        } catch (InterruptedException e) {
            logger.println(MessageFormat.format(ABORT_MESSAGE, PREFIX, e.getMessage()));
            return false;
        } catch (DebianizingException e) {
            logger.println(MessageFormat.format(ABORT_MESSAGE, PREFIX, e.getMessage()));
            return false;
        } catch (IOException e) {
            logger.println(MessageFormat.format(ABORT_MESSAGE, PREFIX, e.getMessage()));
            return false;
        }

        return true;
    }

    /**
     * Get the source from a control file
     * 
     * @param controlFile
     * @return The source name or null
     */
    private String getSourceName(FilePath controlFile) {
        try {
            for (String line : controlFile.readToString().split("\n"))
                if (line.matches("(?i)^Package:\\s.*"))
                    return line.replaceAll("(?i)^Package:\\s+", "").trim();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("rawtypes")
    private void archiveArtifacts(AbstractBuild build, Launcher launcher, BuildListener listener, Runner runner,
            String latestVersion) throws IOException, InterruptedException {
        FilePath path = build.getWorkspace().child(pathToDebian);
        if (path.getName().equals("debian"))
            path = path.getParent().getParent();
        else
            path = path.getParent();

        List<String> masks = Arrays.asList("*_" + latestVersion + "_*", "*_" + latestVersion + ".tar.*", "*_" + latestVersion
                + ".dsc");

        for (String mask : masks) {
            for (FilePath file : path.list(mask)) {
                runner.announce("Archiving file <{0}> as a build artifact", file.getName());
            }
            path.copyRecursiveTo(mask, new FilePath(build.getArtifactsDir()));
        }
    }

    /**
     * Return the path to the 'debian' directory, where the control build files
     * like changelog and rules are stored.
     * 
     * @param workspace
     *            The root of the build workspace
     * @return The absolute path to the 'debian' dir
     */
    public String getRemoteDebian(FilePath workspace) {
        if (pathToDebian.endsWith("debian") || pathToDebian.endsWith("debian/")) {
            return workspace.child(pathToDebian).getRemote();
        } else {
            return workspace.child(pathToDebian).child("debian").getRemote();
        }
    }

    /**
     * Parses changelog from some SCM
     * 
     * @param latestVersion
     * @param runner
     *            Runnner to log the info
     * @param build
     * @param listener
     * @param remoteDebian
     * @param launcher
     * @return
     * 
     * @throws DebianizingException
     * @throws InterruptedException
     * @throws IOException
     */
    @SuppressWarnings({ "rawtypes" })
    private Pair<VersionHelper, List<Change>> generateChangelog(String latestVersion, Boolean useTagAndBuild, Runner runner,
            AbstractBuild build, Launcher launcher, BuildListener listener, String remoteDebian) throws DebianizingException,
            InterruptedException, IOException {

        SCM scm = build.getProject().getScm();

        String choosedVersion = latestVersion;

        // TODO make the set version a plugable interface
        if (useTagAndBuild) {
            if (scm instanceof GitSCM) {

                GitTagsHelper gitTagsHelper = new GitTagsHelper(build, (GitSCM) scm, runner);

                Pair<Boolean, Set<String>> result = build.getWorkspace().act(gitTagsHelper);

                if (!result.getLeft()) {
                    runner.announce("Fail getting tags from repo, using version from changelog");
                } else {
                    Set<String> tags = result.getRight();

                    String biggestTag = null;
                    DebianVersion v = new DebianVersion();
                    for (String tag : tags) {

                        if (!tag.startsWith("v"))
                            continue;
                        tag = tag.substring(1);

                        if (biggestTag == null) {
                            if (v.parseVersion(tag))
                                biggestTag = tag;
                        } else if (DebianVersion.versionTextCompare(biggestTag, tag) < 0)
                            biggestTag = tag;

                    }

                    if (biggestTag != null) {
                        runner.announce("The biggest tag found: " + biggestTag);

                        if (DebianVersion.versionTextCompare(choosedVersion, biggestTag) < 0)
                            choosedVersion = biggestTag;

                    }
                }

                builds: for (AbstractBuild prevBuild = build.getPreviousBuild(); prevBuild != null; prevBuild = prevBuild
                        .getPreviousBuild()) {

                    for (Object action : prevBuild.getBadgeActions()) {
                        if (action instanceof DebianBadge) {
                            DebianBadge badge = (DebianBadge) action;
                            if (badge.getModule().equals(remoteDebian)) {
                                if (DebianVersion.versionTextCompare(choosedVersion, badge.getVersion()) < 0)
                                    choosedVersion = badge.getVersion();
                                break builds;
                            }
                        }
                    }

                }

            }

            if (!choosedVersion.matches(".*\\+build\\.[0-9]+$"))
                choosedVersion += "+build.0";
        }

        VersionHelper versionHelper = new VersionHelper(choosedVersion);

        runner.announce("Determined latest revision to be {0}", versionHelper.getRevision());

        versionHelper.setMinorVersion(versionHelper.getMinorVersion() + 1);
        String oldRevision = versionHelper.getRevision();

        List<Change> changes;

        if (scm instanceof SubversionSCM) {
            versionHelper.setRevision(getRevision(build, (SubversionSCM) scm, runner, remoteDebian, listener));
            if ("".equals(oldRevision)) {
                runner.announce("No last revision known, using changes since last successful build to populate debian/changelog");
                changes = getChangesSinceLastBuild(runner, build);
            } else {
                runner.announce("Calculating changes since revision {0}.", oldRevision);
                String ourMessage = DebianPackagePublisher.getUsedCommitMessage(build);
                changes = getChangesFromSCM(runner, (SubversionSCM) scm, build, remoteDebian, oldRevision,
                        versionHelper.getRevision(), ourMessage);
            }
        } else {
            runner.announce("SCM in use is not Subversion (but <{0}> instead), defaulting to changes since last build", scm
                    .getClass().getName());
            changes = getChangesSinceLastBuild(runner, build);
        }

        return new ImmutablePair<VersionHelper, List<Change>>(versionHelper, changes);
    }

    /**
     * Writes down changelog contained in <b>changes</b>
     * 
     * @param build
     * @param listener
     * @param remoteDebian
     * @param runner
     * @param changes
     * @throws IOException
     * @throws InterruptedException
     * @throws DebianizingException
     */
    @SuppressWarnings("rawtypes")
    private void writeChangelog(AbstractBuild build, BuildListener listener, String remoteDebian, Runner runner,
            Pair<VersionHelper, List<Change>> changes, String distribution) throws IOException, InterruptedException,
            DebianizingException {

        String versionMessage = getCausedMessage(build);

        String newVersionMessage = Util.replaceMacro(versionMessage,
                new VariableResolver.ByMap<String>(build.getEnvironment(listener)));
        startVersion(runner, remoteDebian, changes.getLeft(), newVersionMessage, distribution);

        for (Change change : changes.getRight()) {
            addChange(runner, remoteDebian, change);
        }

        releaseVersion(runner, remoteDebian);
    }

    @SuppressWarnings("rawtypes")
    private boolean isTriggeredAutomatically(AbstractBuild build) {
        for (Object cause : build.getCauses()) {
            if (cause instanceof UserIdCause) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns message based on causes of the build
     * 
     * @param build
     * @return
     */
    @SuppressWarnings("rawtypes")
    private String getCausedMessage(AbstractBuild build) {
        String firstPart = "Build #${BUILD_NUMBER}. ";

        @SuppressWarnings("unchecked")
        List<Cause> causes = build.getCauses();

        List<String> causeMessages = FunctionalPrimitives.map(causes, new Functor<Cause, String>() {

            @Override
            public String execute(Cause value) {
                return value.getShortDescription();
            }
        });

        Set<String> uniqueCauses = new HashSet<String>(causeMessages);

        return firstPart + FunctionalPrimitives.join(uniqueCauses, ". ") + ".";

    }

    private String getRevision(@SuppressWarnings("rawtypes") AbstractBuild build, SubversionSCM scm, Runner runner,
            String remoteDebian, TaskListener listener) throws DebianizingException {
        ModuleLocation location = findOurLocation(build, scm, runner, remoteDebian);
        try {
            Map<String, Long> revisionsForBuild = SubversionHack.getRevisionsForBuild(scm, build);

            return Long.toString(revisionsForBuild.get(location.getSVNURL().toString()));
        } catch (IOException e) {
            throw new DebianizingException("IOException: " + e.getMessage(), e);
        } catch (SVNException e) {
            throw new DebianizingException("SVNException: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new DebianizingException("InterruptedException: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new DebianizingException("IllegalArgumentException: " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new DebianizingException("IllegalAccessException: " + e.getMessage(), e);
        }
    }

    private ModuleLocation findOurLocation(@SuppressWarnings("rawtypes") AbstractBuild build, SubversionSCM scm, Runner runner,
            String remoteDebian) throws DebianizingException {
        for (ModuleLocation location : scm.getLocations()) {
            String moduleDir;
            try {
                ModuleLocation expandedLocation = location.getExpandedLocation(build.getEnvironment(runner.getListener()));
                moduleDir = expandedLocation.getLocalDir();

                if (remoteDebian.startsWith(build.getWorkspace().child(moduleDir).getRemote())) {
                    return expandedLocation;
                }
            } catch (IOException e) {
                throw new DebianizingException("IOException: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                throw new DebianizingException("InterruptedException: " + e.getMessage(), e);
            }
        }

        throw new DebianizingException("Can't find module location for remoteDebian " + remoteDebian);
    }

    private List<Change> getChangesFromSCM(final Runner runner, SubversionSCM scm,
            @SuppressWarnings("rawtypes") AbstractBuild build, final String remoteDebian, String latestRevision,
            String currentRevision, final String ourMessage) throws DebianizingException {
        final List<Change> result = new ArrayList<DebianPackageBuilder.Change>();

        SvnClientManager manager = SubversionSCM.createClientManager(build.getProject());
        try {
            ModuleLocation location = findOurLocation(build, scm, runner, remoteDebian);

            try {
                SVNURL svnurl = location.getSVNURL();
                manager.getLogClient().doLog(svnurl, null, SVNRevision.UNDEFINED,
                        SVNRevision.create(Long.parseLong(latestRevision) + 1), SVNRevision.parse(currentRevision), false, true,
                        0, new ISVNLogEntryHandler() {

                            @Override
                            public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
                                if (!logEntry.getMessage().equals(ourMessage)) {
                                    result.add(new Change(logEntry.getAuthor(), logEntry.getMessage()));
                                }
                            }
                        });
            } catch (SVNException e) {
                throw new DebianizingException("SVNException: " + e.getMessage(), e);
            }
        } finally {
            manager.dispose();
        }

        return result;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private List<Change> getChangesSinceLastBuild(Runner runner, AbstractBuild build) throws InterruptedException,
            DebianizingException {
        List<Change> result = new ArrayList<DebianPackageBuilder.Change>();
        Run lastSuccessfulBuild = build.getProject().getLastSuccessfulBuild();

        int lastSuccessNumber = lastSuccessfulBuild == null ? 0 : lastSuccessfulBuild.number;

        for (int num = lastSuccessNumber + 1; num < build.number; num++) {
            AbstractBuild run = (AbstractBuild) build.getProject().getBuildByNumber(num);

            if (run == null) {
                continue;
            }

            ChangeLogSet<? extends Entry> changeSet = run.getChangeSet();

            for (Entry entry : changeSet) {
                result.add(new Change(entry.getAuthor().getFullName(), entry.getMsg()));
            }
        }

        return result;
    }

    /**
     * Pojo to store change
     * 
     * @author pupssman
     */
    private static final class Change {
        private final String author;
        private final String message;

        public Change(String author, String message) {
            this.author = author;
            this.message = message;
        }

        public String getAuthor() {
            return author;
        }

        public String getMessage() {
            return message;
        }
    }

    private String clearMessage(String message) {
        return message.replaceAll("\\'", "");
    }

    /**
     * @param distributor
     *            The distributor name to use
     * @return The dch parameter with the distributor if the parameter is
     *         allowed, an empty string if not and null if the option is invalid
     */
    private String getDchDistributor(String distributor) {
        if (distributor == null)
            distributor = "";
        else if (!distributor.isEmpty())
            distributor = " " + distributor;

        if (dchDist == null || dchDist.equals("none"))
            return "";

        if (dchDist.equals("distributor"))
            return "--distributor" + distributor;
        if (dchDist.equals("vendor"))
            return "--vendor" + distributor;
        if (dchDist.equals("none"))
            return "";
        return null;
    }

    private void createChangelog(Runner runner, String remoteDebian, String packageName) throws InterruptedException,
            DebianizingException {
        if (packageName == null || packageName.isEmpty())
            throw new DebianizingException("Fail creating the changelog: Invalid package name");

        String dist = getDchDistributor("debian");
        if (dist == null)
            dist = "";

        runner.announce("Creating changelog");
        runner.runCommand(
                "export DEBEMAIL={0} && export DEBFULLNAME={1} && cd ''{2}'' && dch --check-dirname-level 0 --create --package {3} {4} --newVersion 0.0 ''{5}''",
                getDescriptor().getAccountName(), "Jenkins", remoteDebian.replaceAll("/[^/]+$", ""), packageName, dist, "initial");
    }

    private void startVersion(Runner runner, String remoteDebian, VersionHelper helper, String message, String distribution)
            throws InterruptedException, DebianizingException {
        runner.announce("Starting version <{0}> with message <{1}>", helper, clearMessage(message));

        String distributor = getDchDistributor("debian");
        if (distributor == null)
            distributor = "";

        String addDistribution = "";
        if (distribution != null && !distribution.isEmpty())
            addDistribution = "--distribution '" + distribution + "'";

        runner.runCommand(
                "export DEBEMAIL={0} && export DEBFULLNAME={1} && cd ''{2}'' && dch --check-dirname-level 0 -b {3} {4} --newVersion {5} ''{6}''",
                getDescriptor().getAccountName(), "Jenkins", remoteDebian, distributor, addDistribution, helper,
                clearMessage(message));
    }

    private void addChange(Runner runner, String remoteDebian, Change change) throws InterruptedException, DebianizingException {

        String dist = getDchDistributor("debian");
        if (dist == null)
            dist = "";

        runner.announce("Got changeset entry: {0} by {1}", clearMessage(change.getMessage()), change.getAuthor());
        runner.runCommand(
                "export DEBEMAIL={0} && export DEBFULLNAME={1} && cd ''{2}'' && dch --check-dirname-level 0 {3} --append ''{4}''",
                getDescriptor().getAccountName(), change.getAuthor(), remoteDebian, dist, clearMessage(change.getMessage()));
    }

    private void releaseVersion(Runner runner, String remoteDebian) throws InterruptedException, DebianizingException {

        String dist = getDchDistributor("debian");
        if (dist == null)
            dist = "";

        runner.announce("Releasing version");
        runner.runCommand(
                "export DEBEMAIL={0} && export DEBFULLNAME={1} && cd ''{2}'' && dch --check-dirname-level 0 -b {3} --release ''{4}''",
                getDescriptor().getAccountName(), "Jenkins", remoteDebian, dist, "release");
    }

    /**
     * FIXME Doesn't work with multi-line entries
     */
    private Map<String, String> parseChangelog(Runner runner, String remoteDebian) throws DebianizingException {
        String changelogOutput = runner.runCommandForOutput("cd \"{0}\" && dpkg-parsechangelog -lchangelog", remoteDebian);
        Map<String, String> changelog = new HashMap<String, String>();
        Pattern changelogFormat = Pattern.compile("(\\w+):\\s*(.*)");

        for (String row : changelogOutput.split("\n")) {
            Matcher matcher = changelogFormat.matcher(row);
            if (matcher.matches()) {
                changelog.put(matcher.group(1), matcher.group(2));
            }
        }

        if (changelog.isEmpty())
            throw new DebianizingException("Fail parsing changelog. Original input: " + changelogOutput);

        String[] fields = { "Source", "Version", "Distribution", "Urgency", "Maintainer", "Date", "Changes" };

        for (String field : fields) {
            if (changelog.get(field) == null)
                throw new DebianizingException("Fail parsing changelog, '" + field + "' not found. Original input: "
                        + changelogOutput);
        }

        return changelog;
    }

    private void importKeys(FilePath workspace, Runner runner) throws InterruptedException, DebianizingException, IOException {
        if (!runner.runCommandForResult("gpg --list-key {0}", getDescriptor().getAccountName())) {
            FilePath publicKey = workspace.createTextTempFile("public", "key", getDescriptor().getPublicKey());
            runner.runCommand("gpg --import ''{0}''", publicKey.getRemote());
            publicKey.delete();
        }

        if (!runner.runCommandForResult("gpg --list-secret-key {0}", getDescriptor().getAccountName())) {
            FilePath privateKey = workspace.createTextTempFile("private", "key", getDescriptor().getPrivateKey());
            runner.runCommand("gpg --import ''{0}''", privateKey.getRemote());
            privateKey.delete();
        }
    }

    public boolean isGenerateChangelog() {
        return generateChangelog;
    }

    public String getPathToDebian() {
        return pathToDebian;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private String publicKey;
        private String privateKey;
        private String accountName;
        private String passphrase;
        /**
         * Ignore the required dependencies to build the package
         */
        private boolean ignoreDeps;
        /**
         * Do not install the necessary tools to build a package
         */
        private boolean dontInstallTools;

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "Build debian package";
        }

        public String getPublicKey() {
            return publicKey;
        }

        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class type) {
            return true;
        }

        @Override
        public boolean configure(StaplerRequest staplerRequest, JSONObject json) throws FormException {
            setPrivateKey(json.getString("privateKey"));
            setPublicKey(json.getString("publicKey"));
            setAccountName(json.getString("accountName"));
            setPassphrase(json.getString("passphrase"));
            setIgnoreDeps(json.getBoolean("ignoreDeps"));
            setDontInstallTools(json.getBoolean("dontInstallTools"));

            save();
            return true; // indicate that everything is good so far
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public String getPassphrase() {
            return passphrase;
        }

        public void setPassphrase(String passphrase) {
            this.passphrase = passphrase;
        }

        public boolean isIgnoreDeps() {
            return ignoreDeps;
        }

        public void setIgnoreDeps(boolean ignoreDeps) {
            this.ignoreDeps = ignoreDeps;
        }

        public boolean isDontInstallTools() {
            return dontInstallTools;
        }

        public void setDontInstallTools(boolean dontInstallTools) {
            this.dontInstallTools = dontInstallTools;
        }

    }

    /**
     * @param build
     * @return The path in the remote machine of all debian package build steps,
     *         since the user can build than one package
     */
    public static Collection<String> getRemoteModules(AbstractBuild<?, ?> build) {
        ArrayList<String> result = new ArrayList<String>();

        for (DebianPackageBuilder builder : getDPBuilders(build)) {
            result.add(new FilePath(build.getWorkspace().getChannel(), builder.getRemoteDebian(build.getWorkspace())).child("..")
                    .getRemote());
        }

        return result;
    }

    /**
     * @param build
     * @return all the build steps of this build that his type is
     *         {@link DebianPackageBuilder}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Collection<DebianPackageBuilder> getDPBuilders(AbstractBuild<?, ?> build) {
        ArrayList<DebianPackageBuilder> result = new ArrayList<DebianPackageBuilder>();

        if (build.getProject() instanceof Project) {
            DescribableList<Builder, Descriptor<Builder>> builders = ((Project) build.getProject()).getBuildersList();
            for (Builder builder : builders) {
                if (builder instanceof DebianPackageBuilder) {
                    result.add((DebianPackageBuilder) builder);
                }
            }
        }

        return result;
    }

    public boolean isBuildEvenWhenThereAreNoChanges() {
        return buildEvenWhenThereAreNoChanges;
    }

    public boolean isUseTagAndBuild() {
        return useTagAndBuild;
    }

    public String getDistribution() {
        return distribution;
    }

    public String getBuildCommand() {
        return buildCommand;
    }

    public String getDchDist() {
        return dchDist;
    }

}
