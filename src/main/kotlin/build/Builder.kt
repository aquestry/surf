package build

import config.RepoTarget
import dev.aquestry.logger
import java.io.File

fun buildAndPublish(repoDir: File, outputRepo: File, target: RepoTarget) {
    logger.info("Building ${target.name}...")

    val gradlewFile = File(repoDir, "gradlew")
    val gradlewBat = File(repoDir, "gradlew.bat")
    val buildGradle = File(repoDir, "build.gradle")
    val buildGradleKts = File(repoDir, "build.gradle.kts")

    if (!buildGradle.exists() && !buildGradleKts.exists()) {
        throw RuntimeException("No build.gradle or build.gradle.kts found in ${target.name}")
    }

    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    val gradleCmd = when {
        isWindows && gradlewBat.exists() -> gradlewBat.absolutePath
        !isWindows && gradlewFile.exists() -> gradlewFile.absolutePath
        else -> "gradle"
    }

    if (gradlewFile.exists() && !isWindows) {
        val makeExecutable = ProcessBuilder("chmod", "+x", gradlewFile.absolutePath)
            .directory(repoDir)
            .start()
        makeExecutable.waitFor()
    }

    val publishLocalCmd = ProcessBuilder(gradleCmd, "publishToMavenLocal", "--no-daemon", "--continue")
        .directory(repoDir)
        .redirectErrorStream(true)

    val env = publishLocalCmd.environment()
    env["MAVEN_REPO"] = outputRepo.absolutePath
    env["GRADLE_OPTS"] = "-Dorg.gradle.jvmargs=-Xmx2g"

    val process = publishLocalCmd.start()

    process.inputStream.bufferedReader().useLines { lines ->
        lines.forEach { line ->
            logger.info("[${target.name}] $line")
        }
    }

    val exitCode = process.waitFor()
    if (exitCode != 0) {
        throw RuntimeException("Build failed for ${target.name} with exit code $exitCode")
    }

    val userHome = System.getProperty("user.home")
    val m2Repo = File(userHome, ".m2/repository")

    if (m2Repo.exists()) {
        logger.info("Copying artifacts from local Maven repo to output repo...")
        copyRecursively(m2Repo, outputRepo)
    }

    logger.info("Successfully built and published ${target.name}")
}

private fun copyRecursively(source: File, target: File) {
    if (source.isDirectory) {
        target.mkdirs()
        source.listFiles()?.forEach { child ->
            copyRecursively(child, File(target, child.name))
        }
    } else {
        source.copyTo(target, overwrite = true)
    }
}