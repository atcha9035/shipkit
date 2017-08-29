package org.shipkit.internal.gradle.git;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.process.ExecResult;
import org.shipkit.gradle.configuration.ShipkitConfiguration;
import org.shipkit.gradle.exec.ShipkitExecTask;
import org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin;
import org.shipkit.internal.gradle.exec.ExecCommandFactory;
import org.shipkit.internal.gradle.git.tasks.GitCheckOutTask;
import org.shipkit.internal.gradle.util.TaskMaker;

import static java.util.Arrays.asList;

/**
 * Plugin that adds Git tasks commonly used for setting up
 * working copy when running build on CI environment.
 * Adds following tasks:
 * <ul>
 *     <li>
 *         'gitUnshallow' - performs 'git unshallow' to get sufficient amount of commits.
 *         Needed for CI workflows, where the clone is typically shallow.
 *         We need good number of commits to generate release notes for.</li>
 *     <li>
 *         'gitCheckout' ({@link GitCheckOutTask}) - checks out specific branch.
 *         Needed for CI workflows, where CI server automatically checks out rev hash of the commit, detaching from HEAD.
 *         In detached HEAD, all commits are lost. We need to make commits for version bumps and release notes/changelog.
 *         Therefore we need to checkout real branch like "master"</li>
 *     <li>
 *         'setGitUserName' - sets generic user name so that CI server can commit code as neatly described robot,
 *         uses value from {@link ShipkitConfiguration.Git#getUser()}
 *     </li>
 *     <li>
 *         'setGitUserEmail' - sets generic user email so that CI server can commit code as neatly described robot,
 *         uses value from {@link ShipkitConfiguration.Git#getEmail()}
 *     </li>
 *     <li>
 *         'ciReleasePrepare' - prepares for release from CI,
 *         depends on most other tasks (unshallow, git checkout branch, set generic git user and email).
 *     </li>
 * </ul>
 */
public class GitSetupPlugin implements Plugin<Project> {

    private static final Logger LOG = Logging.getLogger(GitSetupPlugin.class);

    private static final String UNSHALLOW_TASK = "gitUnshallow";
    public static final String CHECKOUT_TASK = "gitCheckout";
    public static final String SET_USER_TASK = "setGitUserName";
    public static final String SET_EMAIL_TASK = "setGitUserEmail";
    public static final String CI_RELEASE_PREPARE_TASK = "ciReleasePrepare";

    @Override
    public void apply(Project project) {
        final ShipkitConfiguration conf = project.getPlugins().apply(ShipkitConfigurationPlugin.class).getConfiguration();

        TaskMaker.task(project, UNSHALLOW_TASK, ShipkitExecTask.class, new Action<ShipkitExecTask>() {
            public void execute(ShipkitExecTask t) {
                //Travis default clone is shallow which will prevent correct release notes generation for repos with lots of commits
                t.setDescription("Ensures good chunk of recent commits is available for release notes automation.");
                t.execCommand(ExecCommandFactory.execCommand("Getting more commits",
                    asList("git", "fetch", "--unshallow"), new Action<ExecResult>() {
                        @Override
                        public void execute(ExecResult result) {
                            if (result.getExitValue() != 0) {
                                LOG.lifecycle("  'git fetch --unshallow' failed and will be ignored." +
                                    "\n  Most likely the repository already contains all history.");
                            }
                        }
                    }));
            }
        });

        TaskMaker.task(project, CHECKOUT_TASK, GitCheckOutTask.class, new Action<GitCheckOutTask>() {
            public void execute(final GitCheckOutTask t) {
                t.setDescription("Checks out the branch that can be committed. CI systems often check out revision that is not committable.");
            }
        });

        TaskMaker.task(project, SET_USER_TASK, ShipkitExecTask.class, new Action<ShipkitExecTask>() {
            public void execute(final ShipkitExecTask t) {
                t.setDescription("Overwrites local git 'user.name' with a generic name. Intended for CI.");
                t.execCommand(ExecCommandFactory.execCommand("Setting git user name",
                    "git", "config", "--local", "user.name", conf.getGit().getUser()));
            }
        });

        TaskMaker.task(project, SET_EMAIL_TASK, ShipkitExecTask.class, new Action<ShipkitExecTask>() {
            public void execute(final ShipkitExecTask t) {
                t.setDescription("Overwrites local git 'user.email' with a generic email. Intended for CI.");
                t.execCommand(ExecCommandFactory.execCommand("Setting git user email",
                    "git", "config", "--local", "user.email", conf.getGit().getEmail()));
            }
        });

        TaskMaker.task(project, CI_RELEASE_PREPARE_TASK, new Action<Task>() {
            public void execute(Task t) {
                t.setDescription("Prepares the working copy for releasing from CI build");
                t.dependsOn(UNSHALLOW_TASK, CHECKOUT_TASK, SET_USER_TASK, SET_EMAIL_TASK);
            }
        });
    }
}
