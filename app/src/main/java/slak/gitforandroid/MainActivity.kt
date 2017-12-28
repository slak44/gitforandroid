package slak.gitforandroid

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {
  companion object {
    const val INTENT_REPO_NAME = "slak.gitforandroid.REPO_NAME"
  }

  private var repoNames: ArrayList<String> = ArrayList()
  private var listElements: ArrayAdapter<String>? = null

  private fun addRepoNames() {
    repoNames.clear()
    repoNames.addAll(Arrays.asList(*Repository.getRootDirectory(this).list()))
    repoNames.sort()
  }

  override fun onResume() {
    addRepoNames()
    listElements!!.notifyDataSetChanged()
    super.onResume()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)

    Thread.setDefaultUncaughtExceptionHandler {
      thread, throwable -> Log.e("UNCAUGHT DEFAULT", thread.toString(), throwable)
    }

    // Force the user to quit if there isn't access to the filesystem
    if (!Repository.isFilesystemAvailable) {
      val fatalError = AlertDialog.Builder(this@MainActivity)
      fatalError
          .setTitle(R.string.error_storage_unavailable)
          .setNegativeButton(R.string.app_quit) { _, _ ->
            // It is impossible to have more than one Activity on the stack at this point
            // This means the following call terminates the app
            System.exit(1)
          }
          .create()
          .show()
      return
    }

    addRepoNames()
    listElements = ArrayAdapter(this, R.layout.list_element_main, repoNames)
    repoList.adapter = listElements
    repoList.setOnItemClickListener { _, view: View, _, _ ->
      val repoViewIntent = Intent(this@MainActivity, RepoViewActivity::class.java)
      repoViewIntent.putExtra(INTENT_REPO_NAME, (view as TextView).text.toString())
      startActivity(repoViewIntent)
    }

    fab.setOnClickListener {
      createRepoDialog(this, fab, { newRepoName ->
        repoNames.add(newRepoName)
        listElements!!.notifyDataSetChanged()
      })
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == R.id.settings) {
      val settingsIntent = Intent(this@MainActivity, SettingsActivity::class.java)
      startActivity(settingsIntent)
      return true
    }

    return super.onOptionsItemSelected(item)
  }
}
