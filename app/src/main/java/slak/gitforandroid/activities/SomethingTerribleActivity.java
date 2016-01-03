package slak.gitforandroid.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import slak.gitforandroid.R;

public class SomethingTerribleActivity extends AppCompatActivity {
  public static final String INTENT_ERROR_DATA = "slak.gitforandroid.INTENT_ERROR_DATA";
  public static final String INTENT_ERROR_LEVEL = "slak.gitforandroid.INTENT_ERROR_LEVEL";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_something_terrible);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    Intent sentIntent = getIntent();
    String error = sentIntent.getStringExtra(INTENT_ERROR_DATA);
    String level = sentIntent.getStringExtra(INTENT_ERROR_LEVEL);

    TextView data = (TextView) findViewById(R.id.error_data);
    data.setText(level + "\n" + error);

    Button goBack = (Button) findViewById(R.id.error_return);
    goBack.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        SomethingTerribleActivity.this.finish();
      }
    });
  }

  public static void runATerribleActivity(Activity sourceActivity, String error, String level) {
    Intent errorIntent = new Intent(sourceActivity, SomethingTerribleActivity.class);
    errorIntent.putExtra(INTENT_ERROR_DATA, error);
    errorIntent.putExtra(INTENT_ERROR_LEVEL, level);
    sourceActivity.startActivity(errorIntent);
  }

}
