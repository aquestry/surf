package dev.aquestry

import config.loadTrackedRepos
import http.startHttpServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

lateinit var logger: Logger

fun main() {
    logger = LoggerFactory.getLogger("Surf")
    logger.info("Surf is starting...")
    val outputRepo = File("maven-repo").apply { mkdirs() }
    val baseDir = File("repos").apply { mkdirs() }
    val trackedRepos = loadTrackedRepos()
    startHttpServer(outputRepo, baseDir, trackedRepos)
    logger.info("Server started - builds will happen on demand")
}