package dev.deftu.gradle.bloom

import com.google.common.io.Files
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.util.PatternSet
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import java.io.File

class SourceReplacementTask(
    val bloom: BloomPlugin
) : DefaultTask() {

    companion object {

        private fun createTask(
            bloom: BloomPlugin,
            name: String,
            sourceSet: SourceDirectorySet,
            directory: String,
            compileTaskName: String
        ): SourceReplacementTask {
            val project = bloom.project
            val outputDirectory = File(project.layout.buildDirectory.get().asFile, directory)
            val task = project.tasks.register(name, SourceReplacementTask::class.java) { task ->
                task.group = "bloom"
                task.input = sourceSet
                task.output = outputDirectory
            }

            project.tasks.named(compileTaskName).configure { task ->
                task.dependsOn(task)
                if (task is AbstractCompile) {
                    task.setSource(outputDirectory)
                } else {
                    // Else assume Kotlin 1.7+
                    try {
                        val abstractKotlinCompileTool =
                            Class.forName("org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool")
                        val sourceFilesField = abstractKotlinCompileTool.getDeclaredField("sourceFiles")
                        sourceFilesField.isAccessible = true
                        val sourceFiles = sourceFilesField[task] as ConfigurableFileCollection
                        sourceFiles.setFrom(outputDirectory)
                    } catch (ex: ReflectiveOperationException) {
                        throw RuntimeException(ex)
                    }
                }
            }

            return task.get()
        }

        @JvmStatic
        fun createJavaTask(bloom: BloomPlugin, sourceSet: SourceSet): SourceReplacementTask =
            createTask(bloom, "bloomReplaceJava", sourceSet.java, "sources/java", "compileJava")

        @JvmStatic
        fun createKotlinTask(bloom: BloomPlugin, sourceSet: SourceSet): SourceReplacementTask {
            val kotlinSourceSet = bloom.project.extensions.getByType(KotlinJvmProjectExtension::class.java).sourceSets.getByName(sourceSet.name).kotlin
            return createTask(bloom, "bloomReplaceKotlin", kotlinSourceSet, "sources/kotlin", "compileKotlin")
        }

    }

    var input: SourceDirectorySet? = null
    var output: File? = null

    @TaskAction
    fun run() {
        if (input == null) throw NullPointerException("Input source directory set is null")
        if (output == null) throw NullPointerException("Output directory is null")

        val input = input!!
        var output = output!!

        val pattern = PatternSet()
        pattern.setIncludes(input.includes)
        pattern.setExcludes(input.excludes)

        if (output.exists()) output.deleteRecursively()
        output.mkdirs()
        output = output.canonicalFile
        this.output = output

        val extensions = bloom.getExtensions()
        val isGlobalReplacementDisabled = extensions.any(BloomExtension::isDisabled)
        val disabledFiles = extensions.flatMap(BloomExtension::disabledFiles)
        val allowedFiles = extensions.flatMap(BloomExtension::allowedFiles)
        val replacements = extensions.flatMap(BloomExtension::replacements)
        if (replacements.isEmpty()) {
            logger.debug("No replacements to make")
            return
        }

        for (dirTree in input.srcDirTrees) {
            val dir = dirTree.dir
            if (!dir.exists() || !dir.isDirectory) {
                logger.debug("Skipping invalid directory: {}", dir)
                continue
            }

            val canonicalDir = dir.canonicalFile
            val tree = project.fileTree(canonicalDir).matching(input.filter).matching(pattern)
            for (file in tree) {
                val dest = file.getDestination(canonicalDir, output)
                if (
                    disabledFiles.contains(dest.canonicalPath) ||
                    (!allowedFiles.contains(dest.canonicalPath) && isGlobalReplacementDisabled)
                ) {
                    logger.debug("Skipping file: {}", dest)
                    continue
                }

                dest.parentFile?.mkdirs()
                dest.createNewFile()

                var content = Files.asCharSource(file, Charsets.UTF_8).read()
                var isModified = false
                for (replacement in replacements) {
                    if (replacement.path != null && replacement.path != dest.canonicalPath) continue

                    content = content.replace(replacement.token, replacement.replacement.toString())
                    isModified = true
                }

                val finalPath = file.getFinalFilePath()
                if (isModified) {
                    logger.debug("Writing to file: {}", finalPath)
                    Files.asCharSink(dest, Charsets.UTF_8).write(content)
                } else {
                    logger.debug("Copying file: {}", finalPath)
                    @Suppress("UnstableApiUsage") Files.copy(file, dest)
                }
            }
        }
    }

    private fun File.getDestination(directory: File, output: File): File {
        return File(output, canonicalPath.replace(directory.canonicalPath, ""))
    }

    private fun File.getFinalFilePath(): String {
        val path = path.replace(project.projectDir.path, "").replace('\\', '/')
        if (path.startsWith("/")) return path.substring(1)

        return path
    }

}
