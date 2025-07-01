package build

import config.RepoTarget
import dev.aquestry.logger
import java.io.File
import java.lang.ProcessBuilder

fun buildAndPublish(projectDir: File, outputRepo: File, target: RepoTarget) {
    val wrapper = File(projectDir, "gradlew")

    if (wrapper.exists() && !System.getProperty("os.name").startsWith("Windows")) {
        wrapper.setExecutable(true)
    }

    val gradlew = when {
        wrapper.exists() && System.getProperty("os.name").startsWith("Windows") -> "gradlew.bat"
        wrapper.exists() -> "./gradlew"
        else -> "gradle"
    }

    val proc = ProcessBuilder(gradlew, "build")
        .directory(projectDir)
        .inheritIO()
        .start()

    if (proc.waitFor() != 0) return

    val jar = File(projectDir, "build/libs")
        .listFiles()?.firstOrNull { it.extension == "jar" } ?: return

    val groupPath = "com/github/" +
            target.gitUrl.substringAfter("github.com/").substringBeforeLast(".git").lowercase()
    val version = target.tag ?: target.commit ?: "1.0.0"

    val destDir = File(outputRepo, "$groupPath/$version").apply { mkdirs() }
    File(destDir, "${target.name}-$version.jar").apply { jar.copyTo(this, overwrite = true) }
    File(destDir, "${target.name}-$version.pom").writeText("""
        <project xmlns="http://maven.apache.org/POM/4.0.0">
          <modelVersion>4.0.0</modelVersion>
          <groupId>com.github.${target.gitUrl.substringAfter("github.com/").substringBefore("/")}</groupId>
          <artifactId>${target.name}</artifactId>
          <version>$version</version>
        </project>
    """.trimIndent())

    logger.info("Published ${target.name}-$version")
}