package com.stevenzhang.android.metronome;

import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

public class Metronome implements OnSharedPreferenceChangeListener{
	
	private double bpm = 100;
	private int beat;
	private int noteValue;
	
	private int numSilenceSamples;
	private final int numTickSamples = 1000; // samples per tick
	
	private boolean play = true;
	private AudioGenerator mAudioGenerator = new AudioGenerator(Constants.SAMPLE_RATE);
	private Handler mHandler;
	Handler mDebugHandler;
	private Context mContext;
	
	//Sound stuff
	private double[] silenceSoundDouble; //TODO factor this away
	private byte[] tickSoundByte;
	private byte[] tockSoundByte;
	private double[] silentTickSoundDouble;
	
	private Message msg;
	private int currentBeat = 1;
	private Vibrator mVibrator;
	boolean syncStartFlag = false; //set everytime calcsilence is called, which is when change happens
	SimpleDateFormat df2 = new SimpleDateFormat("HH:mm:ss.SSSZ");
	
	//Preferences
	private SharedPreferences mPrefs;
	private int prefVibrateLength;
	private int prefBeatVibrateLength;
	public boolean prefClick = true;
	public boolean prefSyncOn;
	private boolean prefSyncStartOn;
	
	//variables for the complex caclulations
	private int numCycleSamples;
	private int avOffsetPercent;
	private int startOffsetPercent;
	private int va;
	private int t;
	private int s1;
	int N;
	int va2;
	int s0;
	public boolean prefMute = false;
	private long prefNTPOffset;
	
	
	public Metronome(Handler handler, Handler debugHandler, Context context) {
		mAudioGenerator.createPlayer();
		this.mDebugHandler = debugHandler;
		this.mHandler = handler;
		this.mContext = context;
		this.mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
		
		//Setup prefs
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mPrefs.registerOnSharedPreferenceChangeListener(this);
		
		//extract initial pref values
		
		//extract On-the-fly pref values
		prefVibrateLength = Integer.parseInt(
				mPrefs.getString("vibrate_length", "50"));
		prefBeatVibrateLength = Integer.parseInt(
				mPrefs.getString("beat_vibrate_length", "100"));
		prefClick = mPrefs.getBoolean("click", false);
		prefSyncOn = mPrefs.getBoolean("sync_on", false);
		
		//make sure prefSyncStartOn is off in the right state
		if(!prefSyncOn){
			mPrefs.edit().putBoolean("sync_start_on", false);
		}
		prefSyncStartOn = mPrefs.getBoolean("sync_start_on", false);
	}
	
	void calcNTPOffset() {
    	long time = getCurrentNetworkTime();
		prefNTPOffset = time - System.currentTimeMillis();
		logDebugAndToast("ntp", "NTP time offset = " + prefNTPOffset + " ms");
	}
	
	//Conveninence method for logging, and sending a message via mdebugHandler
	void logDebugAndToast(String tag, CharSequence text){
		Log.d(tag, text.toString());
		msg = Message.obtain();
		msg.obj = ""+text;
		mDebugHandler.sendMessage(msg);
	}
    
    public long getCurrentNetworkTime(){
	    NTPUDPClient timeClient = new NTPUDPClient();
	    
	    TimeInfo timeInfo = null;
	    try{
	    	InetAddress inetAddress = InetAddress.getByName(Constants.TIME_SERVER);
	    	timeInfo = timeClient.getTime(inetAddress);
	    }catch(Exception e){
	    	Log.e("ntp", e.toString());
	    }
	    //long returnTime = timeInfo.getReturnTime();   //local device time
	    long returnTime = timeInfo.getMessage().getTransmitTimeStamp().getTime();   //server time
	    timeInfo.getMessage().getTransmitTimeStamp().getTime();
	    Date time = new Date(returnTime);
	    
	    //TODO make this into a debug mode/verbose?
	    if(true){
    		CharSequence text = "Time from " + Constants.TIME_SERVER + ": " + time + 
    				timeInfo.getMessage().getTransmitTimeStamp().toDateString();
    		Log.d("ntp", text.toString());
    	}
	    
	    return returnTime;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs,
			String key) {
		if (key.equals("vibrate_length")) {
    		prefVibrateLength = Integer.parseInt(prefs.getString(key, "200"));
    		Log.d("prefs", "Vibrate length changed to: " + prefVibrateLength);
    	} 
		if (key.equals("beat_vibrate_length")) {
    		prefBeatVibrateLength = Integer.parseInt(prefs.getString(key, "200"));
    		Log.d("prefs", "Beat Vibrate length changed to: " 
    		+ prefBeatVibrateLength);
    	} 
		if (key.equals("click")) {
			prefClick = prefs.getBoolean("click", true);
			calcSilence();
    	} 
		
		//make sure sync_start_on is off if sync_on is false
		//TODO remove? doesn't seem to do anything...
		if (key.equals("sync_on")){
			if(prefs.getBoolean(key, false)==false){
				mPrefs.edit().putBoolean("sync_start_on", false);
			}
		}
	}
	
	public void calcSilence() {
		if(prefSyncStartOn){
			syncStartFlag = true;
		}
		tickSoundByte = new byte[this.numTickSamples];
		tockSoundByte = new byte[this.numTickSamples];
		silentTickSoundDouble = new double[this.numTickSamples];
		numCycleSamples = (int) ((60/bpm)*Constants.SAMPLE_RATE);
		numSilenceSamples = numCycleSamples-numTickSamples;		
		silenceSoundDouble = new double[this.numCycleSamples];
		
		if(prefClick){ //Clicks sounds
			Log.d("prefs", "prefclick = " + prefClick);
			try {
				tickSoundByte = mAudioGenerator.getTockBytes(mContext);
				tockSoundByte = mAudioGenerator.getTockBytes(mContext);
			} catch (IOException e) {
				Log.e("Metronome", "IO exception" + e.getStackTrace().toString());
			}
		}else {			//sine wave sounds
			tickSoundByte = mAudioGenerator.get16BitPcm(
					mAudioGenerator.getSineWave(
							this.numTickSamples, 
							Constants.SAMPLE_RATE, 
							Constants.BEAT_SOUND_FREQ));
			
			tockSoundByte = mAudioGenerator.get16BitPcm(
					mAudioGenerator.getSineWave(
							this.numTickSamples, 
							Constants.SAMPLE_RATE, 
							Constants.NONBEAT_SOUND_FREQ));
		}

	}
	
	//round to nearest 500ms or TODO whatever is necessary for margin of error...
	public long roundLong(long x){
		long round_x = (long) (Math.ceil(x/Constants.SYNC_ROUNDING_VALUE)
				*Constants.SYNC_ROUNDING_VALUE);
		return round_x;
	}
	

	public void play() {
		calcSilence();
		

		do {
			long time1 = 0L;
			//sync time until starts?
			//TODO should this be synchronized?
			if(syncStartFlag){

				/*
				 * time 0 is the current time
				 * time1 is the start time
				 * 
				*/
				
				currentBeat = 1;

				//TODO use nanos?
				
				long time0 = System.currentTimeMillis();
				long time0NTP = time0 + prefNTPOffset;
				
				//start at time 1
				long time1NTP = roundLong(time0NTP) + Constants.SYNC_WAIT_TIME; 
				time1 = time1NTP - prefNTPOffset;
				
				//false for now since I think it might take up extra time
				if(true){
					Date date = new Date(time1NTP);
		    		CharSequence text = "Waiting until this NTP time to start: " 
	    					+ df2.format(date);
		    		logDebugAndToast("131015", text);
		    	}
				//TODO cleanup
				//gotta add two just in case previous was round down...
				
				//TODO change this to scheduledPoolExecutor?
				//Wait...
			}
			
			//Some renaming of variaables
			avOffsetPercent = mPrefs.getInt("av_offset",1) + 1;	//0-98 originall, = 1 -99
			startOffsetPercent = mPrefs.getInt("start_offset",1) + 1;
			Log.d("timing calc", "start_offset = " + startOffsetPercent);
			va = offsetPercentToOffSet(avOffsetPercent);
			t = numTickSamples;
			s1 = offsetPercentToOffSet(startOffsetPercent);
			N = numCycleSamples;
			va2 = N -t - va - s1;
			s0 = va + s1 - N;
			
			//Do the waiting as close as possible to the playing
			if(syncStartFlag){
				long now = System.currentTimeMillis();   
				while(now < time1){
					now = System.currentTimeMillis();
					Log.d("ntp", "in loop");
				}
			
				syncStartFlag = false;
			}
			
			
			if (va2> 0 ){
				playOffset(s1);
				Log.d("timing calc 1", "s1 = " + s1);
				
				//visual
				visual();
				vibrate();
				
				playOffset(va);
				Log.d("timing calc 1", "va = " + va);
				
				//AUDIO
				audio();
				Log.d("timing calc 1", "t = " + t);
				
				playOffset(va2);
				Log.d("timing calc 1", "va2 = " + va2);
				
				Log.d("timing calc 1", "N = " + N);
			}else{
				playOffset(s0);
				Log.d("timing calc 2", "s0 = " + s0);
				//AUDIO
				audio();
				Log.d("timing calc 2", "t = " + t);
				
				playOffset(s1 - s0 - t);
				Log.d("timing calc 2", "av =s1-s0-t= " + (s1 - s0 - t));
				
				//visual
				visual();
				vibrate();
				
				playOffset(N-s1);
				Log.d("timing calc 2", "N-s1 = " + (N-s1));

				Log.d("timing calc 2", "N = " + (s1+va+t+va2));
			}
			
			
			currentBeat++;
			
			//in dev mode, we want the counts to go up forever,
			//so we can do measurements on metronome accuracy
			if(!Constants.DEV_MODE){
				if(currentBeat > beat)
					currentBeat = 1;
			}
		} while(play);
	}
	
	
	/*
	 * Input: (0-98) offsetPercent
	 * Output: plays a period of silence equivalent to 1-99% of the beat
	 */
	void playOffsetPercent(int offsetPercent){
		mAudioGenerator.writeSoundDouble(Arrays.copyOfRange(
				silenceSoundDouble, 0, offsetPercentToOffSet(offsetPercent)));
	}
	
	/*
	 * Input: 0 < offset < numSamples
	 * Output: plays a period of silence equivalent to 1-99% of the beat
	 */
	
	//TODO factory this away to a common "play silence" method, perhaps on audiogenerator?
	//Invalid playoffset errors would happen when the silence period is negative
	void playOffset(int offset){
		try{
		mAudioGenerator.writeSoundDouble(Arrays.copyOfRange(
				silenceSoundDouble, 0, offset));
		}
		catch(Exception e){
			Log.d("Timing calc", "playoffset error: offset = " + offset);
			msg = Message.obtain();
			msg.obj = "invalid offset combination";
			mDebugHandler.sendMessage(msg);
		}
	}
	
	/*
	 * Input: (0-98)
	 * Output 1-99% of the sample per beat
	 */
	int offsetPercentToOffSet(int offsetPercent){
		return (int) Math.ceil(numSilenceSamples/100.0 * offsetPercent) - 1;
	}


	void audio() {
		if(prefMute){
			mAudioGenerator.writeSoundDouble(silentTickSoundDouble);
		}else{
			if(currentBeat == 4){ //temporary hack to make beats more easily align
				mAudioGenerator.writeSoundByte(tockSoundByte, "1");
			}else{
				mAudioGenerator.writeSoundByte(tickSoundByte, "2");				
			}
		}
	}


	void vibrate() {
		if(!prefMute ){
			if(currentBeat == 1){
				mVibrator.vibrate(prefBeatVibrateLength);
			}else{
				mVibrator.vibrate(prefVibrateLength);
			}
		}
	}


	void visual() {
		msg = Message.obtain();
		msg.obj = ""+currentBeat;
		mHandler.sendMessage(msg);
	}


	
	
	public void stop() {
		play = false;
		mAudioGenerator.destroyAudioTrack();
		mVibrator.cancel();
	}

	//getters and setters
	public double getBpm() {
		return bpm;
	}

	public void setBpm(int bpm) {
		this.bpm = bpm;
	}

	public int getNoteValue() {
		return noteValue;
	}

	public void setNoteValue(int bpmetre) {
		this.noteValue = bpmetre;
	}

	public int getBeat() {
		return beat;
	}

	public void setBeat(int beat) {
		this.beat = beat;
	}
}
