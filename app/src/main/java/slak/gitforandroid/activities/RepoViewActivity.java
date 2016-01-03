package slak.gitforandroid.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

import slak.gitforandroid.AsyncGitTask;
import slak.gitforandroid.R;
import slak.gitforandroid.Repository;
import slak.gitforandroid.SelectableAdapterModel;
import slak.gitforandroid.dialogs.CommitDialogBuilder;
import slak.gitforandroid.dialogs.PushDialogBuilder;

public class RepoViewActivity extends AppCompatActivity {
  private ArrayList<SelectableAdapterModel<File>> nodes;
  private ArrayAdapter<SelectableAdapterModel<File>> listElements;
  private Stack<SelectableAdapterModel<File>> fileStack = new Stack<>();
  private Toolbar toolbar;
  private Repository repo;

  boolean multiSelectState = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_repo_view);
    toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    Intent startIntent = getIntent();
    repo = new Repository(this, startIntent.getStringExtra(MainActivity.INTENT_REPO_NAME));

    final ListView currentDir = (ListView) findViewById(R.id.current_directory);
    nodes = new ArrayList<>();
    nodes.addAll(SelectableAdapterModel.fromArray(repo.getRepoFolder().listFiles()));
    fileStack.push(new SelectableAdapterModel<>(repo.getRepoFolder()));
    listElements =
        new ArrayAdapter<SelectableAdapterModel<File>>(this, R.layout.list_element_main, nodes) {
      @Override
      public int getViewTypeCount() {
        return 3;
      }
      @Override
      public int getItemViewType(int position) {
        if (nodes.size() == 0) return 2;
        if (nodes.get(position).getThing().isDirectory()) return 0;
        if (nodes.get(position).getThing().isFile()) return 1;
        return 1; // javac was complaining
      }
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        if (this.getItemViewType(position) == 2) {
          return RepoViewActivity.this.getLayoutInflater()
              .inflate(R.layout.list_element_empty_folder_repo_view, parent, false);
        }
        String nodeName = nodes.get(position).getThing().getName();
        TextView nodeView;
        int whatNode = this.getItemViewType(position) == 0 ?
            R.id.folder_element : R.id.file_element;
        // If convertView has the correct layout and type, reuse it
        // Otherwise, inflate new layout
        if (convertView != null && convertView instanceof TextView
            && whatNode == convertView.getId()) {
          nodeView = (TextView) convertView;
        } else {
          int layoutToUse = this.getItemViewType(position) == 0 ?
              R.layout.list_element_folder_repo_view : R.layout.list_element_file_repo_view;
          nodeView = (TextView) RepoViewActivity.this.getLayoutInflater()
              .inflate(layoutToUse, parent, false);
        }
        nodeView.setText(nodeName);
        return nodeView;
      }
      @Override
      public int getCount() {
        if (nodes.size() == 0) return 1;
        else return super.getCount();
      }
    };
    currentDir.setAdapter(listElements);
    currentDir.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
      @Override
      public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        multiSelectState = true;
        // No item was checked: style the toolbar for multi-select
        if (SelectableAdapterModel.getSelectedModels(nodes).size() == 0)
          inflateMenu(R.menu.menu_multi_select);
        nodes.get(position).setSelectStatus(true);
        view.setBackgroundColor(
            getResources().getColor(R.color.colorSelected, RepoViewActivity.this.getTheme()));
        return true;
      }
    });
    currentDir.setOnItemClickListener(new ListView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Directory empty, nothing to do here
        if (nodes.size() == 0) return;
        // Clicked on a selected item: deselect it
        if (nodes.get(position).isSelected() && multiSelectState) {
          nodes.get(position).setSelectStatus(false);
          view.setBackground(currentDir.getBackground());
          // No more items are selected: reset the style of the toolbar
          if (SelectableAdapterModel.getSelectedModels(nodes).size() == 0) {
            multiSelectState = false;
            inflateMenu(R.menu.menu_repo_view);
          }
          return;
        // Clicked on a unselected item in multi-select: select it
        } else if (multiSelectState) {
          nodes.get(position).setSelectStatus(true);
          view.setBackgroundColor(
              getResources().getColor(R.color.colorSelected, RepoViewActivity.this.getTheme()));
          return;
        }
        // Entering a new folder
        if (view.getId() == R.id.folder_element) {
          fileStack.push(nodes.get(position));
          nodes.clear();
          nodes.addAll(SelectableAdapterModel.fromArray(fileStack.peek().getThing().listFiles()));
          listElements.notifyDataSetChanged();
        } else if (view.getId() == R.id.file_element) {
          Intent intent = new Intent(Intent.ACTION_VIEW);
          Uri uri = Uri.parse("file://" + nodes.get(position).getThing().getAbsolutePath());
          intent.setDataAndType(uri, "text/plain");
          startActivity(intent);
        }
      }
    });

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        CommitDialogBuilder commit = new CommitDialogBuilder(RepoViewActivity.this);
        commit.showDialog(repo);
      }
    });

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }

  private void inflateMenu(int id) {
    toolbar.getMenu().clear();
    this.getMenuInflater().inflate(id, toolbar.getMenu());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    inflateMenu(R.menu.menu_repo_view);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      // Behave like the hardware back button
      onBackPressed();
      return true; // Event consumed
    } else if (item.getItemId() == R.id.action_push) {
      PushDialogBuilder pushDialog = new PushDialogBuilder(this);
      pushDialog.showDialog(repo);
    } else if (item.getItemId() == R.id.action_pull) {
      // TODO: pull
    } else if (item.getItemId() == R.id.action_stage) {
      ArrayList<String> paths = new ArrayList<>();
      for (Integer index : SelectableAdapterModel.getSelectedModels(nodes))
        paths.add(nodes.get(index).getThing().getPath());
      repo.gitAdd(paths, new AsyncGitTask.AsyncTaskCallback() {
        @Override
        public void onFinish(AsyncGitTask completedTask) {
          if (completedTask.getException() != null) {
            SomethingTerribleActivity.runATerribleActivity(
                RepoViewActivity.this, completedTask.getException().toString(), "Error");
            return;
          }
          Snackbar.make(findViewById(R.id.action_stage), "Staged items", Snackbar.LENGTH_LONG).show();
        }
      });
      return true;
    } else if (item.getItemId() == R.id.action_unstage) {
      // TODO: unstage
    } else if (item.getItemId() == R.id.action_delete) {
      // TODO: delete
    }
    return super.onOptionsItemSelected(item);
  }

  // Override back button so it traverses the folder structure before exiting the activity
  @Override
  public void onBackPressed() {
    if (fileStack.size() <= 1) {
      fileStack = new Stack<>();
      super.onBackPressed();
    } else {
      fileStack.pop();
      nodes.clear();
      nodes.addAll(SelectableAdapterModel.fromArray(fileStack.peek().getThing().listFiles()));
      listElements.notifyDataSetChanged();
    }
  }
}
