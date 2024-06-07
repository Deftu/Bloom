package dev.deftu.gradle.bloom

import dev.deftu.gradle.bloom.BloomPlugin.Companion.PROJECT_EXTENSION_NAME
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer

object BloomBase {

    private val tasks = mutableSetOf<SourceReplacementTask>()

    fun apply(bloom: BloomPlugin, project: Project) {
        val extension = project.extensions.create(PROJECT_EXTENSION_NAME, BloomProjectExtension::class.java, bloom)
        project.afterEvaluate { _ ->
            val sourceSet = project.extensions.getByType(SourceSetContainer::class.java).getByName("main")

            project.pluginManager.withPlugin("java") {
                tasks.add(SourceReplacementTask.createJavaTask(project, extension, sourceSet, "${SourceReplacementTask.BASE_NAME}Java"))
            }

            project.pluginManager.withPlugin("kotlin") {
                tasks.add(SourceReplacementTask.createExtendedTask(project, extension, sourceSet, "${SourceReplacementTask.BASE_NAME}Kotlin", "kotlin"))
            }
        }
    }

    fun getTasks(): Set<SourceReplacementTask> =
        tasks.toSet()

}
