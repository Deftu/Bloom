package dev.deftu.gradle.bloom

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer

object BloomSourceSets {

    private val tasks = mutableSetOf<SourceReplacementTask>()

    fun apply(bloom: BloomPlugin, project: Project) {
        project.pluginManager.withPlugin("java") { _ ->

            val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
            sourceSets.all { sourceSet ->
                val extension = sourceSet.extensions.create(BloomPlugin.SOURCE_SET_EXTENSION_NAME, BloomSourceSetExtension::class.java, bloom)
                if (sourceSet.name == "main") return@all

                tasks.add(SourceReplacementTask.createJavaTask(project, extension, sourceSet, "${SourceReplacementTask.BASE_NAME}Java${sourceSet.name.capitalize()}"))

                project.pluginManager.withPlugin("kotlin") {
                    tasks.add(SourceReplacementTask.createExtendedTask(project, extension, sourceSet, "${SourceReplacementTask.BASE_NAME}Kotlin${sourceSet.name.capitalize()}", "kotlin"))
                }
            }

        }
    }

    fun getTasks(): Set<SourceReplacementTask> =
        tasks.toSet()

}