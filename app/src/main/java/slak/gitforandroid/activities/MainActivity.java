package slak.gitforandroid.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import slak.gitforandroid.R;
import slak.gitforandroid.Repository;

public class MainActivity extends AppCompatActivity {
  public final static String INTENT_REPO_NAME = "slak.gitforandroid.REPO_NAME";

  ArrayList<String> repoNames;
  ArrayAdapter<String> listElements;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    // Force the user to quit if there isn't access to the filesystem
    if (!Repository.isAvailable()) {
      AlertDialog.Builder fatalError = new AlertDialog.Builder(MainActivity.this);
      fatalError
        .setTitle(R.string.app_no_storage)
        .setNegativeButton(R.string.app_quit, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            // It is impossible to have more than one Activity on the stack at this point
            // This means the following call terminates the app
            System.exit(1);
          }
        })
        .create()
        .show();
      return;
    }

    ListView repoList = (ListView) findViewById(R.id.repoList);
    repoNames = new ArrayList<String>();
    repoNames.addAll(Arrays.asList(Repository.getRepoDirectory(this).list()));
    listElements = new ArrayAdapter<String>(this, R.layout.list_element_main, repoNames);
    repoList.setAdapter(listElements);
    repoList.setOnItemClickListener(new ListView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent repoViewIntent = new Intent(MainActivity.this, RepoViewActivity.class);
        repoViewIntent.putExtra(INTENT_REPO_NAME, ((TextView) view).getText().toString());
        startActivity(repoViewIntent);
      }
    });

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        AlertDialog.Builder newRepo = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_add_repo, null);

        DialogInterface.OnClickListener negative = new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            // Dismiss automatically
          }
        };
        Switch toClone = (Switch) dialogView.findViewById(R.id.repo_add_dialog_clone);
        toClone.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
            int cloneTextStatus = View.GONE;
            if (isChecked) cloneTextStatus = View.VISIBLE;
            dialogView.findViewById(R.id.repo_add_dialog_cloneURL).setVisibility(cloneTextStatus);
          }
        });

        final AlertDialog dialog = newRepo
            .setTitle(R.string.add_repo_dialog_title)
            .setView(dialogView)
            .setPositiveButton(R.string.add_repo_dialog_confirm, null)
            .setNegativeButton(R.string.add_repo_dialog_cancel, negative)
            .create();

        dialog.show();

        View.OnClickListener positive = new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            EditText repoNameEditText = (EditText) dialogView.findViewById(R.id.repo_add_dialog_name);
            String newRepoName = repoNameEditText.getText().toString();
            if (newRepoName.isEmpty()) {
              repoNameEditText.setError("This field cannot be empty");
              return;
            }
            repoNameEditText.setError(null);
            boolean cloneURLExists = ((Switch) dialogView.findViewById(R.id.repo_add_dialog_clone)).isChecked();
            Repository newRepo = new Repository(MainActivity.this, newRepoName);
            if (cloneURLExists) {
              EditText cloneURLEditText = (EditText) dialogView.findViewById(R.id.repo_add_dialog_cloneURL);
              String cloneURL = cloneURLEditText.getText().toString();
              if (cloneURL.isEmpty()) {
                cloneURLEditText.setError("A clone URI is required for cloning");
                return;
              }
              cloneURLEditText.setError(null);
              try {
                // TODO: progress bar or something
                newRepo.gitClone(cloneURL);
              } catch (GitAPIException | RuntimeException ex) {
                SomethingTerribleActivity
                    .runATerribleActivity(MainActivity.this, ex.toString(), "Error");
                return;
              }
            } else {
              try {
                newRepo.gitInit();
              } catch (GitAPIException | IOException ex) {
                SomethingTerribleActivity
                    .runATerribleActivity(MainActivity.this, ex.toString(), "Error");
                return;
              }
            }
            repoNames.add(newRepoName);
            listElements.notifyDataSetChanged();
            dialog.dismiss();
          }
        };
        // Dialogs get dismissed automatically on click if the builder is used
        // Add this here instead so they are dismissed only by dialog.dismiss() calls
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(positive);
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    if (id == R.id.action_settings) {
      Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
      startActivity(settingsIntent);
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

}
