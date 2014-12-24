package ttt.bonertron;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;


public class SettingsActivity extends ActionBarActivity
{

    SeekBar seek;
    EditText eText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Intent intent = getIntent();

        int bend = (int)intent.getFloatExtra(MainActivity.EXTRA_BEND, 50);
        String pitchString = intent.getStringExtra(MainActivity.EXTRA_PITCH_STRING);

        seek  = (SeekBar) findViewById(R.id.bend_lfsr);
        seek.setProgress(bend);

        eText = (EditText) findViewById(R.id.note);
        eText.setText(pitchString);

    }

    public void onSetClick(View view)
    {
        Intent intent = new Intent();
        intent.putExtra(MainActivity.EXTRA_PITCH_STRING, eText.getText().toString());
        intent.putExtra(MainActivity.EXTRA_BEND, (seek.getProgress()-50.0f)/50.0f);

        setResult(RESULT_OK, intent);

        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
