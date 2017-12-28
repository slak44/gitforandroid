package slak.gitforandroid

import android.app.AlertDialog
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.*
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

fun commitDialog(context: AppCompatActivity, target: Repository, snack: View): AlertDialog {
  val dialogView: View = context.layoutInflater.inflate(R.layout.dialog_commit, null)
  val builder: AlertDialog.Builder = AlertDialog.Builder(context)
  builder
      .setTitle(R.string.dialog_commit_title)
      .setView(dialogView)
      .setPositiveButton(R.string.dialog_commit_confirm, null)
      .setNegativeButton(R.string.dialog_commit_cancel) { dialog, which -> } // Auto dismiss
  val dialog: AlertDialog = builder.create()

  val substitute = dialogView.findViewById(R.id.dialog_commit_substitute) as Switch
  substitute.setOnCheckedChangeListener { btn: CompoundButton, isChecked: Boolean ->
    val dataStatus = if (isChecked) View.VISIBLE else View.GONE
    dialogView.findViewById<TextView>(R.id.dialog_commit_name).visibility = dataStatus
    dialogView.findViewById<TextView>(R.id.dialog_commit_email).visibility = dataStatus
  }

  dialog.show()
  dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
    val message = dialogView.findViewById(R.id.dialog_commit_message) as EditText
    val name = dialogView.findViewById(R.id.dialog_commit_name) as EditText
    val email = dialogView.findViewById(R.id.dialog_commit_email) as EditText
    var nameString: String? = null
    var emailString: String? = null
    if (!name.text.toString().isEmpty()) nameString = name.text.toString()
    if (!email.text.toString().isEmpty()) emailString = email.text.toString()
    target.gitCommit(nameString, emailString, message.text.toString(), {
      completedTask: SafeAsyncTask ->
      if (completedTask.exception != null) {
        reportError(
            snack,
            context.resources.getString(R.string.error_commit_failed),
            completedTask.exception!!
        )
        return@gitCommit
      }
      Snackbar.make(
          snack,
          context.resources.getString(R.string.snack_item_commit_success, message.text.toString()),
          Snackbar.LENGTH_LONG
      ).setAction(R.string.snack_action_revert_commit, null).show()
      // TODO: implement revert commit
    })
    dialog.dismiss()
  }
  return dialog
}

fun passwordDialog(context: AppCompatActivity, passCb: (String) -> Unit): AlertDialog {
  val dialogView: View = context.layoutInflater.inflate(R.layout.dialog_password, null)
  val builder: AlertDialog.Builder = AlertDialog.Builder(context)
  builder
      .setTitle(R.string.dialog_pass_title)
      .setView(dialogView)
      .setPositiveButton(R.string.dialog_pass_confirm, null)
      .setNegativeButton(R.string.dialog_pass_cancel) { dialog, which -> } // Auto dismiss
  val dialog: AlertDialog = builder.create()
  dialog.show()
  dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
    val pass = dialogView.findViewById(R.id.pass_dialog_data) as EditText
    passCb(pass.text.toString())
    dialog.dismiss()
  }
  return dialog
}

fun createRepoDialog(
    context: AppCompatActivity,
    snack: View,
    onSuccess: (createdName: String) -> Unit = {}
): AlertDialog {
  val newRepo = AlertDialog.Builder(context)
  val dialogView = context.layoutInflater.inflate(R.layout.dialog_add_repo, null)

  val toClone = dialogView.findViewById(R.id.repo_add_dialog_clone) as Switch
  toClone.setOnCheckedChangeListener { btn: CompoundButton, isChecked: Boolean ->
    dialogView.findViewById<TextView>(R.id.repo_add_dialog_cloneURL).visibility =
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
    val repoNameEditText = dialogView.findViewById(R.id.repo_add_dialog_name) as EditText
    val cloneURLExists = (dialogView.findViewById(R.id.repo_add_dialog_clone) as Switch).isChecked
    val failedRes = if (cloneURLExists) R.string.error_clone_failed else R.string.error_init_failed
    val okRes = if (cloneURLExists) R.string.snack_clone_success else R.string.snack_init_success
    val newRepoName = repoNameEditText.text.toString()
    val newRepository = Repository(context, newRepoName)
    val creationCallback = Repository.callbackFactory(
        snack,
        context.resources.getString(failedRes),
        context.resources.getString(okRes),
        {
          onSuccess(newRepoName)
        },
        { t: Throwable ->
          newRepository.repoFolder.deleteRecursively()
        }
    )
    if (cloneURLExists) {
      val cloneURLEditText = dialogView.findViewById(R.id.repo_add_dialog_cloneURL) as EditText
      val cloneURL = cloneURLEditText.text.toString()
      if (cloneURL.isEmpty()) {
        cloneURLEditText.error = context.getString(R.string.error_need_clone_URI)
        return@setOnClickListener
      } else {
        cloneURLEditText.error = null
      }
      // TODO: maybe add a progress bar or something
      newRepository.gitClone(snack, cloneURL, creationCallback)
    } else {
      newRepository.gitInit(snack, creationCallback)
    }
    dialog.dismiss()
  }
  return dialog
}

enum class RemoteOp {
  PUSH, PULL
}

fun pushPullDialog(
    context: AppCompatActivity,
    snack: View,
    target: Repository,
    operation: RemoteOp
): AlertDialog {
  val titleRes = when (operation) {
    RemoteOp.PUSH -> R.string.dialog_push_pull_title_push
    RemoteOp.PULL -> R.string.dialog_push_pull_title_pull
  }
  val confirmRes = when (operation) {
    RemoteOp.PUSH -> R.string.dialog_push_pull_confirm_push
    RemoteOp.PULL -> R.string.dialog_push_pull_confirm_pull
  }
  val successSnackRes = when (operation) {
    RemoteOp.PUSH -> R.string.snack_item_push_success
    RemoteOp.PULL -> R.string.snack_item_pull_success
  }
  val failSnackRes = when (operation) {
    RemoteOp.PUSH -> R.string.error_push_failed
    RemoteOp.PULL -> R.string.error_pull_failed
  }

  val dialogView: View = context.layoutInflater.inflate(R.layout.dialog_push_pull, null)
  val builder: AlertDialog.Builder = AlertDialog.Builder(context)
  builder
      .setTitle(titleRes)
      .setView(dialogView)
      .setPositiveButton(confirmRes, null)
      .setNegativeButton(R.string.dialog_push_pull_cancel) { dialog, which -> } // Auto dismiss
  val dialog: AlertDialog = builder.create()

  val manualRemote = dialogView.findViewById(R.id.dialog_push_pull_switch_remote) as Switch
  manualRemote.setOnCheckedChangeListener { btn: CompoundButton, isChecked: Boolean ->
    val dataStatus = if (isChecked) View.GONE else View.VISIBLE
    dialogView.findViewById<LinearLayout>(R.id.dialog_push_pull_spinner_layout).visibility = dataStatus
    dialogView.findViewById<EditText>(R.id.dialog_push_pull_manual_remote).visibility =
        if (isChecked) View.VISIBLE else View.GONE // just opposite of dataStatus
  }

  // Add remotes to dropdown
  val remoteListDropdown = dialogView.findViewById(R.id.dialog_push_pull_dropdown) as Spinner
  val remotesList = target.listRemotes()
  if (remotesList.isNotEmpty()) {
    remoteListDropdown.adapter =
        ArrayAdapter(context, android.R.layout.simple_spinner_item, remotesList)
  } else {
    dialogView.findViewById<TextView>(R.id.dialog_push_pull_dropdown_empty).visibility = View.VISIBLE
    remoteListDropdown.visibility = View.GONE
  }

  dialog.show()
  dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
    val usingManualRemote =
        (dialogView.findViewById(R.id.dialog_push_pull_switch_remote) as Switch).isChecked
    if (!usingManualRemote && remotesList.isEmpty()) return@setOnClickListener

    val remote: String = if (usingManualRemote)
      (dialogView.findViewById(R.id.dialog_push_pull_manual_remote) as EditText).text.toString()
    else remotesList[remoteListDropdown.selectedItemId.toInt()]

    if (remote.isBlank()) return@setOnClickListener

    val gitActionCallback: (SafeAsyncTask) -> Unit = cb@ { completedTask: SafeAsyncTask ->
      if (completedTask.exception != null) {
        reportError(
            snack,
            context.resources.getString(failSnackRes, remote),
            completedTask.exception!!
        )
        return@cb
      }
      Snackbar.make(
          snack,
          context.resources.getString(successSnackRes, remote),
          Snackbar.LENGTH_LONG
      ).show()
    }

    val actionCallback: (String) -> Unit = when (operation) {
      RemoteOp.PUSH -> { pass: String ->
        target.gitPush(remote, UsernamePasswordCredentialsProvider("", pass), gitActionCallback)
      }
      RemoteOp.PULL -> { pass: String ->
        target.gitPull(remote, UsernamePasswordCredentialsProvider("", pass), gitActionCallback)
      }
    }

    passwordDialog(context, actionCallback)
    dialog.dismiss()
  }

  return dialog
}
