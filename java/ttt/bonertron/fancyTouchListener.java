package ttt.bonertron;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;


public class fancyTouchListener implements OnTouchListener
{
    int t;
    int val;
    MainActivity activity;

    fancyTouchListener(MainActivity a, int t, int val)
    {
        this.t = t;
        this.val = val;
        this.activity = a;
    }

    public boolean onTouch(View v, MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            activity.fancyDown(t, val);
        }
        if (event.getAction() == MotionEvent.ACTION_UP)
        {
            activity.fancyUp(t, val);
        }

        return true;
    }

}
