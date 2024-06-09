package dev.deftu.gradle.bloom

import com.google.common.io.Files
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.util.PatternSet
import java.io.File

open class SourceReplacementTask : DefaultTask() {

    companion object {

        const val BASE_NAME = "bloomReplace"

        @JvmStatic
        fun createJavaTask(
            project: Project,
            extension: BloomExtension,
            sourceSet: SourceSet,
            name: String
        ): SourceReplacementTask {
            val compileTaskName = if (sourceSet.name == "main") "compileJava" else "compile${sourceSet.name.capitalize()}Java"
            return create(project, extension, name, sourceSet.java, "bloom/${sourceSet.name}/java", compileTaskName)
        }

        @JvmStatic
        fun createExtendedTask(
            project: Project,
            extension: BloomExtension,
            sourceSet: SourceSet,
            name: String,
            extensionName: String
        ): SourceReplacementTask {
            val sourceSetExtension = sourceSet.extensions.getByName(extensionName)
            check(sourceSetExtension is SourceDirectorySet) { "Source set extension ($extensionName) is not a SourceDirectorySet (${sourceSetExtension::class.java.name})" }
            val compileTaskName = if (sourceSet.name == "main") "compile${extensionName.capitalize()}" else "compile${sourceSet.name.capitalize()}${extensionName.capitalize()}"
            return create(project, extension, name, sourceSetExtension, "bloom/${sourceSet.name}/$extensionName", compileTaskName)
        }

        private fun create(
            project: Project,
            extension: BloomExtension,
            name: String,
            sourceSet: SourceDirectorySet,
            directory: String,
            compileTaskName: String
        ): SourceReplacementTask {
            val outputDirectory = project.layout.buildDirectory.get().asFile.resolve(directory)
            return project.tasks.register(name, SourceReplacementTask::class.java) { task ->
                task.group = BloomPlugin.PROJECT_NAME.lowercase()

                task.disabled = extension.isDisabled
                task.disabledFiles = extension.disabledFiles
                task.allowedFiles = extension.allowedFiles
                task.replacements = extension.replacements

                task.input = sourceSet
                task.outputDirectory.set(outputDirectory)
            }.get().also { task ->

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

            }
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
    @SkipWhenEmpty
    var input: SourceDirectorySet? = null

    @OutputDirectory
    val outputDirectory: DirectoryProperty = project.objects.directoryProperty()

    @TaskAction
    fun handle() {
        if (replacements.isEmpty()) {
            logger.debug("No replacements to make")
            return
        }

        checkNotNull(input) { "Input source directory set is null" }
        val input = input!!
        check(outputDirectory.isPresent) { "Output directory is not set" }
        val outputDirectory = outputDirectory.asFile.get().canonicalFile

        val pattern = PatternSet()
        pattern.setIncludes(input.includes)
        pattern.setExcludes(input.excludes)

        if (outputDirectory.exists()) outputDirectory.deleteRecursively()
        else check(outputDirectory.mkdirs()) { "Failed to create output directory" }

        for (tree in input.srcDirTrees) {
            val sourceDir = tree.dir.canonicalFile
            if (!sourceDir.exists() || !sourceDir.isDirectory) {
                logger.debug("Skipping invalid directory: {}", sourceDir)
                continue
            }

            val fileTree = project.fileTree(sourceDir).matching(input.filter).matching(pattern)
            for (file in fileTree) {
                val outputtedFile = file.getDestination(sourceDir, outputDirectory)
                if (
                    disabledFiles.contains(outputtedFile.canonicalPath) ||
                    (!allowedFiles.contains(outputtedFile.canonicalPath) && disabled)
                ) {
                    logger.debug("Skipping file: {}", outputtedFile)
                    continue
                } else logger.debug("Processing file: {}", outputtedFile)

                outputtedFile.parentFile.mkdirs()
                outputtedFile.createNewFile()

                var content = Files.asCharSource(file, Charsets.UTF_8).read()
                var isModified = false
                for (replacement in replacements) {
                    if (replacement.path != null && replacement.path != outputtedFile.canonicalPath) continue

                    content = content.replace(replacement.token, replacement.replacement.toString())
                    isModified = true
                }

                if (isModified) {
                    logger.debug("Writing to file: {}", outputtedFile)
                    Files.asCharSink(outputtedFile, Charsets.UTF_8).write(content)
                } else {
                    logger.debug("Copying file: {}", outputtedFile)
                    @Suppress("UnstableApiUsage") Files.copy(file, outputtedFile)
                }
            }
        }
    }

    private fun File.getDestination(directory: File, output: File): File {
        return File(output, canonicalPath.replace(directory.canonicalPath, ""))
    }

}
