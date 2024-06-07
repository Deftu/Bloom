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

### But... why fork?

To put it simply, I feel like the new Blossom 2.x.x version is too bloated, slow and abstracts way too far off of the basic functionality it should provide. I also feel as though the maintainers were oftentimes rude, unprofessional and unhelpful. Updates and fixes were non-existant, despite the willingness of the community (specifically me and a few others) to contribute and aide in maintaining it. Pull requests were left open for weeks to months at a time, instead users were met with a "don't use it" attitude. I'm not here to bash the maintainers, but I am here to provide a better alternative to the new Blossom plugin.

[lgpl]: https://choosealicense.com/licenses/lgpl-3/
