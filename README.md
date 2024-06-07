Bloom [![License](https://img.shields.io/badge/license-LGPL_v3-blue.svg?style=flat)][lgpl]
=========
Bloom is a Gradle plugin for performing source code token replacements in Java, Kotlin, Scala, and Groovy based projects.

## Usage
Apply the plugin to your project.

```kt
plugins {
  id("dev.deftu.gradle.bloom") version "0.1.0"
}
```

Replacements can be configured through the `BloomExtension`, which can be interacted with using a `bloom {}` block.

### Global Replacement (all files)

Replacing all instances of the string `APPLE` (case-sensitive) with the string `BANANA`, in **all files**.

```kt
bloom {
  replacement("APPLE", "BANANA")
}
```

### Local Replacement (per-file)

Replacing all instances of the string `APPLE` (case-sensitive) with the string `BANANA` in the **specified file(s)**.

```kt
bloom {
  val constantsFile = "src/main/java/org/example/application/Constants.java"
  replacement("APPLE", "BANANA", constantsFile)
}
```

[lgpl]: https://choosealicense.com/licenses/lgpl-3/
