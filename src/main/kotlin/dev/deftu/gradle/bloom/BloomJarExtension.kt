package dev.deftu.gradle.bloom

open class BloomJarExtension(val bloom: BloomPlugin) : BloomExtension {

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
