package com.stevenzhang.android.metronome;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Date;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MetronomeActivity extends Activity implements OnSharedPreferenceChangeListener {
	
	private final short minBpm = 30;
	private final short maxBpm = 200;
	
	private short bpm = 100;
	private short noteValue = 4;
	private short beats = 4;
	private short volume;
	private short initialVolume;

	private AudioManager audio;
    private MetronomeAsyncTask metroTask;
    
    private Button plusButton;
    private Button minusButton;
    private TextView currentBeat;
    
    Handler mMetronomeBeatHandler;
    Handler mMetronomeDebugHandler;
    Handler mFetchParamsHandler;
    
    //flag is true when the online Bpm has changed
    public boolean onlineBpmHasChanged = false;
    
    //preferences
	private SharedPreferences mPrefs;
	private Toast mToast;
    
    // have in mind that: http://stackoverflow.com/questions/11407943/this-handler-class-should-be-static-or-leaks-might-occur-incominghandler
    // in this case we should be fine as no delayed messages are queued
    Handler getMetronomeBeatHandler() {
    	return new Handler() {
            @Override
            //SZ 130930: this colors the very first beat in the metronome
            public void handleMessage(Message msg) {
            	String message = (String)msg.obj;
            	if(message.equals("1"))
            		currentBeat.setTextColor(Color.GREEN);
            	else
            		currentBeat.setTextColor(getResources().getColor(R.color.yellow));
            	currentBeat.setText(message);
            }
        };
    }
	
    /** Called when the activity is first created. */
   @SuppressLint("ShowToast")
	//todo refactor into seperate things?
    @Override
    public void onCreate(Bundle savedInstanceState) {    	
        super.onCreate(savedInstanceState);
        
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        
        mToast = Toast.makeText(getApplicationContext(), "empty", Toast.LENGTH_SHORT);
        
        //Flush
        if(Constants.FLUSH_PREFERENCES){
        	Editor e = mPrefs.edit();
        	e.clear(); 
        	e.commit();
        }
        
        setContentView(R.layout.main);
        metroTask = new MetronomeAsyncTask(this);
        
        /* Set values and listeners to buttons and stuff */
        
        TextView bpmText = (TextView) findViewById(R.id.bps);
        bpmText.setText(""+bpm);
        
        TextView timeSignatureText = (TextView) findViewById(R.id.timesignature);
        timeSignatureText.setText(""+beats+"/"+noteValue);
        
        plusButton = (Button) findViewById(R.id.plus);
        plusButton.setOnLongClickListener(plusListener);
        
        minusButton = (Button) findViewById(R.id.minus);
        minusButton.setOnLongClickListener(minusListener);
        
        currentBeat = (TextView) findViewById(R.id.currentBeat);
        currentBeat.setTextColor(Color.GREEN);
        
        Spinner beatSpinner = (Spinner) findViewById(R.id.beatspinner);
        ArrayAdapter<Beats> arrayBeats =
        new ArrayAdapter<Beats>(this,
      	      android.R.layout.simple_spinner_item, Beats.values());
        beatSpinner.setAdapter(arrayBeats);
        beatSpinner.setSelection(Beats.four.ordinal());
        arrayBeats.setDropDownViewResource(R.layout.spinner_dropdown);
        beatSpinner.setOnItemSelectedListener(beatsSpinnerListener);
        
        Spinner noteValuesdSpinner = (Spinner) findViewById(R.id.notespinner);
        ArrayAdapter<NoteValues> noteValues =
        new ArrayAdapter<NoteValues>(this,
      	      android.R.layout.simple_spinner_item, NoteValues.values());
        noteValuesdSpinner.setAdapter(noteValues);
        noteValues.setDropDownViewResource(R.layout.spinner_dropdown);
        noteValuesdSpinner.setOnItemSelectedListener(noteValueSpinnerListener);
        
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        
    	initialVolume = (short) audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        volume = initialVolume;
        
        SeekBar volumebar = (SeekBar) findViewById(R.id.volumebar);
        volumebar.setMax(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volumebar.setProgress(volume);
        volumebar.setOnSeekBarChangeListener(volumeListener);
        
        //TODO make these constants
        SeekBar avOffsetBar = (SeekBar) findViewById(R.id.avOffsetBar);
        avOffsetBar.setProgress((mPrefs.getInt("av_offset", 1) + 49) % 98);
        avOffsetBar.setMax(98); //todo retrieve this from preferences?
        avOffsetBar.setOnSeekBarChangeListener(avOffsetListener);
        
        SeekBar startOffsetBar = (SeekBar) findViewById(R.id.startOffsetBar);
        startOffsetBar.setProgress(mPrefs.getInt("start_offset", 1));
        startOffsetBar.setMax(98); //todo retrieve this from preferences?
        startOffsetBar.setOnSeekBarChangeListener(startOffsetListener);
    }
    
    

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs,
			String key) {
		
		if (key.equals("av_offset")){ 
			Log.d("Timings", key + ": "+ prefs.getInt(key, -1));
		}
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(Menu.NONE, 0, 0, "Preferences...");
    	return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case 0:
    			startActivity(new Intent(this, PreferencesActivity.class));
    			return true;
    	}
    	return false;
    }
    
    
    //start stop is a button:
    /*<Button
    android:id="@+id/startstop"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:onClick="onStartStopClick"
    android:text="@string/start"
    android:textSize="80dp" />
    */

//=====Stop/start buttons===

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public synchronized void onStartStopClick(View view) {
    	Button button = (Button) view;
    	String buttonText = button.getText().toString();
    	
    	//start
    	if(buttonText.equalsIgnoreCase("start")) {
    		button.setText(R.string.stop);
    		
    		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
    		{	metroTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
    		}else{
    			metroTask.execute();   
    		}
    			
    	} else { //stop
    		button.setText(R.string.start);    	
    		
    		metroTask.stop();
    		metroTask = new MetronomeAsyncTask(this);
    		
    		Runtime.getRuntime().gc();
    	}
    }
    
    public synchronized void onMuteUnmuteClick(View view) {
    	Button button = (Button) view;
    	String buttonText = button.getText().toString();
    	
    	//start
    	if(buttonText.equalsIgnoreCase("mute")) {
    		button.setText("Unmute");
    		metroTask.mute();
    	} else { 
    		button.setText("Mute");
    		metroTask.unmute();
    	}
    }
    
//=====Minus and plus buttons===
    private void maxBpmGuard() {
        if(bpm >= maxBpm) {
        	plusButton.setEnabled(false);
        	plusButton.setPressed(false);
        } else if(!minusButton.isEnabled() && bpm>minBpm) {
        	minusButton.setEnabled(true);
        }    	
    }
    
    public void onPlusClick(View view) {
    	bpm++;
    	refreshBpm();
        maxBpmGuard();
    }

    //Helper method, called whenever Bpm changes...
	void refreshBpm() {
		TextView bpmText = (TextView) findViewById(R.id.bps);
        bpmText.setText(""+bpm);
        metroTask.setBpm(bpm);
	}
    
    private OnLongClickListener plusListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			bpm+=20;
			if(bpm >= maxBpm)
				bpm = maxBpm;
	    	refreshBpm();
	        maxBpmGuard();
			return true;
		}
    	
    };
    
    private void minBpmGuard() {
        if(bpm <= minBpm) {
        	minusButton.setEnabled(false);
        	minusButton.setPressed(false);
        } else if(!plusButton.isEnabled() && bpm<maxBpm) {
        	plusButton.setEnabled(true);
        }    	
    }
    
    public void onMinusClick(View view) {
    	bpm--;
    	refreshBpm();
        minBpmGuard();
    }
    

    private OnLongClickListener minusListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			bpm-=20;
			if(bpm <= minBpm)
				bpm = minBpm;
	    	refreshBpm();
	        minBpmGuard();
			return true;
		}
    	
    };
    
    private OnSeekBarChangeListener startOffsetListener = new OnSeekBarChangeListener() {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			if(fromUser){
			Log.d("timing listener", "start_offset " + progress);
			mPrefs.edit().putInt("start_offset", progress).commit();
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// Do nothing.  Exists to satisfy Activity interface.
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			//do nothing
		}   	
    	
    };
    
    private OnSeekBarChangeListener avOffsetListener = new OnSeekBarChangeListener() {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			if(fromUser){
				int offset = (progress - 49 + 98) % 98;
				Log.d("timing listener", "av_offset " + offset);
				mPrefs.edit().putInt("av_offset", offset).commit();
				//modular arithmetic to make center of slider the important part
			}
			
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// Do nothing.  Exists to satisfy Activity interface.
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			//TODO remove?
//			mPrefs.edit().putInt("av_offset", seekBar.getProgress()).commit();
//			Toast.makeText(getApplicationContext(), 
//					"New av sync value: " + seekBar.getProgress(), Toast.LENGTH_SHORT).show(); 
		}   	
    	
    };
    
    private OnSeekBarChangeListener volumeListener = new OnSeekBarChangeListener() {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			//do nothing
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			seekBar.getProgress();
			
			// Do nothing.  Exists to satisfy Activity interface.
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// Do nothing.  Exists to satisfy Activity interface.
		}   	
    	
    };
    
    private OnItemSelectedListener beatsSpinnerListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			Beats beat = (Beats) arg0.getItemAtPosition(arg2);
			TextView timeSignature = (TextView) findViewById(R.id.timesignature);
			timeSignature.setText(""+beat+"/"+noteValue);
			metroTask.setBeat(beat.getNum());
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// Do nothing.  Exists to satisfy Activity interface.
			
		}
    	
    };
    
    //====How spinner is implemented===
    private OnItemSelectedListener noteValueSpinnerListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			NoteValues noteValue = (NoteValues) arg0.getItemAtPosition(arg2);
			TextView timeSignature = (TextView) findViewById(R.id.timesignature);
			timeSignature.setText(""+beats+"/"+noteValue);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// Do nothing.  Exists to satisfy Activity interface.
		}
    	
    };

    //==SZ: this handles the remainder of the keys, namely the volume up and down buttons===//
    @Override
    public boolean onKeyUp(int keycode, KeyEvent e) {
    	SeekBar volumebar = (SeekBar) findViewById(R.id.volumebar);
    	volume = (short) audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        switch(keycode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN: 
                volumebar.setProgress(volume);
            	break;                
        }

        return super.onKeyUp(keycode, e);
    }
    
    public void onBackPressed() {
    	metroTask.stop();
    	Runtime.getRuntime().gc();
		audio.setStreamVolume(AudioManager.STREAM_MUSIC, initialVolume, 
				AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    	//TODO registerOnSharedPreferenceChangeListener
		finish();    
    }
    
    //===============

    private Handler getMetronomeDebugHandler() {
    	return new Handler() {
            @Override
            public void handleMessage(Message msg) {
            	CharSequence text = ((CharSequence) msg.obj);
            	
        		mToast.setText(text);
        		mToast.show();            	
        		Log.d("ToastText", text.toString());
            }
        };
    }
    
      
    //Metronome background task
    //Android API says AsyncTask should only be used for short operations, may need to change this to FutureTask
    //TODO "If you need to keep threads running for long periods of time, it is highly recommended you use the various APIs provided by the java.util.concurrent pacakge such as Executor, ThreadPoolExecutor and FutureTask."
    class MetronomeAsyncTask extends AsyncTask<Void, Void,  String> 
    {	public Metronome metronome;
	private long prefNTPOffset;
	private Message msg;
    	
    	MetronomeAsyncTask(Context context) {
            mMetronomeBeatHandler = getMetronomeBeatHandler();
            mMetronomeDebugHandler = getMetronomeDebugHandler();
            
            //TODO if statement here
            calcNTPOffset();
            
    		metronome = new Metronome(mMetronomeBeatHandler, mMetronomeDebugHandler, context, prefNTPOffset);
    	}
    	
    	
    	void calcNTPOffset() {
        	long time = getCurrentNetworkTime();
    		prefNTPOffset = time - System.currentTimeMillis();
    		Log.d("ntp", "NTP time offset = " + prefNTPOffset + " ms");
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
        		logDebugAndToast("ntp", text.toString());
        	}
    	    
    	    return returnTime;
    	}
    	
    	//Conveninence method for logging, and sending a message via mdebugHandler
    	void logDebugAndToast(String tag, CharSequence text){
    		Log.d(tag, text.toString());
    		msg = Message.obtain();
    		msg.obj = ""+text;
    		mMetronomeDebugHandler.sendMessage(msg);
    	}

		public void mute() {
			if(metronome != null)
				metronome.prefMute = true;
		}
		
		public void unmute() {
			if(metronome != null)
				metronome.prefMute = false;
		}

		protected String doInBackground(Void... params) {
			metronome.setBeat(beats);
			metronome.setNoteValue(noteValue);
			metronome.setBpm(bpm);
			metronome.play();
			
			return null;			
		}
		
		public void stop() {
			if(metronome != null){
				metronome.stop();
				metronome = null;
			}
		}
		
		public void setBpm(short bpm) {
			metronome.setBpm(bpm);
			metronome.calcSilence();
			//TODO refactor so that calcSilence is in setBpm?
		}
		
		public void setBeat(short beat) {
			if(metronome != null)
				metronome.setBeat(beat);
		}
    }

}