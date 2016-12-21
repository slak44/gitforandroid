package slak.gitforandroid

import android.content.Context
import android.os.Environment
import android.preference.PreferenceManager
import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View

import org.eclipse.jgit.api.CloneCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.WrongRepositoryStateException
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import slak.gitforandroid.activities.reportError
import slak.gitforandroid.activities.rootActivityView

import java.io.File
import java.io.IOException
import java.util.ArrayList

class Repository(private val context: AppCompatActivity, name: String) {
  companion object {
    fun getRepoDirectory(currentActivity: Context): File {
      val f = File(currentActivity.getExternalFilesDir(null), "repositories")
      f.mkdirs()
      return f
    }

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
  private var git: Git? = null

  init {
    // The storage dir is a folder called repositories in the private external storage
    val storageDir = File(context.getExternalFilesDir(null), "repositories")
    repoFolder = File(storageDir, name)
    alreadyExists = repoFolder.exists()
    val builder = FileRepositoryBuilder()
    try {
      builder.gitDir = File(repoFolder, ".git")
      val internalRepo = builder
          .readEnvironment()
          .findGitDir()
          .build()
      git = Git(internalRepo)
    } catch (ioEx: IOException) {
      reportError(context, R.string.error_io_failed, ioEx, "WTF")
    }
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
      git!!.repository.create()
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
      val aCom = git!!.add()
      for (pattern in filePatterns) aCom.addFilepattern(pattern)
      aCom.call()
    }, callback).execute()
  }

  fun gitAddAll(callback: (SafeAsyncTask) -> Unit) {
    SafeAsyncTask({
      val aCom = git!!.add()
      aCom.addFilepattern(".")
      aCom.call()
    }, callback).execute()
  }

  fun gitCommit(
      author: PersonIdent?,
      committer: PersonIdent?,
      message: String,
      callback: (SafeAsyncTask) -> Unit
  ) {
    // FIXME: this kind of retarded settings access i want abstracted
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val stored = PersonIdent(
        prefs.getString("git_name", "")!!, prefs.getString("git_email", "")!!)
    SafeAsyncTask({
      git!!.commit()
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
    val pi: PersonIdent?
    if (name == null || email == null)
      pi = null
    else
      pi = PersonIdent(name, email)
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
      val prefs = PreferenceManager.getDefaultSharedPreferences(context)
      val upcp = UsernamePasswordCredentialsProvider(
          prefs.getString("git_username", ""),
          password)
      git!!.push().setRemote(remote).setCredentialsProvider(upcp).setPushAll().call()
    }, callback).execute()
  }

  fun gitPull(
      remote: String,
      password: String,
      callback: (SafeAsyncTask) -> Unit
  ) {
    SafeAsyncTask({
      val prefs = PreferenceManager.getDefaultSharedPreferences(context)
      val upcp = UsernamePasswordCredentialsProvider(
          prefs.getString("git_username", ""),
          password)
      git!!.pull().setRemote(remote).setCredentialsProvider(upcp).call()
    }, callback).execute()
  }

  fun listRemotes(): Array<out String> {
    return File(repoFolder, ".git/refs/remotes").list()
  }
}