package org.shipkit.internal.gradle.contributors;

import org.gradle.api.DefaultTask;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.shipkit.gradle.ReleaseConfiguration;
import org.shipkit.internal.notes.contributors.AllContributorsSerializer;
import org.shipkit.internal.notes.contributors.Contributors;
import org.shipkit.internal.notes.contributors.GitHubContributorsProvider;
import org.shipkit.internal.notes.contributors.ProjectContributorsSet;
import org.shipkit.internal.notes.util.IOUtil;

import java.io.File;

/**
 * Fetches data about all project contributors and stores it in file.
 * The data feeds release notes generation and pom.xml content.
 * It uses GitHub repos/contributors endpoint: https://developer.github.com/v3/repos/#list-contributors
 * This endpoint is cached by GitHub and may return information a few hours old.
 * Therefore, we also fetch recent contributors from GitHub using the "commit" end point:
 * https://developer.github.com/v3/repos/commits/
 * This way, we can also fetch the most recent contributors, necessary for correct release notes information.
 */
public class AllContributorsFetcherTask extends DefaultTask {

    private static final Logger LOG = Logging.getLogger(AllContributorsFetcherTask.class);

    @Input private String apiUrl;
    @Input private String repository;
    @Input private String readOnlyAuthToken;

    @OutputFile private File outputFile;

    /**
     * See {@link ReleaseConfiguration.GitHub#getApiUrl()}
     */
    public String getApiUrl() {
        return apiUrl;
    }

    /**
     * See {@link #getApiUrl()}
     */
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    /**
     * See {@link ReleaseConfiguration.GitHub#getRepository()}
     */
    public String getRepository() {
        return repository;
    }

    /**
     * See {@link #getRepository()}
     */
    public void setRepository(String repository) {
        this.repository = repository;
    }

    /**
     * See {@link ReleaseConfiguration.GitHub#getReadOnlyAuthToken()}
     */
    public String getReadOnlyAuthToken() {
        return readOnlyAuthToken;
    }

    /**
     * See {@link #getReadOnlyAuthToken()}
     */
    public void setReadOnlyAuthToken(String readOnlyAuthToken) {
        this.readOnlyAuthToken = readOnlyAuthToken;
    }

    /**
     * Where serialized information about contributors will be stored.
     */
    public File getOutputFile() {
        return outputFile;
    }

    /**
     * See {@link #getOutputFile()}
     */
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    @TaskAction
    public void fetchContributors() {
        LOG.lifecycle("  Fetching all contributors for project");

        GitHubContributorsProvider contributorsProvider = Contributors.getGitHubContributorsProvider(apiUrl, repository, readOnlyAuthToken);
        ProjectContributorsSet contributors = contributorsProvider.getAllContributorsForProject();

        AllContributorsSerializer serializer = new AllContributorsSerializer();
        final String json = serializer.serialize(contributors);
        IOUtil.writeFile(outputFile, json);

        LOG.lifecycle("  Serialized all contributors into: {}", getProject().relativePath(outputFile));
    }
}
