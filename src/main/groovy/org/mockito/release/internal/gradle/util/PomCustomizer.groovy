package org.mockito.release.internal.gradle.util

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.publish.maven.MavenPublication
import org.mockito.release.gradle.ReleaseToolsProperties

class PomCustomizer {

    private static final Logger LOG = Logging.getLogger(PomCustomizer)

    /**
     * Customizes the pom. The method requires following 'ext' properties on the project to function correctly:
     *
     * <ul>
     *  <li> project.description
     *  <li> project.archivesBaseName
     *  <li> project.rootProject.ext.gh_repository
     *  <li> project.rootProject.ext.pom_developers
     *  <li> project.rootProject.ext.pom_contributors
     * </ul>
     */
    static void customizePom(Project project, MavenPublication publication) {

        /**
         * See issue https://github.com/mockito/mockito-release-tools/issues/36
         *
         * To implement automatic contributors in the pom, we would need something like (brainstorming):
         *  - getting all contributors for the project using GitHub api and feeding this plugin with it
         *  - parse the release notes file and just include all contributors we can find there :)
         *  - make the release generation create an additional file with release notes metadata in some structured format
         *      (like JSON or xml), then we could parse that file to get the contributors
         *  - make the release create additional file with contributors
         *  - the list of core developers would be static, but the list of contributors would grow
         */

        //TODO accessing 'ext' properties needs to be 'safe' and fail if the user have not provided stuff
        //TODO relies on ext properties set on the root project. Seems not right
        def ext = new ExtContainer(project.rootProject)
        publication.pom.withXml {
            LOG.info("""  Customizing pom for publication '$publication.name' in project '$project.path'
    - Module name (project.archivesBaseName): $project.archivesBaseName
    - Description (project.description): $project.description
    - GitHub repository (project.rootProject.ext.gh_repository): ${ext.getString(ReleaseToolsProperties.gh_repository)}
    - Developers (project.rootProject.ext.pom_developers): ${ext.getCollection(ReleaseToolsProperties.pom_developers).join(', ')}
    - Contributors (project.rootProject.ext.pom_contributors): ${ext.getCollection(ReleaseToolsProperties.pom_contributors).join(', ')}""")
            
            def root = asNode()
            def rootProject = project.rootProject

            //Assumes project has java plugin applied. Pretty safe assumption
            root.appendNode('name', project.archivesBaseName)

            root.appendNode('packaging', 'jar')
            root.appendNode('url', "https://github.com/${rootProject.ext.gh_repository}")
            root.appendNode('description', project.description)

            def license = root.appendNode('licenses').appendNode('license')
            license.appendNode('name', 'The MIT License')
            license.appendNode('url', "https://github.com/${rootProject.ext.gh_repository}/blob/master/LICENSE")
            license.appendNode('distribution', 'repo')

            root.appendNode('scm').appendNode('url', "https://github.com/${rootProject.ext.gh_repository}.git")

            def issues = root.appendNode('issueManagement')
            issues.appendNode('url', "https://github.com/${rootProject.ext.gh_repository}/issues")
            issues.appendNode('system', 'GitHub issues')

            def ci = root.appendNode('ciManagement')
            ci.appendNode('url', "https://travis-ci.org/${rootProject.ext.gh_repository}")
            ci.appendNode('system', 'TravisCI')

            def developers = root.appendNode('developers')
            rootProject.ext.pom_developers.each {
                def split = it.split(':')
                assert split.length == 2
                def d = developers.appendNode('developer')
                d.appendNode('id', split[0])
                d.appendNode('name', split[1])
                d.appendNode('roles').appendNode('role', 'Core developer')
                d.appendNode('url', "https://github.com/${split[0]}")
            }

            def contributors = root.appendNode('contributors')
            rootProject.ext.pom_contributors.each {
                def split = it.split(':')
                assert split.length == 2
                def c = contributors.appendNode('contributor')
                c.appendNode('name', split[1])
                c.appendNode('url', "https://github.com/${split[0]}")
            }
        }
    }
}
