package dev.deftu.gradle.bloom

import org.gradle.api.Plugin
import org.gradle.api.Project

open class BloomPlugin : Plugin<Project> {

    companion object {
        const val PROJECT_NAME = "@PROJECT_NAME@"
        const val PROJECT_VERSION = "@PROJECT_VERSION@"

        const val PROJECT_EXTENSION_NAME = "bloom"
        const val SOURCE_SET_EXTENSION_NAME = "bloom"
    }

    lateinit var project: Project
        private set

    override fun apply(project: Project) {
        project.logger.lifecycle("$PROJECT_NAME v$PROJECT_VERSION")
        this.project = project

        BloomBase.apply(this, project)
        BloomSourceSets.apply(this, project)
    }

}
