package slak.gitforandroid.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.EditText;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;

import java.io.File;
import java.util.ArrayList;

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
        dialogView.findViewById(R.id.push_dialog_spinner_layout).setVisibility(dataStatus);
        dialogView.findViewById(R.id.push_dialog_manual_remote).setVisibility(
            dataStatus == View.GONE ? View.VISIBLE : View.GONE); // Basically: !dataStatus
      }
    });
    dialog = this.create();
  }

  public void showDialog(final Repository target) {
    final Spinner remoteList = (Spinner) dialogView.findViewById(R.id.push_dialog_dropdown);
    final String[] remotesString = new File(target.getRepoFolder(), ".git/refs/remotes").list();
    if (remotesString != null && remotesString.length != 0) {
      ArrayAdapter<String> remotes = new ArrayAdapter<>(
          context, android.R.layout.simple_spinner_item,
          remotesString);
      remoteList.setAdapter(remotes);
    } else {
      dialogView.findViewById(R.id.push_dialog_dropdown_empty).setVisibility(View.VISIBLE);
      remoteList.setVisibility(View.GONE);
    }
    dialog.show();
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        boolean usingManualRemote = ((Switch) dialogView.findViewById(R.id.push_dialog_switch_remote)).isChecked();
        String remote;
        if (usingManualRemote) {
          EditText remoteText = (EditText) dialogView.findViewById(R.id.push_dialog_manual_remote);
          remote = remoteText.getText().toString();
        } else {
          try {
            remote = remotesString[(int) remoteList.getSelectedItemId()];
          } catch (NullPointerException npe) {
            remote = null;
          }
        }
        final String finalRemote = remote;
        PasswordDialogBuilder pass = new PasswordDialogBuilder(context) {
          @Override
          public void getPassword(String password) {
            target.gitPush(finalRemote, password, new AsyncGitTask.AsyncTaskCallback() {
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
