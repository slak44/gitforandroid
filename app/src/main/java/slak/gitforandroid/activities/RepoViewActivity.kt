package slak.gitforandroid.activities

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import slak.gitforandroid.*
import slak.gitforandroid.filesystem.FSListView

class RepoViewActivity : AppCompatActivity() {
  private var toolbar: Toolbar? = null
  private var repo: Repository? = null

  private var fab: FloatingActionButton? = null
  private var lv: FSListView? = null

  private fun inflateMenu(id: Int) {
    toolbar!!.menu.clear()
    this.menuInflater.inflate(id, toolbar!!.menu)
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

    repo = Repository(this, intent.getStringExtra(MainActivity.INTENT_REPO_NAME))

    lv = findViewById(R.id.current_directory) as FSListView
    lv!!.init(this, repo!!.repoFolder)
    lv!!.onMultiSelectStart = { inflateMenu(R.menu.menu_multi_select) }
    lv!!.onMultiSelectEnd = { inflateMenu(R.menu.menu_repo_view) }

    fab = findViewById(R.id.fab) as FloatingActionButton
    fab!!.setOnClickListener {
      commitDialog(this@RepoViewActivity, repo!!)
    }

    supportActionBar!!.setDisplayHomeAsUpEnabled(true)
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
          val gitPushCb = Repository.callbackFactory(
              fab!!,
              R.string.error_push_failed,
              R.string.snack_item_push_success
          )
          val gitQCommitCb = Repository.callbackFactory(
              fab!!,
              R.string.error_commit_failed,
              R.string.snack_item_commit_success,
              { repo!!.gitPush("origin", pass, gitPushCb) }
          )
          val gitAddCb = Repository.callbackFactory(
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
