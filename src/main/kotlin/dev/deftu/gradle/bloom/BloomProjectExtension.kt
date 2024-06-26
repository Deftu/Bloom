package dev.deftu.gradle.bloom

/**
 * The project-wide extension for Bloom, which can be configured in the `bloom` block in the `build.gradle.kts` file.
 *
 * @since 0.1.0
 * @author Deftu
 */
open class BloomProjectExtension(val bloom: BloomPlugin) : BloomExtension {

    final override var isDisabled = false
        private set
    override val disabledFiles = mutableListOf<String>()
    override val allowedFiles = mutableListOf<String>()
    override val replacements = mutableListOf<BloomExtension.ReplacementInfo>()

    override fun disableGlobalReplacements() {
        isDisabled = true
    }

    override fun disableReplacements(path: String) {
        disabledFiles.add(path)
        allowedFiles.remove(path)
    }

    override fun allowFile(path: String) {
        allowedFiles.add(path)
        disabledFiles.remove(path)
    }

    override fun replacement(
        token: String,
        replacement: Any
    ): BloomExtension.ReplacementInfo {
        val info = BloomExtension.ReplacementInfo(token, replacement, null)
        replacements.add(info)
        return info
    }

    override fun replacement(
        token: String,
        replacement: Any,
        path: String
    ): BloomExtension.ReplacementInfo {
        val info = BloomExtension.ReplacementInfo(token, replacement, path)
        replacements.add(info)
        return info
    }

}
