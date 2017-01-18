package slak.gitforandroid

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import java.util.*

class MainActivity : AppCompatActivity() {
  companion object {
    const val INTENT_REPO_NAME = "slak.gitforandroid.REPO_NAME"
  }

  internal var repoNames: ArrayList<String> = ArrayList()
  internal var listElements: ArrayAdapter<String>? = null

  private var fab: FloatingActionButton? = null

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
    val toolbar = findViewById(R.id.toolbar) as Toolbar
    setSupportActionBar(toolbar)

    // Force the user to quit if there isn't access to the filesystem
    if (!Repository.isFilesystemAvailable) {
      val fatalError = AlertDialog.Builder(this@MainActivity)
      fatalError
          .setTitle(R.string.error_storage_unavailable)
          .setNegativeButton(R.string.app_quit) { dialog, id ->
            // It is impossible to have more than one Activity on the stack at this point
            // This means the following call terminates the app
            System.exit(1)
          }
          .create()
          .show()
      return
    }

    val repoList = findViewById(R.id.repoList) as ListView
    addRepoNames()
    listElements = ArrayAdapter(this, R.layout.list_element_main, repoNames)
    repoList.adapter = listElements
    repoList.setOnItemClickListener { parent: AdapterView<*>, view, position: Int, id: Long ->
      val repoViewIntent = Intent(this@MainActivity, RepoViewActivity::class.java)
      repoViewIntent.putExtra(INTENT_REPO_NAME, (view as TextView).text.toString())
      startActivity(repoViewIntent)
    }

    fab = findViewById(R.id.fab) as FloatingActionButton
    fab!!.setOnClickListener {
      createRepoDialog(this, fab!!, { newRepoName ->
        repoNames.add(newRepoName)
        listElements!!.notifyDataSetChanged()
      })
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    val id = item.itemId

    if (id == R.id.menu_main_action_settings) {
      val settingsIntent = Intent(this@MainActivity, SettingsActivity::class.java)
      startActivity(settingsIntent)
      return true
    }

    return super.onOptionsItemSelected(item)
  }
}
