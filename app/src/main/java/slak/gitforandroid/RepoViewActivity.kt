package slak.gitforandroid

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import org.eclipse.jgit.diff.DiffEntry
import slak.fslistview.FSListView
import slak.gitforandroid.*
import java.io.File
import java.util.*

class RepoViewActivity : AppCompatActivity() {
  companion object {
    const val FILE_OPEN_REQ_CODE = 0xF11E
  }

  private var toolbar: Toolbar? = null
  private var repo: Repository? = null

  private var fab: FloatingActionButton? = null
  private var lv: FSListView? = null

  private var fileDiffs: List<DiffEntry> = ArrayList()

  private fun inflateMenu(id: Int) {
    toolbar!!.menu.clear()
    this.menuInflater.inflate(id, toolbar!!.menu)
  }

  private fun repoSettingsDialog() {
    val layout = layoutInflater.inflate(R.layout.dialog_repo_settings, null)
    val builder = AlertDialog.Builder(this)
    builder
        .setTitle(R.string.dialog_repo_settings_title)
        .setView(layout)
    val dialog = builder.create()
    dialog.show()
    val renameEditText = layout.findViewById(R.id.dialog_repo_settings_rename) as EditText
    renameEditText.setText(toolbar!!.subtitle)
    (layout.findViewById(R.id.dialog_repo_settings_btn_rename) as Button)
        .setOnClickListener {
          if (renameEditText.text.isBlank()) {
            renameEditText.error = getString(R.string.error_field_blank)
            return@setOnClickListener
          }
          val target = File(Repository.getRepoDirectory(this), renameEditText.text.toString())
          if (target.exists()) {
            renameEditText.error = getString(R.string.error_repo_name_conflict)
            return@setOnClickListener
          }
          val renameResult = repo!!.repoFolder.renameTo(target)
          if (!renameResult) {
            renameEditText.error = getString(R.string.error_rename_failed)
            return@setOnClickListener
          }
          toolbar!!.subtitle = renameEditText.text
          dialog.dismiss()
        }
    val deleteBtn = layout.findViewById(R.id.dialog_repo_settings_btn_delete) as Button
    (layout.findViewById(R.id.dialog_repo_settings_sw_enable_delete) as Switch)
        .setOnCheckedChangeListener { button, isChecked ->
          deleteBtn.isEnabled = isChecked
        }
    deleteBtn.setOnClickListener {
      repo!!.repoFolder.deleteRecursively()
      this@RepoViewActivity.finish()
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    inflateMenu(R.menu.menu_repo_view)
    return true
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_repo_view)
    toolbar = findViewById(R.id.toolbar) as Toolbar
    setSupportActionBar(toolbar)

    val repoName = intent.getStringExtra(MainActivity.INTENT_REPO_NAME)
    toolbar!!.subtitle = repoName
    repo = Repository(this, repoName)

    lv = findViewById(R.id.current_directory) as FSListView
    lv!!.onMultiSelectStart = { inflateMenu(R.menu.menu_multi_select) }
    lv!!.onMultiSelectEnd = { inflateMenu(R.menu.menu_repo_view) }

    lv!!.onChildViewPrepare = cb@ {
      context: AppCompatActivity, file: File, convertView: View?, parent: ViewGroup ->
      val path = repo!!.relativize(file.toURI()).toString()
      val newGitStatus: GitStatus = fileDiffs
          .firstOrNull { it.newPath == path }
          ?.let { GitStatus.from(it.changeType) }
          ?: GitStatus.NONE
      if (convertView != null && convertView is RepoListItemView &&
          convertView.gitStatus == newGitStatus) {
        return@cb convertView
      }
      val view = context.layoutInflater.inflate(R.layout.list_element, parent, false)
          as RepoListItemView
      view.gitStatus = newGitStatus
      return@cb view
    }

    val emptyFolderText = findViewById(R.id.repo_view_empty_folder)!!
    lv!!.onFolderChange = { old, new ->
      if (new.list().isEmpty()) {
        emptyFolderText.visibility = View.VISIBLE
      } else {
        emptyFolderText.visibility = View.GONE
      }
    }

    lv!!.onFileOpen = { file ->
      val intent = Intent(Intent.ACTION_VIEW)
      val uri = Uri.parse("content://" + file.absolutePath)
      intent.setDataAndType(uri, "text/plain")
      startActivityForResult(intent, FILE_OPEN_REQ_CODE)
    }

    repo!!.gitDiff { diffs ->
      fileDiffs = diffs
      lv!!.init(this, repo!!.repoFolder, R.layout.list_element, R.color.colorSelected)
    }

    fab = findViewById(R.id.fab) as FloatingActionButton
    fab!!.setOnClickListener {
      commitDialog(this@RepoViewActivity, repo!!)
    }

    supportActionBar!!.setDisplayHomeAsUpEnabled(true)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == FILE_OPEN_REQ_CODE) {
      repo!!.gitDiff { diffs ->
        fileDiffs = diffs
        lv!!.update()
      }
      return
    }
    super.onActivityResult(requestCode, resultCode, data)
  }

  private fun stageSelected() {
    repo!!.gitAdd(lv!!.selectedPaths, Repository.callbackFactory(
        fab!!,
        R.string.error_add_failed,
        R.string.snack_item_stage_success
    ))
  }

  private fun unstageSelected() {
    repo!!.gitRm(lv!!.selectedPaths, Repository.callbackFactory(
        fab!!,
        R.string.error_rm_failed,
        R.string.snack_item_unstage_success
    ))
  }

  private fun deleteSelected() {
    repo!!.gitDelete(lv!!.selectedPaths, Repository.callbackFactory(
        fab!!,
        R.string.error_delete_failed,
        R.string.snack_item_delete_success
    ))
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      android.R.id.home -> onBackPressed() // Behave like the hardware back button
      R.id.menu_repo_view_action_settings -> repoSettingsDialog()
      R.id.menu_repo_view_action_push -> pushPullDialog(this, repo!!, RemoteOp.PUSH)
      R.id.menu_repo_view_action_pull -> pushPullDialog(this, repo!!, RemoteOp.PULL)
      R.id.menu_repo_view_action_stage -> stageSelected()
      R.id.menu_repo_view_action_unstage -> unstageSelected()
      R.id.menu_repo_view_action_delete -> deleteSelected()
      R.id.menu_repo_view_action_add_all -> repo!!.gitAddAll(Repository.callbackFactory(
          fab!!, R.string.error_add_failed, R.string.snack_item_stage_all_success))
      R.id.menu_repo_view_action_quick_commit -> {
        // 1. Ask for password
        // 2. Stage everything
        // 3. Commit
        // 4. Push to origin
        passwordDialog(this, { pass: String ->
          val gitPushCb = Repository.Companion.callbackFactory(
              fab!!,
              R.string.error_push_failed,
              R.string.snack_item_push_success
          )
          val gitQCommitCb = Repository.Companion.callbackFactory(
              fab!!,
              R.string.error_commit_failed,
              R.string.snack_item_commit_success,
              { repo!!.gitPush("origin", pass, gitPushCb) }
          )
          val gitAddCb = Repository.Companion.callbackFactory(
              fab!!,
              R.string.error_add_failed,
              R.string.snack_item_stage_all_success,
              { repo!!.gitQuickCommit(gitQCommitCb) }
          )
          repo!!.gitAddAll(gitAddCb)
        })
      }
      else -> return super.onOptionsItemSelected(item)
    }
    return true
  }

  // Override back button so it traverses the folder structure before exiting the activity
  override fun onBackPressed() {
    val wentUp = lv!!.goUp()
    if (!wentUp) super.onBackPressed()
  }
}
