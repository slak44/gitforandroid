package slak.gitforandroid

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.StringRes
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import slak.gitforandroid.R
import slak.gitforandroid.Repository
import java.util.*

fun reportError(view: View, @StringRes strRes: Int, e: Exception, type: String = "Error") {
  when (type) {
    "WTF" -> Log.wtf("GitForAndroid", view.resources.getString(strRes), e)
    else -> Log.e("GitForAndroid", view.resources.getString(strRes), e)
  }
  // TODO: make 'more' button on snack, to lead to err text
  Snackbar.make(view, strRes, Snackbar.LENGTH_LONG).show()
}

fun reportError(activity: AppCompatActivity, @StringRes strRes: Int, e: Exception, type: String = "Error") {
  reportError(rootActivityView(activity), strRes, e, type)
}

fun rootActivityView(activity: AppCompatActivity): View {
  return activity.window.decorView.rootView
}

fun getStringSetting(context: Context, key: String): String {
  return PreferenceManager.getDefaultSharedPreferences(context).getString(key, "")
}

class MainActivity : AppCompatActivity() {
  companion object {
    const val INTENT_REPO_NAME = "slak.gitforandroid.REPO_NAME"
  }

  internal var repoNames: ArrayList<String> = ArrayList()
  internal var listElements: ArrayAdapter<String>? = null

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
    repoNames = ArrayList<String>()
    repoNames.addAll(Arrays.asList(*Repository.getRepoDirectory(this).list()))
    listElements = ArrayAdapter(this, R.layout.list_element_main, repoNames)
    repoList.adapter = listElements
    repoList.setOnItemClickListener { parent: AdapterView<*>, view, position: Int, id: Long ->
      val repoViewIntent = Intent(this@MainActivity, RepoViewActivity::class.java)
      repoViewIntent.putExtra(INTENT_REPO_NAME, (view as TextView).text.toString())
      startActivity(repoViewIntent)
    }

    val fab = findViewById(R.id.fab) as FloatingActionButton
    fab.setOnClickListener {
      val newRepo = AlertDialog.Builder(this@MainActivity)
      val inflater = this@MainActivity.layoutInflater
      val dialogView = inflater.inflate(R.layout.dialog_add_repo, null)

      val toClone = dialogView.findViewById(R.id.repo_add_dialog_clone) as Switch
      toClone.setOnCheckedChangeListener { btn: CompoundButton, isChecked: Boolean ->
        dialogView.findViewById(R.id.repo_add_dialog_cloneURL).visibility =
            if (isChecked) View.VISIBLE else View.GONE
      }

      val dialog = newRepo
          .setTitle(R.string.dialog_add_repo_title)
          .setView(dialogView)
          // This listener is set below
          .setPositiveButton(R.string.dialog_add_repo_confirm, null)
          // Dismiss automatically
          .setNegativeButton(R.string.dialog_add_repo_cancel, { dialog, id -> Unit })
          .create()

      dialog.show()

      // Dialogs get dismissed automatically on click if the builder is used
      // Add this here instead so they are dismissed only by dialog.dismiss() calls
      dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        createRepo(dialogView, dialog)
      }
    }
  }

  private fun createRepo(dialogView: View, dialog: AlertDialog) {
    val repoNameEditText = dialogView.findViewById(R.id.repo_add_dialog_name) as EditText
    val cloneURLExists = (dialogView.findViewById(R.id.repo_add_dialog_clone) as Switch).isChecked
    val newRepoName = repoNameEditText.text.toString()
    val newRepo = Repository(this@MainActivity, newRepoName)
    val creationCallback = Repository.callbackFactory(
        dialogView,
        if (cloneURLExists) R.string.error_clone_failed else R.string.error_init_failed,
        if (cloneURLExists) R.string.snack_clone_success else R.string.snack_init_success,
        {
          repoNames.add(newRepoName)
          listElements!!.notifyDataSetChanged()
        }
    )
    if (cloneURLExists) {
      val cloneURLEditText = dialogView.findViewById(R.id.repo_add_dialog_cloneURL) as EditText
      val cloneURL = cloneURLEditText.text.toString()
      if (cloneURL.isEmpty()) {
        cloneURLEditText.error = resources.getString(R.string.error_need_clone_URI)
        return
      } else {
        cloneURLEditText.error = null
      }
      // TODO: maybe add a progress bar or something
      newRepo.gitClone(cloneURL, creationCallback)
    } else {
      newRepo.gitInit(creationCallback)
    }
    dialog.dismiss()
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
