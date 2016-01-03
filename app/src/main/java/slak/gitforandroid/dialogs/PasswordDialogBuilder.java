package slak.gitforandroid.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import org.eclipse.jgit.lib.PersonIdent;

import slak.gitforandroid.R;
import slak.gitforandroid.Repository;

public class PasswordDialogBuilder extends AlertDialog.Builder {
  private Activity context;
  private AlertDialog dialog;
  private final View dialogView;

  public PasswordDialogBuilder(Activity context) {
    super(context);
    this.context = context;
    dialogView = context.getLayoutInflater().inflate(R.layout.dialog_password, null);
    this.setTitle(R.string.pass_dialog_title);
    this.setView(dialogView);
    this.setPositiveButton(R.string.pass_dialog_confirm, null);
    this.setNegativeButton(R.string.pass_dialog_cancel, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        // Automatically dismiss
      }
    });
    dialog = this.create();
  }

  public void showDialog() {
    dialog.show();
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        EditText pass = (EditText) dialogView.findViewById(R.id.pass_dialog_data);
        getPassword(pass.getText().toString());
        dialog.dismiss();
      }
    });
  }

  public void getPassword(String password) {
  }
}
