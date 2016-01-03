package slak.gitforandroid.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.EditText;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;

import slak.gitforandroid.AsyncGitTask;
import slak.gitforandroid.R;
import slak.gitforandroid.Repository;
import slak.gitforandroid.activities.SomethingTerribleActivity;

public class PushDialogBuilder extends AlertDialog.Builder {
  private Activity context;
  private AlertDialog dialog;
  private final View dialogView;

  public PushDialogBuilder(Activity context) {
    super(context);
    this.context = context;
    dialogView = context.getLayoutInflater().inflate(R.layout.dialog_push, null);
    this.setTitle(R.string.push_dialog_title);
    this.setView(dialogView);
    this.setPositiveButton(R.string.push_dialog_confirm, null);
    this.setNegativeButton(R.string.push_dialog_cancel, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        // Automatically dismiss
      }
    });
    Switch manualRemote = (Switch) dialogView.findViewById(R.id.push_dialog_switch_remote);
    manualRemote.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
        int dataStatus = View.VISIBLE;
        if (isChecked) dataStatus = View.GONE;
        dialogView.findViewById(R.id.push_dialog_dropdown).setVisibility(dataStatus);
        dialogView.findViewById(R.id.push_dialog_manual_remote).setVisibility(
            dataStatus == View.GONE ? View.VISIBLE : View.GONE); // Basically: !dataStatus
      }
    });
    // TODO: add all existing remotes to R.id.push_dialog_dropdown here
    dialog = this.create();
  }

  public void showDialog(final Repository target) {
    dialog.show();
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // TODO: add input validation
        boolean usingManualRemote = ((Switch) dialogView.findViewById(R.id.push_dialog_switch_remote)).isChecked();
        final String remote;
        if (usingManualRemote) {
          EditText remoteText = (EditText) dialogView.findViewById(R.id.push_dialog_manual_remote);
          remote = remoteText.getText().toString();
        } else {
          // TODO: get selected from spinner, use that remote
          remote = null; // Just use the default
        }
        PasswordDialogBuilder pass = new PasswordDialogBuilder(context) {
          @Override
          public void getPassword(String password) {
            target.gitPush(remote, password, new AsyncGitTask.AsyncTaskCallback() {
              @Override
              public void onFinish(AsyncGitTask completedTask) {
                if (completedTask.getException() != null) {
                  SomethingTerribleActivity.runATerribleActivity(
                      context, completedTask.getException().toString(), "Error");
                  return;
                }
                Snackbar.make(
                    context.findViewById(R.id.fab), "Pushed repository", Snackbar.LENGTH_LONG).show();
              }
            });
            dialog.dismiss();
          }
        };
        pass.showDialog();
      }
    });
  }
}
