package ttt.bonertron;

import android.content.Intent;
import android.content.res.Resources;
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
import android.widget.EditText;


public class MainActivity extends ActionBarActivity {

    public final static String EXTRA_MESSAGE = "ttt.bonertron.MESSAGE";

    public final static int FANCY_TAP = 0;
    public final static int FANCY_LFSR = 1;


    public final static int TAP_NUM = 12;
    public final static int LFSR_NUM = 3;

    public final static int Fs = 44100;
    public final static float CLOCK_RATE = 2000.0f;
    public final static float AUDIO_SCALE = 10000.0f;

    public static int[] taps;
    public static int[] lfsr;
    public static int[] lfsr_divs;
    public static int lfsr_sel;

    Button[] tap_buttons;
    Button[] lfsr_buttons;

    Thread t;
    boolean isRunning = true;

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

    public void fancyDown(int t, int v)
    {
        if(t == FANCY_LFSR)
        {
            lfsr_buttons[lfsr_sel].setBackgroundColor(Color.LTGRAY);
            lfsr_sel = v;
            lfsr_buttons[v].setBackgroundColor(Color.DKGRAY);

        }
        else if(t == FANCY_TAP)
        {
            taps[lfsr_sel] |= (1 << v);
            tap_buttons[v].setBackgroundColor(Color.DKGRAY);
        }
    }

    public void fancyUp(int t, int v)
    {
        if(t == FANCY_TAP)
        {
            taps[lfsr_sel] &= ~(1 << v);
            tap_buttons[v].setBackgroundColor(Color.LTGRAY);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        taps = new int[LFSR_NUM];
        lfsr = new int[LFSR_NUM];
        lfsr_divs = new int[LFSR_NUM];
        lfsr_sel = 0;

        tap_buttons = new Button[TAP_NUM];
        lfsr_buttons = new Button[LFSR_NUM];

        for(short i = 0; i < LFSR_NUM; ++i)
        {
            taps[i] = 0;
            lfsr[i] = 0;
            lfsr_divs[i] = 0;
        }

        String button_id;
        String button_text;
        for(short i = 0; i < TAP_NUM; ++i)
        {
            button_id = "button_tap_" + i;
            button_text = ""+i;
            tap_buttons[i] = (Button) findViewById(getResources().getIdentifier(button_id, "id", "ttt.bonertron"));
            tap_buttons[i].setText(button_text);
            tap_buttons[i].setBackgroundColor(Color.LTGRAY);


            fancyTouchListener ftl = new fancyTouchListener(this, FANCY_TAP, i);

            tap_buttons[i].setOnTouchListener(ftl);
        }

        for(short i = 0; i < LFSR_NUM; ++i)
        {
            button_id = "button_lfsr_" + i;
            button_text = ""+i;
            lfsr_buttons[i] = (Button) findViewById(getResources().getIdentifier(button_id, "id", "ttt.bonertron"));
            lfsr_buttons[i].setText(button_text);
            lfsr_buttons[i].setBackgroundColor(Color.LTGRAY);
            if(i == 0)
                lfsr_buttons[i].setBackgroundColor(Color.DKGRAY);

            fancyTouchListener ftl = new fancyTouchListener(this, FANCY_LFSR, i);

            lfsr_buttons[i].setOnTouchListener(ftl);

        }


        t = new Thread()
        {
            public void run()
            {
                setPriority(Thread.MAX_PRIORITY);

                int buffSize = AudioTrack.getMinBufferSize(Fs, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

                AudioTrack audioTrack =
                        new AudioTrack(AudioManager.STREAM_MUSIC, Fs,
                                        AudioFormat.CHANNEL_OUT_MONO,
                                        AudioFormat.ENCODING_PCM_16BIT,
                                        buffSize,
                                        AudioTrack.MODE_STREAM);

                short samples[] = new short[buffSize];

                float nextClock;

                audioTrack.play();
                nextClock = Fs/CLOCK_RATE;
                while(isRunning)
                {

                    for(short i = 0; i < buffSize; ++i)
                    {
                        //On clocks do LFSRS
                        if(i >= nextClock)
                        {
                            nextClock += Fs/CLOCK_RATE;
                            for(short j = 0; j < LFSR_NUM; ++j)
                            {
                                ++lfsr_divs[j];
                                if(lfsr_divs[j] >> j != 0)
                                {
                                    lfsr_divs[j] = 0;
                                    lfsr[j] = doLFSR(taps[j], lfsr[j]);
                                }
                            }
                        }

                        //Compute buffer vals
                        samples[i]= 0;
                        for(short j = 0; j < LFSR_NUM; ++j)
                        {
                            samples[i]+= lfsr[j]&1;
                        }
                        samples[i] -= LFSR_NUM/2;
                        samples[i] *= AUDIO_SCALE/LFSR_NUM;
                    }

                    nextClock -= (buffSize-1);
                    //write buffer
                    audioTrack.write(samples, 0, buffSize);

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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
