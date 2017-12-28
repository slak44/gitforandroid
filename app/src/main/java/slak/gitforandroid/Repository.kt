package slak.gitforandroid

import android.content.Context
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import org.eclipse.jgit.api.CloneCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.CredentialsProvider
import java.io.File
import java.net.URI
import java.util.*

/**
 * Representation of a repository that provides a wrapper over JGit's `Git` and `Repository` classes
 * @param name repository name in storage
 */
class Repository(private val context: Context, name: String) {
  companion object {
    /**
     * Fetch the parent directory for repositories.
     * @return a File object
     */
    fun getRootDirectory(context: Context): File {
      val f = File(context.getExternalFilesDir(null), "repositories")
      f.mkdirs()
      return f
    }

    /**
     * Check availability of the external storage.
     * @return if it's available
     */
    val isFilesystemAvailable: Boolean
      get() = Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
  }

  var alreadyExists: Boolean = false
    private set
  val repoFolder: File
  private var git: Git

  init {
    // The storage dir is a folder called repositories in the private external storage
    val storageDir = File(context.getExternalFilesDir(null), "repositories")
    repoFolder = File(storageDir, name)
    alreadyExists = repoFolder.exists()
    val builder = FileRepositoryBuilder()
    builder.gitDir = File(repoFolder, ".git")
    val internalRepo: org.eclipse.jgit.lib.Repository = builder
        .readEnvironment()
        .findGitDir()
        .build()
    git = Git(internalRepo)
  }

  fun init() = launch(CommonPool) {
    if (alreadyExists) throw IllegalStateException("Repo already exists")
    git.repository.create()
    alreadyExists = true
  }

  fun clone(uri: String) = launch(CommonPool) {
    if (alreadyExists) throw IllegalStateException("Repo already exists")
    val clCom = CloneCommand()
    clCom.setCloneAllBranches(true)
    clCom.setDirectory(repoFolder)
    clCom.setURI(uri)
    clCom.call()
    alreadyExists = true
  }

  fun add(filePatterns: ArrayList<String>) = launch(CommonPool) {
    val aCom = git.add()
    for (pattern in filePatterns) aCom.addFilepattern(pattern)
    aCom.call()
  }

  fun addAll() = launch(CommonPool) {
    val aCom = git.add()
    aCom.addFilepattern(".")
    aCom.call()
  }

  fun removeFromIndex(filePatterns: ArrayList<String>) = launch(CommonPool) {
    val rmCom = git.rm().setCached(true)
    for (pattern in filePatterns) rmCom.addFilepattern(pattern)
    rmCom.call()
  }

  fun delete(filePatterns: ArrayList<String>) = launch(CommonPool) {
    val rmCom = git.rm()
    for (pattern in filePatterns) rmCom.addFilepattern(pattern)
    rmCom.call()
  }

  fun commit(author: PersonIdent?,
             committer: PersonIdent?,
             message: String) = launch(CommonPool) {
    val stored = PersonIdent(
        getStringSetting(this@Repository.context, "git_name"),
        getStringSetting(this@Repository.context, "git_email"))
    git.commit()
        .setAuthor(author ?: stored)
        .setCommitter(committer ?: stored)
        .setMessage(message)
        .call()
  }

  fun commit(committer: PersonIdent?, message: String) = commit(committer, committer, message)

  fun commit(name: String, email: String, message: String): Job {
    val pi: PersonIdent? =
        if (name.isEmpty() && email.isEmpty()) PersonIdent(name, email)
        else null
    return commit(pi, pi, message)
  }

  fun quickCommit() = commit(null, "")

  fun push(remote: String, cp: CredentialsProvider?) = launch(CommonPool) {
    git.push().setRemote(remote).setCredentialsProvider(cp).setPushAll().call()
  }

  fun pull(remote: String, cp: CredentialsProvider?) = launch(CommonPool) {
    git.pull().setRemote(remote).setCredentialsProvider(cp).call()
  }

  fun diff() = async2(CommonPool) {
    git.diff().setCached(false).setShowNameAndStatusOnly(true).call()
  }

  fun listRemotes(): Array<out String> {
    return File(repoFolder, ".git/refs/remotes").list() ?: arrayOf()
  }

  fun relativize(path: URI): URI = repoFolder.toURI().relativize(path)
}
