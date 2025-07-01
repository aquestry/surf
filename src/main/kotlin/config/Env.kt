package config

data class RepoTarget(
    val name: String,
    val gitUrl: String,
    val branch: String? = null,
    val commit: String? = null,
    val tag: String? = null
)

fun loadTrackedRepos(): List<RepoTarget> {
    return System.getenv()
        .filterKeys { it.startsWith("SURF_REPO_") }
        .values
        .mapNotNull { parseRepoTarget(it) }
}

private fun parseRepoTarget(input: String): RepoTarget? {
    val cleanInput = input.removePrefix("https://").removePrefix("http://")

    if (!cleanInput.startsWith("github.com/")) return null

    val mainPart = cleanInput.substringAfter("github.com/")
    val (path, ref) = mainPart.split(":", limit = 2).let {
        it.first() to it.getOrNull(1)
    }

    val repoName = path.substringAfterLast("/").removeSuffix(".git")
    val gitUrl = "https://github.com/${path.removeSuffix(".git")}.git"

    return when {
        ref == null -> RepoTarget(repoName, gitUrl)
        "-" in ref -> {
            val (branch, commit) = ref.split("-", limit = 2)
            RepoTarget(repoName, gitUrl, branch = branch, commit = commit)
        }
        else -> RepoTarget(repoName, gitUrl, tag = ref)
    }
}