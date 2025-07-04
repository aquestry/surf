package http

import build.buildAndPublish
import config.RepoTarget
import git.syncRepoTarget
import dev.aquestry.logger
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress

fun startHttpServer(outputRepo: File, baseDir: File, trackedRepos: Collection<RepoTarget>, port: Int = 8080) {
    val server = HttpServer.create(InetSocketAddress(port), 0)
    server.createContext("/") { exchange ->
        val path = exchange.requestURI.path.removePrefix("/")
        val file = File(outputRepo, path)

        if (!file.isFile) {
            val parts = path.split("/")
            if (parts.size >= 5) {
                val user = parts[2]
                val repoName = parts[3]
                val versionStr = parts[4]

                val branch: String?
                val commit: String?
                val tag: String?
                if ("-" in versionStr) {
                    branch = versionStr.substringBefore("-")
                    commit = versionStr.substringAfter("-")
                    tag = null
                } else {
                    branch = null
                    commit = null
                    tag = versionStr
                }

                val gitUrl = "https://github.com/$user/$repoName.git"
                val requestedTarget = RepoTarget(repoName, gitUrl, branch = branch, commit = commit, tag = tag)

                val isTracked = trackedRepos.any { tracked ->
                    tracked.gitUrl == requestedTarget.gitUrl && tracked.name == requestedTarget.name
                }

                if (isTracked) {
                    logger.info("Building on-demand: ${requestedTarget.name}")
                    try {
                        val dir = syncRepoTarget(requestedTarget, baseDir)
                        buildAndPublish(dir, outputRepo, requestedTarget)
                    } catch (e: Exception) {
                        logger.error("Build failed for ${requestedTarget.name}: ${e.message}")
                        exchange.sendResponseHeaders(500, -1)
                        exchange.responseBody.close()
                        return@createContext
                    }
                } else {
                    logger.warn("Requested repo not in tracked list: $gitUrl")
                    exchange.sendResponseHeaders(403, -1)
                    exchange.responseBody.close()
                    return@createContext
                }
            }
        }

        val toServe = File(outputRepo, path)
        if (toServe.exists() && toServe.isFile) {
            exchange.sendResponseHeaders(200, toServe.length())
            toServe.inputStream().use { it.copyTo(exchange.responseBody) }
        } else {
            exchange.sendResponseHeaders(404, -1)
        }
        exchange.responseBody.close()
    }
    server.executor = null
    server.start()
    logger.info("Serving Maven repo at http://localhost:$port/")
}