package slak.gitforandroid

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_repo_view.*
import kotlinx.android.synthetic.main.dialog_repo_settings.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import slak.fslistview.FSListView
import java.io.File
import java.util.*

class RepoViewActivity : AppCompatActivity() {
  companion object {
    const val FILE_OPEN_REQ_CODE = 0xF11E
  }

  private lateinit var repo: Repository
  private var fileDiffs: List<DiffEntry> = ArrayList()

  private fun inflateMenu(id: Int) {
    toolbar.menu.clear()
    this.menuInflater.inflate(id, toolbar.menu)
  }

  private fun repoSettingsDialog() {
    val layout = layoutInflater.inflate(R.layout.dialog_repo_settings, null)
    val builder = AlertDialog.Builder(this)
    builder
        .setTitle(R.string.dialog_repo_settings_title)
        .setView(layout)
    val dialog = builder.create()
    dialog.show()
    renameRepo.setText(toolbar.subtitle)
    renameRepoBtn.setOnClickListener {
      if (renameRepo.text.isBlank()) {
        renameRepo.error = getString(R.string.error_field_blank)
        return@setOnClickListener
      }
      val target = File(Repository.getRootDirectory(this), renameRepo.text.toString())
      if (target.exists()) {
        renameRepo.error = getString(R.string.error_repo_name_conflict)
        return@setOnClickListener
      }
      val renameResult = repo.repoFolder.renameTo(target)
      if (!renameResult) {
        renameRepo.error = getString(R.string.error_rename_failed)
        return@setOnClickListener
      }
      toolbar.subtitle = renameRepo.text
      dialog.dismiss()
    }
    enableDeleteSwitch.setOnCheckedChangeListener { _, isChecked ->
      deleteRepoBtn.isEnabled = isChecked
    }
    deleteRepoBtn.setOnClickListener {
      repo.repoFolder.deleteRecursively()
      this@RepoViewActivity.finish()
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    inflateMenu(R.menu.menu_repo_view)
    return true
  }

  private fun getDiffs() = launch(CommonPool) {
    try {
      fileDiffs = repo.diff().await()
    } catch (e: GitAPIException) {
      e.printStackTrace()
      Snackbar.make(fab, R.string.error_diff_failed, Snackbar.LENGTH_LONG)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_repo_view)
    setSupportActionBar(toolbar)
    supportActionBar!!.setDisplayHomeAsUpEnabled(true)

    val repoName = intent.getStringExtra(MainActivity.INTENT_REPO_NAME)
    repo = Repository(this, repoName)
    toolbar.subtitle = repoName

    fileList.onMultiSelectStart = { inflateMenu(R.menu.menu_multi_select) }
    fileList.onMultiSelectEnd = { inflateMenu(R.menu.menu_repo_view) }

    fileList.onChildViewPrepare = cb@ {
      context: Context, file: File, convertView: View?, parent: ViewGroup ->
      val path = repo.relativize(file.toURI()).toString()
      val newGitStatus: GitStatus = fileDiffs
          .firstOrNull { it.newPath == path }
          ?.let { GitStatus.from(it.changeType) }
          ?: GitStatus.NONE
      if (convertView != null && convertView is RepoListItemView &&
          convertView.gitStatus == newGitStatus) {
        return@cb convertView
      }
      val view = LayoutInflater.from(context).inflate(R.layout.list_element, parent, false)
          as RepoListItemView
      view.gitStatus = newGitStatus
      return@cb view
    }

    fileList.onFolderChange = { _, new ->
      emptyFolder.visibility = if (new.list().isEmpty()) View.VISIBLE else View.GONE
    }

    fileList.onFileOpen = { file ->
      val intent = Intent(Intent.ACTION_VIEW)
      val uri = Uri.parse("content://" + file.absolutePath)
      intent.setDataAndType(uri, "text/plain")
      startActivityForResult(intent, FILE_OPEN_REQ_CODE)
    }

    fab.setOnClickListener { commitDialog(this@RepoViewActivity, repo, fab) }

    launch(UI) {
      getDiffs().join()
      fileList.init(this@RepoViewActivity,
          repo.repoFolder, R.layout.list_element, R.color.colorSelected)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == FILE_OPEN_REQ_CODE) {
      runBlocking { getDiffs().join() }
      fileList.update()
      return
    }
    super.onActivityResult(requestCode, resultCode, data)
  }

  private fun openFileManager(target: File) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(Uri.parse(target.absolutePath), "*/*")
    intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
        "vnd.android.cursor.item/file",
        "vnd.android.cursor.item/directory",
        "vnd.android.cursor.dir/*",
        "resource/folder",
        "inode/directory",
        "x-directory/normal"
    ))
    startActivity(Intent.createChooser(intent, getString(R.string.intent_open_manager_chooser)))
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    for (item in arrayOf(R.id.push, R.id.pull, R.id.stage, R.id.unstage, R.id.delete)) {
      menu.findItem(item)?.iconTint(this, R.color.white)
    }
    return super.onPrepareOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      android.R.id.home -> onBackPressed() // Behave like the hardware back button
      R.id.open_file_manager -> openFileManager(fileList.currentDirectory)
      R.id.settings -> repoSettingsDialog()
      R.id.push -> pushPullDialog(this, fab, repo, RemoteOp.PUSH)
      R.id.pull -> pushPullDialog(this, fab, repo, RemoteOp.PULL)
      R.id.stage -> repo.add(fileList.selectedPaths).withSnackResult(fab,
          R.string.snack_item_stage_success, R.string.error_add_failed)
      R.id.unstage -> repo.removeFromIndex(fileList.selectedPaths).withSnackResult(fab,
          R.string.snack_item_unstage_success, R.string.error_rm_failed)
      R.id.delete -> repo.delete(fileList.selectedPaths).withSnackResult(fab,
          R.string.snack_item_delete_success, R.string.error_delete_failed)
      R.id.stage_all -> repo.addAll().withSnackResult(fab,
          R.string.snack_item_stage_all_success, R.string.error_add_failed)
      R.id.quick_commit -> {
        // 1. Ask for password
        // 2. Stage everything
        // 3. Commit
        // 4. Push to origin
        passwordDialog(this, { pass: String -> launch(UI) {
          repo.addAll().withSnackResult(fab,
              R.string.snack_item_stage_all_success, R.string.error_add_failed).join()
          repo.quickCommit().withSnackResult(fab,
              R.string.snack_item_commit_success, R.string.error_commit_failed).join()
          repo.push("origin", UsernamePasswordCredentialsProvider("", pass))
              .withSnackResult(fab,
                  resources.getString(R.string.snack_item_push_success, "origin"),
                  resources.getString(R.string.error_push_failed))
        } })
      }
      else -> return super.onOptionsItemSelected(item)
    }
    return true
  }

  // Override back button so it traverses the folder structure before exiting the activity
  override fun onBackPressed() {
    val wentUp = fileList.goUp()
    if (!wentUp) super.onBackPressed()
  }
}
