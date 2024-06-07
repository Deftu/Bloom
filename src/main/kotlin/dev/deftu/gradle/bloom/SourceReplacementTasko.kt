package dev.deftu.gradle.bloom

import com.google.common.io.Files
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.util.PatternSet
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import java.io.File
import java.util.Locale
import javax.inject.Inject

open class SourceReplacementTasko @Inject constructor() : DefaultTask() {

    companion object {

        const val BASE_NAME = "blossomReplace"
        const val COMPILE_JAVA_TASK_NAME = "compileJava"
        const val COMPILE_KOTLIN_TASK_NAME = "compileKotlin"

        @JvmStatic
        fun createJavaTask(
            bloom: BloomPlugin,
            extension: BloomExtension,
            sourceSet: SourceSet,
            name: String
        ): SourceReplacementTasko {
            return createTask(bloom, extension, name, sourceSet.java, "bloom/java", COMPILE_JAVA_TASK_NAME)
        }

        @JvmStatic
        fun createKotlinTask(
            bloom: BloomPlugin,
            extension: BloomExtension,
            sourceSet: SourceSet,
            name: String
        ): SourceReplacementTasko {
            val kotlinSourceSet = bloom.project.extensions.getByType(KotlinJvmProjectExtension::class.java).sourceSets.getByName(sourceSet.name).kotlin
            return createTask(bloom, extension, name, kotlinSourceSet, "bloom/kotlin", COMPILE_KOTLIN_TASK_NAME)
        }

        private fun createTask(
            bloom: BloomPlugin,
            extension: BloomExtension,

            name: String,
            sourceSet: SourceDirectorySet,
            directory: String,
            compileTaskName: String
        ): SourceReplacementTasko {
            val project = bloom.project
            val outputDirectory = File(project.layout.buildDirectory.get().asFile, directory)
            val task = project.tasks.register(name, SourceReplacementTasko::class.java) { task ->
                task.group = BloomPlugin.PROJECT_NAME.lowercase(Locale.US)

                task.disabled = extension.isDisabled
                task.disabledFiles = extension.disabledFiles
                task.allowedFiles = extension.allowedFiles
                task.replacements = extension.replacements

                task.input = sourceSet
                task.outputDirectory.set(outputDirectory)
            }

            project.tasks.named(compileTaskName).configure { compileTask ->
                compileTask.dependsOn(task)
                if (compileTask is AbstractCompile) {
                    compileTask.setSource(outputDirectory)
                } else {
                    // Else assume Kotlin 1.7+
                    try {
                        val abstractKotlinCompileTool =
                            Class.forName("org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool")
                        val sourceFilesField = abstractKotlinCompileTool.getDeclaredField("sourceFiles")
                        sourceFilesField.isAccessible = true
                        val sourceFiles = sourceFilesField[compileTask] as ConfigurableFileCollection
                        sourceFiles.setFrom(outputDirectory)
                    } catch (ex: ReflectiveOperationException) {
                        throw RuntimeException(ex)
                    }
                }
            }

            return task.get()
        }

    }

    @Input
    var disabled: Boolean = false

    @InputFiles
    var disabledFiles: List<String> = emptyList()

    @InputFiles
    var allowedFiles: List<String> = emptyList()

    @Input
    var replacements: List<BloomExtension.ReplacementInfo> = emptyList()

    @InputFiles
    var input: SourceDirectorySet? = null

    @OutputDirectory
    val outputDirectory: DirectoryProperty = project.objects.directoryProperty()

    @TaskAction
    fun run() {
        if (input == null) throw NullPointerException("Input source directory set is null")
        if (!outputDirectory.isPresent) throw NullPointerException("Output directory is not set")

        val input = input!!
        var output = outputDirectory.get().asFile

        val pattern = PatternSet()
        pattern.setIncludes(input.includes)
        pattern.setExcludes(input.excludes)

//        if (output.exists()) output.deleteRecursively()
        output.mkdirs()
        output = output.canonicalFile
        println("Output: $output")
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
                    (!allowedFiles.contains(dest.canonicalPath) && disabled)
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
