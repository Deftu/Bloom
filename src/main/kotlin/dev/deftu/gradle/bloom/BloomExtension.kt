package dev.deftu.gradle.bloom

interface BloomExtension {

    /**
     * Represents a replacement that should be made in the project's source code.
     *
     * @param token The token to replace.
     * @param replacement The replacement for the token.
     * @param path The path to the file where the replacement should be made. If null, the replacement will be made globally.
     *
     * @since 0.1.0
     * @author Deftu
     */
    data class ReplacementInfo(
        val token: String,
        val replacement: Any,
        val path: String?
    )

    val isDisabled: Boolean
    val disabledFiles: List<String>
    val allowedFiles: List<String>
    val replacements: List<ReplacementInfo>

    /**
     * Disables global replacements.
     *
     * @since 0.1.0
     * @author Deftu
     */
    fun disableGlobalReplacements()

    /**
     * Disables replacements in a specific file.
     *
     * @param path The path to the file to disable replacements in.
     *
     * @since 0.1.0
     * @author Deftu
     */
    fun disableReplacements(path: String)

    /**
     * Allows global replacements in a specific file, only useful when global replacements are disabled or if you'd like to re-enable the file.
     *
     * @param path The path to the file to allow global replacements in.
     *
     * @since 0.1.0
     * @author Deftu
     */
    fun allowFile(path: String)

    /**
     * Adds a replacement to the project.
     *
     * @param token The token to replace.
     * @param replacement The replacement for the token.
     *
     * @since 0.1.0
     * @author Deftu
     */
    fun replacement(
        token: String,
        replacement: Any
    ): ReplacementInfo

    /**
     * Adds a replacement to the project.
     *
     * @param token The token to replace.
     * @param replacement The replacement for the token.
     * @param path The path to the file where the replacement should be made, bypasses global replacements.
     *
     * @since 0.1.0
     * @author Deftu
     */
    fun replacement(
        token: String,
        replacement: Any,
        path: String
    ): ReplacementInfo

}
