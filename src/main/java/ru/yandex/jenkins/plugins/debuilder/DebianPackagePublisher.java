package ru.yandex.jenkins.plugins.debuilder;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildBadgeAction;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.model.Run.Artifact;
import hudson.plugins.git.GitSCM;
import hudson.remoting.VirtualChannel;
import hudson.scm.SCM;
import hudson.scm.SubversionSCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jenkins.model.ArtifactManager;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.lang3.tuple.Pair;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;

import ru.yandex.jenkins.plugins.debuilder.DebUtils.Runner;
import ru.yandex.jenkins.plugins.debuilder.dpkg.DebianChanges;
import ru.yandex.jenkins.plugins.debuilder.dpkg.common.DebianDistributions;
import ru.yandex.jenkins.plugins.debuilder.dpkg.common.DebianFileEntry;
import ru.yandex.jenkins.plugins.debuilder.dupload.Dupload;
import ru.yandex.jenkins.plugins.debuilder.dupload.DuploadException;
import ru.yandex.jenkins.plugins.debuilder.dupload.DuploadFTPMethod;

public class DebianPackagePublisher extends Recorder implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String PREFIX = "debian-package-publisher";

	private String repoId;
	private String commitMessage;
	private final boolean commitChanges;
	private final boolean republish;
	private String republishDistribution;

	/**
	 * Constructor with the required fields that jenkins require
	 * 
	 * @param repoId
	 *            The repo id
	 * @param commitMessage
	 *            The commit message requested by SCMs
	 * @param commitChanges
	 *            Condition required to commit the changes
	 */
	@DataBoundConstructor
	public DebianPackagePublisher(String repoId, String commitMessage, boolean commitChanges, boolean republish, String republishDistribution) {
		this.commitChanges = commitChanges;
		this.commitMessage = commitMessage;
		this.repoId = repoId;
		this.republish = republish;
		this.republishDistribution = republishDistribution;

		if (getRepo() == null) {
			throw new IllegalArgumentException(MessageFormat.format("Repo {0} is not found in global configuration", repoId));
		}
	}

	/**
	 * Get the repository object from its id
	 * 
	 * @return Repository Object
	 */
	private DebianPackageRepo getRepo() {
		for(DebianPackageRepo repo: getDescriptor().getRepositories()) {
			if (repo.getName().equals(repoId)) {
				return repo;
			}
		}
		return null;
	}

	/**
	 * Get the message used to commit changes an a arbitrary build
	 * 
	 * @param build
	 *            The build where the message will be extracted
	 * @return The configured message
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String getUsedCommitMessage(AbstractBuild build) {
		DescribableList<Publisher, Descriptor<Publisher>> publishersList = ((Project)build.getProject()).getPublishersList();
		for (Publisher publisher: publishersList) {
			if (publisher instanceof DebianPackagePublisher) {
				return ((DebianPackagePublisher) publisher).commitMessage;
			}
		}

		return "";
	}

	/**
	 * The key located inside the file debian-package-builder-keys on the root
	 * of the Jenkins Instalation is copied to the remote slave.
	 * 
	 * @param build
	 *            The build where te key will be stored
	 * @return The remote path to the key
	 * @throws IOException
	 *             If some error occurs creating the remote temp file
	 * @throws InterruptedException
	 *             If the file operations where interrupted
	 */
	private String getRemoteKeyPath(AbstractBuild<?, ?> build) throws IOException, InterruptedException {
		String keysDir = "debian-package-builder-keys";

		String relativeKeyPath = new File(keysDir, getRepo().getKeypath()).getPath();
		File absoluteKeyPath = new File (Jenkins.getInstance().getRootDir(), relativeKeyPath);
		FilePath localKey = new FilePath(absoluteKeyPath);

		FilePath remoteKey = build.getWorkspace().createTextTempFile("private", "key", localKey.readToString());
		remoteKey.chmod(0600);
		return remoteKey.getRemote();
	}

	/**
	 * Create a new dupload.conf prepared to upload the packages
	 * 
	 * @param filePath
	 *            Path to the dupload.conf
	 * @param build
	 *            Build that needs this configuration. The remote build path is
	 *            used to store the ssh-key used on the scpb upload method.
	 * @param runner
	 *            The runner is used to log the process
	 * @throws IOException
	 *             If some i/o error occour
	 * @throws InterruptedException
	 *             If some file process is interrupted
	 * @throws DebianizingException
	 *             If the log fail
	 */
	private void generateDuploadConf(String filePath, AbstractBuild<?, ?> build, Runner runner) throws IOException, InterruptedException, DebianizingException {
		String confTemplate =
				"package config;\n\n" +
				"$default_host = '${name}';\n\n" +
				"$cfg{'${name}'} = {\n" +
				"\tlogin => '${login}',\n" +
				"\tfqdn => '${fqdn}',\n" +
				"\tmethod => '${method}',\n" +
				"\tincoming => '${incoming}',\n" +
				"\tdinstall_runs => 0,\n" +
				"\toptions => '${options}',\n" +
				"};\n\n" +
				"1;\n";

		Map<String, String> values = new HashMap<String, String>();

		DebianPackageRepo repo = getRepo();

		values.put("name", repo.getName());
		values.put("method", repo.getMethod());
		values.put("fqdn", repo.getFqdn());
		values.put("incoming", repo.getIncoming());
		values.put("login", repo.getLogin());
		values.put("options", MessageFormat.format("-i {0} ", getRemoteKeyPath(build)) + repo.getOptions());

		StrSubstitutor substitutor = new StrSubstitutor(values);
		String conf = substitutor.replace(confTemplate);

		FilePath duploadConf = build.getWorkspace().createTempFile("dupload", "conf");
		duploadConf.touch(System.currentTimeMillis()/1000);
		duploadConf.write(conf, "UTF-8");

		runner.runCommand("sudo mv ''{0}'' ''{1}''", duploadConf.getRemote().replaceAll("'", "'\''"), filePath.replaceAll("'", "'\''"));
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException {
		PrintStream logger = listener.getLogger();

		Runner runner = new DebUtils.Runner(build, launcher, listener, PREFIX);

		List<String> modules = new ArrayList<String>();
		Map<String, DebianChanges> moduleChange = new HashMap<String, DebianChanges>();

		if (republish) {
			boolean someChangesFound = false;

			AbstractBuild<?, ?> targetBuild = build.getRootBuild();
			ArtifactManager artManager = targetBuild.getArtifactManager();

			// Get artifacts
			@SuppressWarnings("rawtypes")
			Map<String, Artifact> artifacts = new HashMap<String, Artifact>();
			for (@SuppressWarnings("rawtypes")
			Artifact artifact : targetBuild.getArtifacts()) {
				artifacts.put(artifact.getFileName(), artifact);
			}

			// Get all .changes files
			for (String artName : artifacts.keySet()) {
				if (artName.endsWith(".changes")) {

					runner.announce("Republishing " + artName);
					someChangesFound = true;

					FilePath tmpDir = createTmpDir(build.getWorkspace());

					FilePath changesFile = copyArtifact(artifacts.get(artName), artManager, tmpDir);

					DebianChanges changes = new DebianChanges();
					List<Pair<String, String>> rejected = changes.parse(changesFile);

					if (rejected == null) {
						runner.announce("Fail reading the .changes file");
						return false;
					} else if (!rejected.isEmpty()) {
						runner.announce("Some lines from .changes are invalid.");
						for (Pair<String, String> line : rejected)
							runner.announce(line.getLeft() + ": " + line.getRight());
					}

					if (changes.getFiles() == null || changes.getFiles().getFileEntries() == null) {
						runner.announce("None file in .changes");
						return false;
					}

					Iterator<DebianFileEntry> iFiles = changes.getFiles().getFileEntries().iterator();
					while (iFiles.hasNext()) {
						DebianFileEntry entry = iFiles.next();

						if (!artifacts.containsKey(entry.getName())) {
							runner.announce("File " + entry.getName() + " not found in artifacts, removing from .changes");
							iFiles.remove();
							continue;
						}

						if (copyArtifact(artifacts.get(entry.getName()), artManager, tmpDir) == null)
							runner.announce("Fail copying " + entry.getName());
					}

					if (republishDistribution != null && !republishDistribution.isEmpty()) {
						DebianDistributions dist = new DebianDistributions();
						if (!dist.add(republishDistribution)) {
							runner.announce("Fail forcing the ditribution");
							return false;
						}
						changes.setDistributions(dist);
					}

					try {
						changesFile.write(changes.getBody(), "UTF-8");
					} catch (InterruptedException e) {
						e.printStackTrace();
						runner.announce("Fail rewriting the .changes");
					}
					modules.add(tmpDir.getRemote());
					moduleChange.put(tmpDir.getRemote(), changes);
				}
			}

			// upload
			if (!someChangesFound) {
				runner.announce("None .changes file found in artifacts");
				return false;
			}

		} else {

			if (build.getResult().isWorseThan(Result.SUCCESS)) {
				logger.println(MessageFormat.format(DebianPackageBuilder.ABORT_MESSAGE, PREFIX, "Build is not success, will not execute debrelease"));
				return true;
			}

			Map<String, DebianBadge> badges = new HashMap<String, DebianBadge>();

			// Get all badges from this build. (A badge is a mark when the build
			// was
			// successful)
			for (BuildBadgeAction action : build.getBadgeActions()) {
				if (action instanceof DebianBadge) {
					badges.put(((DebianBadge) action).getModule(), (DebianBadge) action);
				}
			}

			// Get the path in the remote machine from all debian build steps
			Collection<String> allRemoteModules = DebianPackageBuilder.getRemoteModules(build);

			if (allRemoteModules.size() <= 0) {
				runner.announce("None module found!");
			}

			for (String remoteModule : allRemoteModules) {
				// If a module was not built, skip
				String builtModule = new FilePath(build.getWorkspace().getChannel(), remoteModule).child("debian").getRemote();
				if (!badges.containsKey(builtModule)) {
					runner.announce("Module in {0} was not built - not releasing", remoteModule);
					continue;
				}
				modules.add(remoteModule);
			}
		}

		DebianPackageRepo repo = getRepo();

		try {
			for (String module : modules) {
				runner.announce("Publishing package from '" + module + "'");

				// Get the publish success
				boolean wereBuilds = false;

				if (repo.getMethod() == null)
					throw new DebianizingException("Repo method not found");

				if (repo.getMethod().equals("scpb")) {

					String duploadConfPath = "/etc/dupload.conf";

					if (!getDescriptor().isDontInstallTools())
						runner.runCommand("sudo apt-get -y install dupload devscripts");
					generateDuploadConf(duploadConfPath, build, runner);

					if (!runner.runCommandForResult("cd ''{0}'' && debrelease", module.replaceAll("'", "'\''")))
						throw new DebianizingException("Debrelease failed");

					wereBuilds = true;

				} else if (repo.getMethod().equals("ftp")) {

					FilePath modulePath = new FilePath(build.getWorkspace().getChannel(), module);

					FilePath dotChangeBasePath = modulePath.getParent();
					FilePath changelog = modulePath.child("debian/changelog");

					DuploadFTPMethod duploadFTP = new DuploadFTPMethod(repo.getFqdn());
					duploadFTP.setPassword(repo.getPassword());
					duploadFTP.setUsername(repo.getLogin());

					// TODO execute the upload process on the slave machine
					if (!republish)
						Dupload.start(changelog, dotChangeBasePath, duploadFTP, repo.getIncoming(), "$distribution$", runner);
					else {
						Dupload.start(modulePath.list("*.changes")[0], duploadFTP, repo.getIncoming(), "$distribution$", runner);
						DebianChanges changes = moduleChange.get(module);

						if (changes != null && changes.getSource() != null && changes.getVersion() != null) {
							EnvVars envVars = new EnvVars("DEBIAN_SOURCE_PACKAGE", changes.getSource(), "DEBIAN_PACKAGE_VERSION", changes.getVersion().toString());
							build.getEnvironments().add(Environment.create(envVars));
						}
					}

					wereBuilds = true;
				}

				if (republish) {
					for (String tmpDir : modules)
						if (!removeTmpDir(build.getWorkspace().getChannel(), tmpDir))
							runner.announce("Fail removing tmpdir " + tmpDir);

					// If republishing skip final publish process
					continue;
				}

				if (wereBuilds && commitChanges) {
					String expandedCommitMessage = getExpandedCommitMessage(build, listener);
					commitChanges(build, runner, expandedCommitMessage);
				}

			}
		} catch (InterruptedException e) {
			logger.println(MessageFormat.format(DebianPackageBuilder.ABORT_MESSAGE, PREFIX, e.getMessage()));
			build.setResult(Result.UNSTABLE);
		} catch (DebianizingException e) {
			logger.println(MessageFormat.format(DebianPackageBuilder.ABORT_MESSAGE, PREFIX, e.getMessage()));
			build.setResult(Result.UNSTABLE);
		} catch (DuploadException e) {
			logger.println(MessageFormat.format(DebianPackageBuilder.ABORT_MESSAGE, PREFIX, e.getMessage()));
			build.setResult(Result.UNSTABLE);
		}

		return true;
	}

	private String getExpandedCommitMessage(AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
		EnvVars env = build.getEnvironment(listener);
		return env.expand(getCommitMessage());
	}

	private void commitChanges(AbstractBuild<?, ?> build, Runner runner, String commitMessage) throws DebianizingException, IOException, InterruptedException {
		SCM scm = build.getProject().getScm();

		if (scm instanceof SubversionSCM) {
			commitToSVN(build, runner, (SubversionSCM)scm, commitMessage);
		} else if (scm instanceof GitSCM) {
			commitToGitAndPush(build, runner, (GitSCM)scm, commitMessage);
		} else {
			throw new DebianizingException("SCM used is not a know one but " + scm.getType());
		}
	}

	private void commitToGitAndPush(final AbstractBuild<?, ?> build, final Runner runner, GitSCM scm, String commitMessage) throws DebianizingException {
		try {
			GitCommitHelper helper = new GitCommitHelper(build, scm, runner, commitMessage, DebianPackageBuilder.getRemoteModules(build));

			if (build.getWorkspace().act(helper)) {
				runner.announce("Successfully commited to git");
			} else {
				throw new DebianizingException("Failed to commit to git");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void commitToSVN(final AbstractBuild<?, ?> build, final Runner runner, SubversionSCM svn, String commitMessage) throws DebianizingException {
		hudson.scm.SubversionSCM.DescriptorImpl descriptor = (hudson.scm.SubversionSCM.DescriptorImpl) Jenkins.getInstance().getDescriptor(hudson.scm.SubversionSCM.class);
		ISVNAuthenticationProvider authenticationProvider = descriptor.createAuthenticationProvider(build.getProject());

		try {
			for (String module: DebianPackageBuilder.getRemoteModules(build)) {
				SVNCommitHelper helper = new SVNCommitHelper(authenticationProvider, module, commitMessage);
				runner.announce("Commited revision <{0}> of <{2}> with message <{1}>", runner.getChannel().call(helper), commitMessage, module);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new DebianizingException("IOException: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new DebianizingException("Interrupted: " + e.getMessage(), e);
		}
	}

	@Override
	public boolean needsToRunAfterFinalized() {
		return false;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		private List<DebianPackageRepo> repos = new ArrayList<DebianPackageRepo>();

		/**
		 * Do not install the necessary tools to build a package.
		 * Defaults to false for backward compatibility
		 */
		private boolean dontInstallTools = false;

		public DescriptorImpl() {
			super();
			load();
		}

		public ArrayList<DebianPackageRepo> getRepositories() {
			return new ArrayList<DebianPackageRepo>(repos);
		}

		public ListBoxModel doFillRepoIdItems() {
			ListBoxModel model = new ListBoxModel();

			for (DebianPackageRepo repo: repos) {
				model.add(repo.getName(), repo.getName());
			}

			return model;
		}

		public FormValidation doCheckMethod(@QueryParameter String method) {
			if (method != "scpb" || method != "ftp") {
				return FormValidation.error("This method is not supported yet");
			} else {
				return FormValidation.ok();
			}
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			repos = req.bindJSONToList(DebianPackageRepo.class, formData.get("repositories"));
			setDontInstallTools(formData.getBoolean("dontInstallTools"));
			save();

			return super.configure(req,formData);
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Publish debian packages";
		}

		public boolean isDontInstallTools() {
			return dontInstallTools;
		}

		public void setDontInstallTools(boolean dontInstallTools) {
			this.dontInstallTools = dontInstallTools;
		}
	}

	public boolean isCommitChanges() {
		return commitChanges;
	}

	public String getCommitMessage() {
		return commitMessage;
	}

	public String getRepoId() {
		return repoId;
	}

	public boolean isRepublish() {
		return republish;
	}

	public String getRepublishDistribution() {
		return republishDistribution;
	}

	public void setRepublishDistribution(String republishDistribution) {
		this.republishDistribution = republishDistribution;
	}

	/**
	 * @param file
	 * @return The file copied or null if it fail.
	 */
	@SuppressWarnings("rawtypes")
	private FilePath copyArtifact(Artifact artifact, ArtifactManager manager, FilePath path) {
		if (artifact == null || manager == null || path == null)
			return null;

		InputStream input = null;
		try {
			input = manager.root().child(artifact.relativePath).open();

			FilePath dest = path.child(artifact.getFileName());
			dest.copyFrom(input);

			return dest;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (input != null)
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	/**
	 * @param path
	 * @return The tmp path or null
	 */
	private FilePath createTmpDir(FilePath path) {
		if (path == null)
			return null;

		try {
			return path.createTempDir("debian-package-builder.", ".republish");
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @param path
	 * @return True if removed false if not
	 */
	private boolean removeTmpDir(FilePath path) {
		if (path == null)
			return false;

		try {
			path.deleteRecursive();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * @param channel
	 *            The channel
	 * @param path
	 *            the remote path
	 * @return Same as {@link #removeTmpDir(FilePath)}
	 */
	private boolean removeTmpDir(VirtualChannel channel, String path) {
		return removeTmpDir(new FilePath(channel, path));
	}
}
