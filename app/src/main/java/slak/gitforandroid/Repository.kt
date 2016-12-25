package slak.gitforandroid

import android.content.Context
import android.os.Environment
import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import org.eclipse.jgit.api.CloneCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import slak.gitforandroid.activities.getStringSetting
import slak.gitforandroid.activities.reportError
import slak.gitforandroid.activities.rootActivityView
import java.io.File
import java.net.URI
import java.util.*

/**
 * Representation of a repository that provides a wrapper over JGit's `Git` and `Repository` classes
 * @param name repository name in storage
 */
class Repository(private val context: AppCompatActivity, name: String) {
  companion object {
    fun getRepoDirectory(currentActivity: Context): File {
      val f = File(currentActivity.getExternalFilesDir(null), "repositories")
      f.mkdirs()
      return f
    }

    /**
     * Returns a callback that alerts the user of the task result.
     * @param view to attach snackbar to
     * @param fail snackbar text for error
     * @param success snackbar text for success
     * @param next optional function to run only after success
     */
    fun callbackFactory(
        view: View,
        @StringRes fail: Int,
        @StringRes success: Int,
        next: () -> Unit = {}
    ): (SafeAsyncTask) -> Unit {
      return cb@ { completedTask: SafeAsyncTask ->
        if (completedTask.exception != null) {
          reportError(view, fail, completedTask.exception!!)
          return@cb
        }
        Snackbar.make(view, success, Snackbar.LENGTH_LONG).show()
        next()
      }
    }

    val isFilesystemAvailable: Boolean
      get() = Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
  }

  private var alreadyExists: Boolean = false
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

  fun gitInit(callback: (SafeAsyncTask) -> Unit) {
    SafeAsyncTask({
      if (alreadyExists) {
        Snackbar.make(
            rootActivityView(context),
            R.string.error_repo_name_conflict,
            Snackbar.LENGTH_LONG
        ).show()
        return@SafeAsyncTask
      }
      git.repository.create()
      alreadyExists = true
    }, callback).execute()
  }

  fun gitClone(uri: String, callback: (SafeAsyncTask) -> Unit) {
    SafeAsyncTask({
      if (alreadyExists) {
        Snackbar.make(
            rootActivityView(context),
            R.string.error_repo_name_conflict,
            Snackbar.LENGTH_LONG
        ).show()
        return@SafeAsyncTask
      }
      val clCom = CloneCommand()
      clCom.setCloneAllBranches(true)
      clCom.setDirectory(repoFolder)
      clCom.setURI(uri)
      clCom.call()
      alreadyExists = true
    }, callback).execute()
  }

  fun gitAdd(filePatterns: ArrayList<String>, callback: (SafeAsyncTask) -> Unit) {
    SafeAsyncTask({
      val aCom = git.add()
      for (pattern in filePatterns) aCom.addFilepattern(pattern)
      aCom.call()
    }, callback).execute()
  }

  fun gitAddAll(callback: (SafeAsyncTask) -> Unit) {
    SafeAsyncTask({
      val aCom = git.add()
      aCom.addFilepattern(".")
      aCom.call()
    }, callback).execute()
  }

  fun gitRm(filePatterns: ArrayList<String>, callback: (SafeAsyncTask) -> Unit) {
    SafeAsyncTask({
      val rmCom = git.rm().setCached(true)
      for (pattern in filePatterns) rmCom.addFilepattern(pattern)
      rmCom.call()
    }, callback).execute()
  }

  fun gitDelete(filePatterns: ArrayList<String>, callback: (SafeAsyncTask) -> Unit) {
    SafeAsyncTask({
      val rmCom = git.rm()
      for (pattern in filePatterns) rmCom.addFilepattern(pattern)
      rmCom.call()
    }, callback).execute()
  }

  fun gitCommit(
      author: PersonIdent?,
      committer: PersonIdent?,
      message: String,
      callback: (SafeAsyncTask) -> Unit
  ) {
    val stored = PersonIdent(
        getStringSetting(context, "git_name"), getStringSetting(context, "git_email"))
    SafeAsyncTask({
      git.commit()
          .setAuthor(author ?: stored)
          .setCommitter(committer ?: stored)
          .setMessage(message)
          .call()
    }, callback).execute()
  }

  fun gitCommit(
      committer: PersonIdent?,
      message: String,
      callback: (SafeAsyncTask) -> Unit
  ) {
    gitCommit(committer, committer, message, callback)
  }

  fun gitCommit(
      name: String?,
      email: String?,
      message: String,
      callback: (SafeAsyncTask) -> Unit
  ) {
    val pi: PersonIdent? =
        if (name != null && email != null) PersonIdent(name, email)
        else null
    gitCommit(pi, pi, message, callback)
  }

  fun gitQuickCommit(callback: (SafeAsyncTask) -> Unit) {
    gitCommit(null, "", callback)
  }

  fun gitPush(
      remote: String,
      password: String,
      callback: (SafeAsyncTask) -> Unit
  ) {
    SafeAsyncTask({
      val upcp = UsernamePasswordCredentialsProvider(
          getStringSetting(context, "git_username"),
          password
      )
      git.push().setRemote(remote).setCredentialsProvider(upcp).setPushAll().call()
    }, callback).execute()
  }

  fun gitPull(
      remote: String,
      password: String,
      callback: (SafeAsyncTask) -> Unit
  ) {
    SafeAsyncTask({
      val upcp = UsernamePasswordCredentialsProvider(
          getStringSetting(context, "git_username"),
          password
      )
      git.pull().setRemote(remote).setCredentialsProvider(upcp).call()
    }, callback).execute()
  }

  fun gitDiff(callback: (List<DiffEntry>) -> Unit) {
    var diffs: List<DiffEntry>? = null
    SafeAsyncTask({
      diffs = git.diff().setCached(false).setShowNameAndStatusOnly(true).call()
    }, { completedTask: SafeAsyncTask ->
      if (completedTask.exception != null) {
        reportError(context, R.string.error_diff_failed, completedTask.exception!!)
        return@SafeAsyncTask
      }
      callback(diffs!!)
    }).execute()
  }

  fun listRemotes(): Array<out String> {
    return File(repoFolder, ".git/refs/remotes").list() ?: arrayOf()
  }

  fun relativize(path: URI): URI {
    return repoFolder.toURI().relativize(path)
  }
}
