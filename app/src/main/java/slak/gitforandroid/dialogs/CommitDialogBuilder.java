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

import slak.gitforandroid.R;
import slak.gitforandroid.Repository;
import slak.gitforandroid.activities.SomethingTerribleActivity;

public class CommitDialogBuilder extends AlertDialog.Builder {
  private Activity context;
  private AlertDialog dialog;
  private final View dialogView;

  public CommitDialogBuilder(Activity context) {
    super(context);
    this.context = context;
    dialogView = context.getLayoutInflater().inflate(R.layout.dialog_commit, null);
    this.setTitle(R.string.commit_dialog_title);
    this.setView(dialogView);
    this.setPositiveButton(R.string.commit_dialog_confirm, null);
    this.setNegativeButton(R.string.commit_dialog_cancel, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        // Automatically dismiss
      }
    });
    Switch substitute = (Switch) dialogView.findViewById(R.id.dialog_commit_substitute);
    substitute.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
        int dataStatus = View.GONE;
        if (isChecked) dataStatus = View.VISIBLE;
        dialogView.findViewById(R.id.dialog_commit_name).setVisibility(dataStatus);
        dialogView.findViewById(R.id.dialog_commit_email).setVisibility(dataStatus);
      }
    });
    dialog = this.create();
  }

  public void showDialog(final Repository target) {
    dialog.show();
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // TODO: add input validation
        EditText message = (EditText) dialogView.findViewById(R.id.dialog_commit_message);
        EditText name = (EditText) dialogView.findViewById(R.id.dialog_commit_name);
        EditText email = (EditText) dialogView.findViewById(R.id.dialog_commit_email);
        try {
          String nameString = null;
          String emailString = null;
          if (!name.getText().toString().isEmpty()) nameString = name.getText().toString();
          if (!email.getText().toString().isEmpty()) emailString = email.getText().toString();
          target.gitCommit(nameString, emailString, message.getText().toString());
          Snackbar.make(
              context.findViewById(R.id.fab),
              "Created commit " + message.getText().toString(),
              Snackbar.LENGTH_LONG)
              .setAction("Revert", null).show(); // TODO: implement revert commit
        } catch (RuntimeException ex) {
          SomethingTerribleActivity.runATerribleActivity(context, ex.toString(), "Error");
          return;
        }
        dialog.dismiss();
      }
    });
  }
}
