package dev.deftu.gradle.bloom

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer

class BloomPlugin : Plugin<Project> {

    companion object {
        const val PROJECT_EXTENSION_NAME = "bloom"
        const val JAR_EXTENSION_NAME = "bloom"
    }

    lateinit var project: Project
        private set
    private val tasks = mutableListOf<SourceReplacementTask>()

    override fun apply(project: Project) {
        this.project = project

        val extension = project.extensions.create(PROJECT_EXTENSION_NAME, BloomProjectExtension::class.java, this)
        project.pluginManager.withPlugin("java", ::setupJarExtension)

        project.afterEvaluate { _ ->
            setupTasks()
        }
    }

    fun getExtensions(): Set<BloomExtension> {
        val extensions = mutableSetOf<BloomExtension>()

        project.extensions.findByType(BloomProjectExtension::class.java)?.let(extensions::add)
        maybeGetJarExtension()?.let(extensions::add)

        return extensions
    }

    private fun setupTasks() {
        val sourceSet = project.extensions.getByType(SourceSetContainer::class.java).getByName("main")

        project.pluginManager.withPlugin("java") {
            SourceReplacementTask.createJavaTask(this, sourceSet)
        }

        project.pluginManager.withPlugin("kotlin") {
            SourceReplacementTask.createKotlinTask(this, sourceSet)
        }
    }

    /**
     * Adds a `bloom` block to the project's `jar` extension, allowing specific JAR behavior to be configured.
     */
    private fun setupJarExtension(plugin: AppliedPlugin) {
        check(plugin is JavaPlugin)

        val task = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME)
        task.extensions.create(JAR_EXTENSION_NAME, BloomJarExtension::class.java, this)
    }

    private fun maybeGetJarExtension(): BloomJarExtension? {
        return project.tasks.getByName(JavaPlugin.JAR_TASK_NAME).extensions.findByType(BloomJarExtension::class.java)
    }

}
