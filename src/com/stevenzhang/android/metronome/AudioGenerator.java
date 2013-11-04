
package com.stevenzhang.android.metronome;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;
import com.stevenzhang.android.metronome.R;
import static com.stevenzhang.android.metronome.Constants.DEV_MODE;

public class AudioGenerator {
	
    private int sampleRate;
    private AudioTrack audioTrack;
    
    public AudioGenerator(int sampleRate) {
    	this.sampleRate = sampleRate;
    }
    
    /**
     * Inspiration: 
     * http://stackoverflow.com/questions/2413426/playing-an-arbitrary-tone-with-android
     * which came from here:
     * http://marblemice.blogspot.com/2010/04/generate-and-play-tone-in-android.html
     */
    public double[] getSineWave(int samples,int sampleRate,
    		double frequencyOfTone) {
    	double[] sample = new double[samples];
        for (int i = 0; i < samples; i++) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/frequencyOfTone));
        }
		return sample;
    }
    
//Then write the byte sample
    public byte[] get16BitPcm(double[] samples) {
    	byte[] generatedSound = new byte[2 * samples.length];
    	int index = 0;
        for (double sample : samples) {
            // scale to maximum amplitude
            short maxSample = (short) ((sample * Short.MAX_VALUE));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSound[index++] = (byte) (maxSample & 0x00ff);
            generatedSound[index++] = (byte) ((maxSample & 0xff00) >>> 8);

        }
    	return generatedSound;
    }
    
//Create player
    public void createPlayer(){
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, sampleRate,
                AudioTrack.MODE_STREAM);
        //FIXME differences
    	audioTrack.play();
    }
    
//Write the sound onto the audiotrack buffer
    public void writeSoundDouble(double[] samples) {
    	byte[] generatedSnd = get16BitPcm(samples);
    	audioTrack.write(generatedSnd, 0, generatedSnd.length);
    }
    

    public void writeSoundByte(byte[] generatedSnd, String soundName) {
    	
    	if(DEV_MODE){
    		//explicitly generate the length of the array for easier debugging without expressions
    		int length = generatedSnd.length;
    	}
    	
    	audioTrack.write(generatedSnd, 0, generatedSnd.length);
    }
    
	public byte[] getTockBytes(Context ctx) throws IOException{
        InputStream is = ctx.getResources().openRawResource(R.raw.ray);
	    BufferedInputStream     bis = new BufferedInputStream   (is, Constants.SAMPLE_RATE);
	    DataInputStream         dis = new DataInputStream       (bis);      //  Create a DataInputStream to read the audio data from the saved file

	    //hacky way of getting payload's length
	    //TODO figure out a better way of doing this
	    byte[] payload = new byte[2100];
	    int i = 0; 
	    
	    //  Read the file into the "music" array
	    while (i < 2100)
	    {
	        payload[i] = dis.readByte();                                      //  This assignment does not reverse the order
	    
	    	i++;
	    }
	    
		if(DEV_MODE){
			Log.d("TickTock", "i = " + Integer.toString(i));
			Log.d("TickTock", "payload = " + Integer.toString(payload.length));
		}
	    dis.close();
		
    	
    	return payload;
    }
    
    public void destroyAudioTrack() {
    	audioTrack.stop();
    	audioTrack.release();
    }
    
}
