package dev.deftu.gradle.bloom

import java.util.*

fun String.capitalize(): String = replaceFirstChar { firstChar ->
    if (firstChar.isLowerCase()) firstChar.titlecase(Locale.US) else firstChar.toString()
}
