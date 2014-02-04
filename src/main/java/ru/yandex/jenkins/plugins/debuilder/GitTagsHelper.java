package ru.yandex.jenkins.plugins.debuilder;

import hudson.EnvVars;
import hudson.FilePath.FileCallable;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.plugins.git.GitSCM;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;

import ru.yandex.jenkins.plugins.debuilder.DebUtils.Runner;

/**
 * Performs git commiting actions in a remote WS, namely, commiting changelog to
 * the current branch Note to future self: all the fields should be serializable
 * 
 * @author pupssman
 * 
 */
public class GitTagsHelper implements FileCallable<Pair<Boolean, Set<String>>> {
    private static final long serialVersionUID = -63574604785497622L;
    private final EnvVars environment;
    private final TaskListener listener;
    private final String gitExe;
    private final String gitPrefix;

    public GitTagsHelper(AbstractBuild<?, ?> build, GitSCM scm, Runner runner) throws IOException, InterruptedException {
        this.environment = build.getEnvironment(runner.getListener());
        this.listener = runner.getListener();
        this.gitExe = scm.getGitExe(build.getBuiltOn(), listener);
        this.gitPrefix = scm.getRelativeTargetDir();
    }

    @Override
    public Pair<Boolean, Set<String>> invoke(File localWorkspace, VirtualChannel channel) {

        Set<String> tags;
        Boolean st = false;
        File gitClonePath = localWorkspace;
        if (gitPrefix != null)
            gitClonePath = new File(localWorkspace, gitPrefix);

        GitClient git = Git.with(listener, environment).in(gitClonePath).using(gitExe).getClient();

        if (git.hasGitRepo()) {
            st = true;
            tags = git.getTagNames("*");
        } else
            tags = new HashSet<String>();

        return new ImmutablePair<Boolean, Set<String>>(st, tags);
    }
}
