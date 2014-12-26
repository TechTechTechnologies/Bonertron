package ttt.bonertron;

public class LFSR
{
    public int Fs;
    public float clockRate;
    public float spClock;
    //samples/clock= samples/sec * sec/clock = Fs/clockRate

    public long taps;
    long lfsr;

    long sampleNum;
    float nextClock;
    float phase;

    public void doLFSR()
    {
        long masked = taps&lfsr;
        int feed = 1;
        while(masked != 0)
        {
            feed += masked&1;
            masked>>=1;
        }
        feed&=1;

        lfsr<<=1;
        lfsr|=feed;
    }

    public LFSR(int Fs, float cR)
    {
        this.Fs = Fs;
        this.clockRate = cR;
        spClock = Fs/cR;
        nextClock = spClock;
        taps = 0;
        lfsr = 0;
        sampleNum = 0;
        phase = 0;
    }

    public void setTime(long sNum)
    {
        sampleNum = sNum;
        nextClock = (float)java.lang.Math.ceil(sampleNum/spClock)*spClock+phase;
    }

    public void setPhase(int buffSize)
    {
        phase = nextClock-buffSize;
    }

    //takes time in milliseconds
    public int tick()
    {

        if (sampleNum >= nextClock)
        {
            nextClock += spClock;
            doLFSR();
        }

        ++sampleNum;

        return (int)lfsr&1;
    }

}
