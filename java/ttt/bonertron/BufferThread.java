package ttt.bonertron;

/**
 * Created by Gravyman3321 on 12/27/2014.
 */
public class BufferThread extends Thread
{
    MainActivity a;
    long time;

    public BufferThread(MainActivity a, long t)
    {
        this.a = a;
        this.time = t;
    }

    public void run()
    {
        a.fillBufferFromTime(time);
    }

}
