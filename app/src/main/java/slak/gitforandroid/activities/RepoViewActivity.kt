package slak.gitforandroid.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import slak.gitforandroid.*
import java.io.File
import java.util.*

class RepoViewActivity : AppCompatActivity() {
  private var nodes: ArrayList<SelectableAdapterModel<File>> = ArrayList()
  private var listElements: ArrayAdapter<SelectableAdapterModel<File>>? = null
  private var fileStack = Stack<SelectableAdapterModel<File>>()
  private var toolbar: Toolbar? = null
  private var repo: Repository? = null

  internal var multiSelectState = false

  private var fab: FloatingActionButton? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_repo_view)
    toolbar = findViewById(R.id.toolbar) as Toolbar
    setSupportActionBar(toolbar)

    val startIntent = intent
    repo = Repository(this, startIntent.getStringExtra(MainActivity.INTENT_REPO_NAME))

    val currentDir = findViewById(R.id.current_directory) as ListView
    nodes = ArrayList<SelectableAdapterModel<File>>()
    nodes.addAll(SelectableAdapterModel.fromArray(repo!!.repoFolder.listFiles()))
    fileStack.push(SelectableAdapterModel(repo!!.repoFolder))
    listElements = object :
        ArrayAdapter<SelectableAdapterModel<File>>(this, R.layout.list_element_main, nodes) {
      override fun getViewTypeCount(): Int {
        return 3
      }

      override fun getItemViewType(position: Int): Int {
        if (nodes.size == 0) return 2
        else if (nodes[position].thing.isDirectory) return 0
        else if (nodes[position].thing.isFile) return 1
        else return 1
      }

      override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        if (this.getItemViewType(position) == 2) {
          return layoutInflater.inflate(R.layout.list_element_empty_folder_repo_view, parent, false)
        }
        val nodeName = nodes[position].thing.name
        val nodeView: TextView
        val whatNode =
            if (this.getItemViewType(position) == 0) R.id.folder_element else R.id.file_element
        // If convertView has the correct layout and type, reuse it
        // Otherwise, inflate new layout
        if (convertView != null && convertView is TextView
            && whatNode == convertView.id) {
          nodeView = convertView
        } else {
          val layoutToUse = if (this.getItemViewType(position) == 0)
              R.layout.list_element_folder_repo_view else R.layout.list_element_file_repo_view
          nodeView = layoutInflater.inflate(layoutToUse, parent, false) as TextView
        }
        nodeView.text = nodeName
        return nodeView
      }

      override fun getCount(): Int {
        if (nodes.size == 0)
          return 1
        else
          return super.getCount()
      }
    }
    currentDir.adapter = listElements
    currentDir.onItemLongClickListener = AdapterView.OnItemLongClickListener {
      parent, view, position, id ->
      multiSelectState = true
      // No item was checked: style the toolbar for multi-select
      if (SelectableAdapterModel.getSelectedModels(nodes).size == 0)
        inflateMenu(R.menu.menu_multi_select)
      nodes[position].setSelectStatus(true)
      view.setBackgroundColor(
          resources.getColor(R.color.colorSelected, this@RepoViewActivity.theme))
      true
    }
    currentDir.setOnItemClickListener listener@ {
      parent: AdapterView<*>, view: View, position: Int, id: Long ->
      // Directory empty, nothing to do here
      if (nodes.size == 0) return@listener
      // Clicked on a selected item: deselect it
      if (nodes[position].isSelected && multiSelectState) {
        nodes[position].setSelectStatus(false)
        view.background = currentDir.background
        // No more items are selected: reset the style of the toolbar
        if (SelectableAdapterModel.getSelectedModels(nodes).size == 0) {
          multiSelectState = false
          inflateMenu(R.menu.menu_repo_view)
        }
        return@listener
        // Clicked on a unselected item in multi-select: select it
      } else if (multiSelectState) {
        nodes[position].setSelectStatus(true)
        view.setBackgroundColor(
            resources.getColor(R.color.colorSelected, this@RepoViewActivity.theme))
        return@listener
      }
      // Entering a new folder
      if (view.id == R.id.folder_element) {
        fileStack.push(nodes[position])
        nodes.clear()
        nodes.addAll(SelectableAdapterModel.fromArray(fileStack.peek().thing.listFiles()))
        listElements!!.notifyDataSetChanged()
      } else if (view.id == R.id.file_element) {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = Uri.parse("content://" + nodes[position].thing.absolutePath)
        intent.setDataAndType(uri, "text/plain")
        startActivity(intent)
      }
    }

    fab = findViewById(R.id.fab) as FloatingActionButton
    fab!!.setOnClickListener {
      commitDialog(this@RepoViewActivity, repo!!)
    }

    supportActionBar!!.setDisplayHomeAsUpEnabled(true)
  }

  private fun inflateMenu(id: Int) {
    toolbar!!.menu.clear()
    this.menuInflater.inflate(id, toolbar!!.menu)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    inflateMenu(R.menu.menu_repo_view)
    return true
  }

  private fun stageSelected() {
    val paths = SelectableAdapterModel.getSelectedModels(nodes).mapTo(ArrayList<String>()) {
      nodes[it].thing.toURI().relativize(repo!!.repoFolder.toURI()).toString()
    }
    repo!!.gitAdd(paths, Repository.callbackFactory(
        fab!!,
        R.string.error_add_failed,
        R.string.snack_item_stage_success
    ))
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      // Behave like the hardware back button
      onBackPressed()
      return true // Event consumed
    } else if (item.itemId == R.id.menu_repo_view_action_push) {
      pushPullDialog(this, repo!!, RemoteOp.PUSH)
    } else if (item.itemId == R.id.menu_repo_view_action_pull) {
      pushPullDialog(this, repo!!, RemoteOp.PULL)
    } else if (item.itemId == R.id.menu_repo_view_action_quick_commit) {
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
      return true
    } else if (item.itemId == R.id.menu_repo_view_action_stage) {
      stageSelected()
      return true
    } else if (item.itemId == R.id.menu_repo_view_action_unstage) {
      // TODO: unstage
    } else if (item.itemId == R.id.menu_repo_view_action_delete) {
      // TODO: delete
    }
    return super.onOptionsItemSelected(item)
  }

  // Override back button so it traverses the folder structure before exiting the activity
  override fun onBackPressed() {
    if (fileStack.size <= 1) {
      fileStack = Stack<SelectableAdapterModel<File>>()
      super.onBackPressed()
    } else {
      fileStack.pop()
      nodes.clear()
      nodes.addAll(SelectableAdapterModel.fromArray(fileStack.peek().thing.listFiles()))
      listElements!!.notifyDataSetChanged()
    }
  }
}
