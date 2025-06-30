package build

import config.RepoTarget
import logger
import java.io.File
import java.lang.ProcessBuilder

fun buildAndPublish(projectDir: File, outputRepo: File, target: RepoTarget) {
    logger.info("Building ${target.name}...")
    val gradlew = if (System.getProperty("os.name").startsWith("Windows")) "gradlew.bat" else "./gradlew"
    val proc = ProcessBuilder(gradlew, "build")
        .directory(projectDir)
        .inheritIO()
        .start()
    if (proc.waitFor() != 0) return
    val jar = File(projectDir, "build/libs")
        .listFiles()?.firstOrNull { it.extension == "jar" } ?: return
    val groupPath = "com/github/${target.gitUrl.substringAfter("github.com/").substringBefore(".git").lowercase()}"
    val version = when {
        target.tag != null -> target.tag
        target.branch != null && target.commit != null -> "${target.branch}-${target.commit.take(10)}"
        target.commit != null -> target.commit.take(10)
        else -> extractVersion(projectDir) ?: "1.0.0"
    }
    val dest = File(outputRepo, "$groupPath/$version").apply { mkdirs() }
    File(dest, "${target.name}-$version.jar").apply { jar.copyTo(this, overwrite = true) }
    File(dest, "${target.name}-$version.pom").writeText("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"><modelVersion>4.0.0</modelVersion><groupId>com.github.${target.gitUrl.substringAfter("github.com/").substringBefore("/")}</groupId><artifactId>${target.name}</artifactId><version>$version</version></project>")
    logger.info("Published ${target.name}-$version")
}

private fun extractVersion(projectDir: File): String? {
    return listOf("version.txt", "VERSION")
        .map { File(projectDir, it) }
        .firstOrNull { it.exists() }
        ?.readText()?.trim()
}