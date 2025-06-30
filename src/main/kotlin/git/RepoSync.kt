package git

import config.RepoTarget
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.NoHeadException
import java.io.File

fun syncRepoTarget(target: RepoTarget, baseDir: File): File {
    val dir = baseDir.resolve(target.name)
    if (!dir.exists()) {
        val clone = Git.cloneRepository()
            .setURI(target.gitUrl)
            .setDirectory(dir)
        if (target.branch != null && target.commit == null) {
            clone.setBranch(target.branch)
        }
        clone.call()
    }
    val git = Git.open(dir)
    when {
        target.commit != null -> git.checkout().setName(target.commit).call()
        target.tag    != null -> git.checkout().setName("refs/tags/${target.tag}").call()
        target.branch != null -> {
            try { git.checkout().setName(target.branch).call() } catch (_: Exception) {}
            try { git.pull().call() } catch (_: NoHeadException) {}
        }
        else -> try { git.pull().call() } catch (_: NoHeadException) {}
    }
    return dir
}