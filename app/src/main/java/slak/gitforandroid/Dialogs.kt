package slak.gitforandroid

import android.app.AlertDialog
import android.content.Context
import android.support.design.widget.Snackbar
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.dialog_add_repo.view.*
import kotlinx.android.synthetic.main.dialog_commit.view.*
import kotlinx.android.synthetic.main.dialog_password.view.*
import kotlinx.android.synthetic.main.dialog_push_pull.view.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

fun commitDialog(context: Context, target: Repository, snack: View): AlertDialog {
  val dialogView: View = LayoutInflater.from(context).inflate(R.layout.dialog_commit, null)
  val builder: AlertDialog.Builder = AlertDialog.Builder(context)
  builder
      .setTitle(R.string.dialog_commit_title)
      .setView(dialogView)
      .setPositiveButton(R.string.dialog_commit_confirm, null)
      .setNegativeButton(R.string.dialog_commit_cancel) { _, _ -> } // Auto dismiss
  val dialog: AlertDialog = builder.create()

  with(dialogView) {
    committerData.setOnCheckedChangeListener { _, isChecked: Boolean ->
      authorName.visibility = if (isChecked) View.VISIBLE else View.GONE
      authorEmail.visibility = if (isChecked) View.VISIBLE else View.GONE
    }
    dialog.show()
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
      target.commit(
          authorName.text.toString(),
          authorEmail.text.toString(),
          commitMessage.text.toString()).invokeOnCompletion {
        if (it != null) {
          Snackbar.make(snack, R.string.error_commit_failed, Snackbar.LENGTH_LONG)
          it.printStackTrace()
          return@invokeOnCompletion
        }
        Snackbar.make(
            snack,
            context.resources.getString(R.string.snack_item_commit_success, commitMessage.text.toString()),
            Snackbar.LENGTH_LONG
        ).setAction(R.string.snack_action_revert_commit, null /* TODO: implement revert commit */).show()
      }
      dialog.dismiss()
    }
  }

  return dialog
}

fun passwordDialog(context: Context, callback: (String) -> Unit): AlertDialog {
  val dialogView: View = LayoutInflater.from(context).inflate(R.layout.dialog_password, null)
  val builder: AlertDialog.Builder = AlertDialog.Builder(context)
  builder
      .setTitle(R.string.dialog_pass_title)
      .setView(dialogView)
      .setPositiveButton(R.string.dialog_pass_confirm, null)
      .setNegativeButton(R.string.dialog_pass_cancel) { _, _ -> } // Dismiss
  val dialog: AlertDialog = builder.create()
  dialog.show()
  with(dialogView) {
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
      callback(password.text.toString())
      dialog.dismiss()
    }
  }
  return dialog
}

fun createRepoDialog(context: Context, snack: View,
                     onSuccess: (createdName: String) -> Unit = {}): AlertDialog {
  val newRepo = AlertDialog.Builder(context)
  val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_repo, null)

  with(dialogView) {
    cloneSwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
      cloneUrl.visibility = if (isChecked) View.VISIBLE else View.GONE
    }
  }

  val dialog = newRepo
      .setTitle(R.string.dialog_add_repo_title)
      .setView(dialogView)
      // This listener is set below
      .setPositiveButton(R.string.dialog_add_repo_confirm, null)
      // Dismiss automatically
      .setNegativeButton(R.string.dialog_add_repo_cancel, { _, _ -> })
      .create()

  dialog.show()

  // Dialogs get dismissed automatically on click if the builder is used
  // Add this here instead so they are dismissed only by dialog.dismiss() calls
  dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { launch(UI) { with(dialogView) {
    val cloneURLExists = cloneSwitch.isChecked
    val failedRes = if (cloneURLExists) R.string.error_clone_failed else R.string.error_init_failed
    val okRes = if (cloneURLExists) R.string.snack_clone_success else R.string.snack_init_success
    val newRepoName = newRepoName.text.toString()
    val newRepository = Repository(context, newRepoName)

    if (newRepository.alreadyExists) {
      Snackbar.make(
          snack,
          R.string.error_repo_name_conflict,
          Snackbar.LENGTH_LONG
      ).show()
      dialog.dismiss()
      return@launch
    }

    val creationJob: Job
    if (cloneURLExists) {
      val cloneURL = cloneUrl.text.toString()
      if (cloneURL.isEmpty()) {
        cloneUrl.error = context.getString(R.string.error_need_clone_URI)
        return@launch
      } else {
        cloneUrl.error = null
      }
      // TODO: maybe add a progress bar or something
      creationJob = newRepository.clone(cloneURL)
    } else {
      creationJob = newRepository.init()
    }
    creationJob.withSnackResult(snack, okRes, failedRes, {
      if (it == null) onSuccess(newRepoName)
      else newRepository.repoFolder.deleteRecursively()
    })
    dialog.dismiss()
  } } }
  return dialog
}

enum class RemoteOp {
  PUSH, PULL
}

fun pushPullDialog(context: Context, snack: View, target: Repository,
                   operation: RemoteOp): AlertDialog {
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

  val dialogView: View = LayoutInflater.from(context).inflate(R.layout.dialog_push_pull, null)
  val builder: AlertDialog.Builder = AlertDialog.Builder(context)
  builder
      .setTitle(titleRes)
      .setView(dialogView)
      .setPositiveButton(confirmRes, null)
      .setNegativeButton(R.string.dialog_push_pull_cancel) { _, _ -> } // Dismiss
  val dialog: AlertDialog = builder.create()

  with(dialogView) {
    manualRemoteSwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
      spinnerLayout.visibility = if (isChecked) View.GONE else View.VISIBLE
      manualRemote.visibility = if (isChecked) View.VISIBLE else View.GONE
    }
    // Add remotes to dropdown
    val remotesList = target.listRemotes()
    if (remotesList.isNotEmpty()) {
      remotesDropdown.adapter =
          ArrayAdapter(context, android.R.layout.simple_spinner_item, remotesList)
    } else {
      noRemotesText.visibility = View.VISIBLE
      remotesDropdown.visibility = View.GONE
    }
    dialog.show()
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
      if (!manualRemoteSwitch.isChecked && remotesList.isEmpty()) return@setOnClickListener

      val remote: String =
          if (manualRemoteSwitch.isChecked) manualRemote.text.toString()
          else remotesList[remotesDropdown.selectedItemId.toInt()]

      if (remote.isBlank()) return@setOnClickListener

      passwordDialog(context) { pass ->
        val actionJob = if (operation == RemoteOp.PULL) {
          target.pull(remote, UsernamePasswordCredentialsProvider("", pass))
        } else {
          target.push(remote, UsernamePasswordCredentialsProvider("", pass))
        }
        actionJob.withSnackResult(snack,
            context.resources.getString(successSnackRes, remote),
            context.resources.getString(failSnackRes, remote))
      }
      dialog.dismiss()
    }
  }

  return dialog
}
