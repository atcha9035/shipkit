package org.shipkit.internal.gradle.versionupgrade

import org.gradle.api.ProjectConfigurationException
import org.gradle.testfixtures.ProjectBuilder
import org.shipkit.gradle.exec.ShipkitExecTask
import org.shipkit.internal.gradle.git.tasks.CloneGitRepositoryTask
import org.shipkit.internal.gradle.java.ShipkitJavaPlugin
import testutil.PluginSpecification

class UpgradeDownstreamPluginTest extends PluginSpecification {

    void setup(){
        project.plugins.apply(ShipkitJavaPlugin)
    }

    def "should fail when no consumer repositories defined"() {
        when:
        project.plugins.apply(UpgradeDownstreamPlugin)
        project.evaluate()

        then:
        def ex = thrown(ProjectConfigurationException)
        ex.cause instanceof IllegalArgumentException
        ex.cause.message == "'upgradeDownstream.repositories' cannot be null."
    }

    def "should correctly configure tasks for consumer repositories"() {
        when:
        def upgradeDownstream = project.plugins.apply(UpgradeDownstreamPlugin).upgradeDownstreamExtension
        upgradeDownstream.repositories = ['wwilk/shipkit-example', 'wwilk/mockito']
        project.evaluate()

        then:
        project.tasks.upgradeDownstream
        project.tasks['upgradeWwilkShipkitExample']
        project.tasks['upgradeWwilkMockito']
        project.tasks['cloneWwilkShipkitExample']
        project.tasks['cloneWwilkMockito']
    }

    def "should correctly configure clone consumer repo task"() {
        when:
        def upgradeDownstream = project.plugins.apply(UpgradeDownstreamPlugin).upgradeDownstreamExtension
        upgradeDownstream.repositories = ['wwilk/mockito']
        conf.gitHub.url = 'http://git.com'
        project.evaluate()

        then:
        CloneGitRepositoryTask task = project.tasks['cloneWwilkMockito']
        task.targetDir == project.file(project.buildDir.absolutePath + '/downstream-upgrade/wwilkMockito')
        task.repositoryUrl == 'http://git.com/wwilk/mockito'
    }

    def "should correctly configure upgrade{Repo} task"() {
        when:
        def upgradeDownstream = project.plugins.apply(UpgradeDownstreamPlugin).upgradeDownstreamExtension
        upgradeDownstream.repositories = ['wwilk/mockito']
        project.group = "depGroup"

        project.evaluate()

        then:
        ShipkitExecTask task = project.tasks['upgradeWwilkMockito']
        task.execCommands[0].commandLine == ["./gradlew", "performVersionUpgrade", "-Pdependency=depGroup:depName:0.1.2"]
    }

    def "should add CI tasks to upgrade{Repo} task execCommands when CiUpgradeDownstreamPlugin applied"() {
        when:
        def upgradeDownstream = project.plugins.apply(UpgradeDownstreamPlugin).upgradeDownstreamExtension
        upgradeDownstream.repositories = ['wwilk/mockito']
        project.group = "depGroup"
        project.plugins.apply(CiUpgradeDownstreamPlugin)

        project.evaluate()

        then:
        ShipkitExecTask task = project.tasks['upgradeWwilkMockito']
        task.execCommands[0].commandLine == ["./gradlew", "setGitUserEmail", "setGitUserName", "performVersionUpgrade", "-Pdependency=depGroup:depName:0.1.2"]
    }

    @Override
    void initProject() {
        project = new ProjectBuilder().withName("depName").withProjectDir(tmp.root).build()
        project.file("version.properties" ) << "version=0.1.3\npreviousVersion=0.1.2"
    }
}
