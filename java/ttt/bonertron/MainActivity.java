package ttt.bonertron;

import android.content.Intent;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.os.SystemClock;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity implements OnTouchListener
{

    //Intent Extra Strings
    public final static String EXTRA_PITCH_STRING = "pString";
    public final static String EXTRA_BEND = "bend";

    //Numbers Of Things
    public final static int TAP_NUM = 12;
    public final static int LFSR_NUM = 3;

    //Frequency Stuff
    public final static int Fs = 44100;
    public static float CLOCK_RATE = 2000.0f;
    public final static float AUDIO_SCALE = 10000.0f;
    public static float BEND = 0;
    public static final float BEND_SCALE = 100.0f;
    public static String pitchString = "C8";

    //                                          A B  C D E F G
    public final static int[] RAW_NOTE_TABLE = {9, 11, 0, 2, 4, 5, 7};
    public final static double C0 = 16.351597831287414667365624595207;

    //IIR Filter Stuff
    public static final int IIR_LEN = 3;
    public static final float IIR_GAIN = 1;
    public static final float[] IIR_Cy= {0, 1.9959701796f, -0.9959782831f};
    public static final float[] IIR_Cx = {1, -2, 1};
    public static float[] IIR_x;
    public static float[] IIR_y;
    public static int IIR_i;

    //Audio Streaming Stuff
    public static long streamTicks = 0;
    public static long streamTime;  //In ms
    public static long updateTime;
    short samples[];
    int buffSize;

    Thread t;
    boolean isRunning = true;

    //LFSR Stuff
    LFSR[] lfsrs;
    public static int lfsr_sel;

    //Buttons
    Button[] tap_buttons;
    Button[] lfsr_buttons;
    int[] tap_button_ids;
    int[] lfsr_button_ids;

    public final static int BUTTON_NONE = 0;
    public final static int BUTTON_TAP = 1;
    public final static int BUTTON_LFSR = 2;

    //Debug Text
    public static TextView debugText;

    //Computes one tick of LFSR given taps and value
    public int doLFSR(int taps, int lfsr)
    {
        int masked = taps&lfsr;
        int feed = 1;
        while(masked != 0)
        {
            feed += masked&1;
            masked>>=1;
        }
        feed&=1;

        lfsr<<=1;
        lfsr|=feed;

        return lfsr;
    }

    //Respond to touch events
    public boolean onTouch(View v, MotionEvent event)
    {
        int viewID;
        int t;
        int val = 0;

        t = BUTTON_NONE;
        viewID = v.getId();
        for(int i = 0; i < TAP_NUM; ++i)
        {
            if(viewID == tap_button_ids[i])
            {
                t = BUTTON_TAP;
                val = i;
                break;
            }
        }
        if(t ==  BUTTON_NONE)
        {
            for (int i = 0; i < LFSR_NUM; ++i)
            {
                if (viewID == lfsr_button_ids[i])
                {
                    t = BUTTON_LFSR;
                    val = i;
                    break;
                }
            }
        }

        if(t == BUTTON_NONE) return false;

        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            touchDown(t, val);
        }
        if (event.getAction() == MotionEvent.ACTION_UP)
        {
            touchUp(t, val);
        }

        return true;
    }

    //Touch Down event
    public void touchDown(int t, int v)
    {
        updateTime = SystemClock.elapsedRealtime();
        if(t == BUTTON_LFSR)
        {
            lfsr_buttons[lfsr_sel].setBackgroundColor(Color.LTGRAY);
            lfsr_sel = v;
            lfsr_buttons[v].setBackgroundColor(Color.DKGRAY);
        }
        else if(t == BUTTON_TAP)
        {
            lfsrs[lfsr_sel].taps |= (1 << v);
            tap_buttons[v].setBackgroundColor(Color.DKGRAY);
        }
    }

    //Touch Up event
    public void touchUp(int t, int v)
    {
        updateTime = SystemClock.elapsedRealtime();
        if(t == BUTTON_TAP)
        {
            lfsrs[lfsr_sel].taps &= ~(1 << v);
            tap_buttons[v].setBackgroundColor(Color.LTGRAY);

        }
    }

    //Handle settings changes
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        BEND = data.getFloatExtra(MainActivity.EXTRA_BEND, 0);
        pitchString = data.getStringExtra(MainActivity.EXTRA_PITCH_STRING);

        if(pitchString == null)
        {
            debugText.setText("Settings result fail " + debugText.getText());
            return;
        }

        CLOCK_RATE = (float)getFrequency();

        debugText.setText(CLOCK_RATE +  " " + debugText.getText());

    }

    //Compute frequency from the current pitchString and BEND values
    double getFrequency()
    {
        int baseNote;
        int octave = 0;
        double basePitch;

        pitchString.toUpperCase();
        int tChar;
        int i = 0;
        tChar = (int)pitchString.charAt(i);

        //Search for Note letter
        while(i < pitchString.length()-1 && (tChar < (int)'A' || tChar > (int)'G'))
        {
            ++i;
            tChar = (int)pitchString.charAt(i);
        }
        if(tChar - (int)'A' >= RAW_NOTE_TABLE.length)
        {
            return CLOCK_RATE;
        }

        baseNote = RAW_NOTE_TABLE[tChar - 'A'];

        //Search for note number
        while(i < pitchString.length()-1 && (tChar < (int)'0' || tChar > (int)'9'))
        {
            if(tChar == (int)'#')
            {
                ++baseNote;
            }
            else if(tChar == (int)'b')
            {
                --baseNote;
            }
            ++i;
            tChar = (int)pitchString.charAt(i);
        }

        if((tChar >= (int)'0' && tChar <= (int)'9'))
        {
            octave = (int)tChar - (int)'0';
        }

        float exponent = octave + (baseNote+BEND)/12.0f;

        //Compute base pitch from note and octave;
        basePitch = C0*java.lang.Math.pow(2.0f, exponent);

        return basePitch;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize arrays
        lfsrs = new LFSR[LFSR_NUM];
        lfsr_sel = 0;

        tap_buttons = new Button[TAP_NUM];
        lfsr_buttons = new Button[LFSR_NUM];

        tap_button_ids = new int[TAP_NUM];
        lfsr_button_ids = new int[LFSR_NUM];

        CLOCK_RATE = (float)getFrequency();

        //Get Tap Buttons
        String button_id;
        String button_text;
        for(short i = 0; i < TAP_NUM; ++i)
        {
            button_id = "button_tap_" + i;
            button_text = ""+i;

            tap_button_ids[i] = getResources().getIdentifier(button_id, "id", "ttt.bonertron");
            tap_buttons[i] = (Button) findViewById(tap_button_ids[i]);
            tap_buttons[i].setText(button_text);
            tap_buttons[i].setBackgroundColor(Color.LTGRAY);

            tap_buttons[i].setOnTouchListener(this);

        }

        //Get LFSR Buttons
        for(short i = 0; i < LFSR_NUM; ++i)
        {
            button_id = "button_lfsr_" + i;
            button_text = ""+i;
            lfsr_button_ids[i] = getResources().getIdentifier(button_id, "id", "ttt.bonertron");
            lfsr_buttons[i] = (Button) findViewById(lfsr_button_ids[i]);
            lfsr_buttons[i].setText(button_text);
            lfsr_buttons[i].setBackgroundColor(Color.LTGRAY);
            if(i == 0)
                lfsr_buttons[i].setBackgroundColor(Color.DKGRAY);

            lfsr_buttons[i].setOnTouchListener(this);

        }

        //Get debug text box
        debugText = (TextView) findViewById(R.id.debugText);
        debugText.setText("");

        //Audio streaming thread
        t = new Thread()
        {
            public void run()
            {
                setPriority(Thread.MAX_PRIORITY);

                buffSize = AudioTrack.getMinBufferSize(Fs, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

                AudioTrack audioTrack =
                        new AudioTrack(AudioManager.STREAM_MUSIC, Fs,
                                        AudioFormat.CHANNEL_OUT_MONO,
                                        AudioFormat.ENCODING_PCM_16BIT,
                                        buffSize,
                                        AudioTrack.MODE_STREAM);

                samples = new short[buffSize];

                String debugString = debugText.getText() + " " + buffSize;
                debugText.setText(debugString);

                for(short i = 0; i < LFSR_NUM; ++i)
                {
                    lfsrs[i] = new LFSR(Fs, (float)(CLOCK_RATE/Math.pow(2, i)) );
                }

                IIR_x = new float[IIR_LEN];
                IIR_y = new float[IIR_LEN];
                IIR_i = 0;
                for(int i = 0; i < IIR_LEN; ++i)
                {
                    IIR_x[i] = 0;
                    IIR_y[i] = 0;
                }


                float nextClock;
                float rawVal;
                int i_eff;

                audioTrack.play();

                while(isRunning)
                {

                    streamTime = SystemClock.elapsedRealtime();

                    for(short i = 0; i < LFSR_NUM; ++i)
                    {
                        lfsrs[i].setTime(0);
                    }

                    for(short i = 0; i < buffSize; ++i)
                    {

                        rawVal = 0;
                        //Compute buffer vals

                        for(short j = 0; j < LFSR_NUM; ++j)
                        {
                            //rawVal+= lfsr[j]&1;
                            rawVal += lfsrs[j].tick();
                        }
                        rawVal -= LFSR_NUM/2;
                        rawVal *= AUDIO_SCALE/LFSR_NUM;

                        //Do DSP's

                        IIR_x[IIR_i] = rawVal/IIR_GAIN;
                        IIR_y[IIR_i] = 0;

                        for(int j = 0; j < IIR_LEN; ++j)
                        {
                            i_eff = IIR_i-j;
                            if(i_eff < 0) i_eff += IIR_LEN;

                            IIR_y[IIR_i] += IIR_y[i_eff]*IIR_Cy[j];
                            IIR_y[IIR_i] += IIR_x[i_eff]*IIR_Cx[j];
                        }

                        samples[i] = (short)(IIR_y[IIR_i]);

                        ++IIR_i;
                        if(IIR_i == IIR_LEN) IIR_i=0;

                    }
                    //write buffer
                    audioTrack.write(samples, 0, buffSize);
                    for(short i = 0; i < LFSR_NUM; ++i)
                    {
                        lfsrs[i].setPhase(buffSize);
                    }

                }

                audioTrack.stop();
                audioTrack.release();
            }

        };

        t.start();

    }

    public void onDestroy()
    {
        super.onDestroy();
        isRunning = false;
        try
        {
            t.join();
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        t = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void openSettings()
    {
        Intent intent = new Intent(this, SettingsActivity.class);

        intent.putExtra(EXTRA_PITCH_STRING, pitchString);
        intent.putExtra(EXTRA_BEND, BEND*50+50);

        startActivityForResult(intent, 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            openSettings();
        }

        return super.onOptionsItemSelected(item);
    }

}
