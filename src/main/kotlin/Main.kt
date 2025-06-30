import config.loadTrackedRepos
import build.buildAndPublish
import git.syncRepoTarget
import http.startHttpServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

lateinit var logger: Logger

fun main() {
    logger = LoggerFactory.getLogger("Surf")
    val outputRepo = File("maven-repo").apply { mkdirs() }
    val baseDir    = File("repos").apply { mkdirs() }
    val repoTargets = loadTrackedRepos()
    repoTargets
        .filter { it.tag == null && it.commit == null }
        .forEach { target ->
            val dir = syncRepoTarget(target, baseDir)
            buildAndPublish(dir, outputRepo, target)
        }
    startHttpServer(outputRepo, baseDir)
}