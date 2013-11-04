// Steven Zhang github@stevenzhang.com
//Some constant flags

package com.stevenzhang.android.metronome;

public final class Constants {
	public static final boolean DEV_MODE = false;
	
	
	/*
	 * Vibration ranges: use preferences now
	 * 
	 * Acceptable ranges:
	 * 
	 * Date		phone		VALUE		+,-,0	comments
	 * 131016 	xiaomi		100			-		can't feel it well
	 * 131016	xiaomi		50			0
	 * 			g2			100, 200			++		allmost too much
	 * 			g2			100			+
	 */
	@Deprecated
	public static final int NONBEAT_VIBRATE_LENGTH = 50; //length that phone should vibrate in ms, for nonbeat
	@Deprecated
	public static final int BEAT_VIBRATE_LENGTH = 100; //200 is long, 100 can still be felt
	public static final int SAMPLE_RATE = 8000;
	
	//====FETCHING STUFF
	public static final boolean FETCH_PARAMS_ON = true; //whether to have fetch params mode or not
	
	/*
	 * Frequency of fetching from server
	 * 
	 * Acceptable ranges:
	 * 
	 * Date		type of server		phone		VALUE			+,-		comments
	 * 131016 	local				xiaomi		100, 200		+		works fine, no lag perceived.
	 * 131016 	local				g2			100				+ 		no problem
	 * 131016	local				g2			100				+		no problem
	 * 131017 	freehostia			g2/xiaomi	500				-		changing a few times makes it bad
	 */
	public static final long FETCH_FREQUENCY = 1000;
	
	//====SYNCING STUFF===
	public static final boolean SYNC_START_ON = true; //if true, then will sync starts of beats with global time
	
	/*
	 * Sync rounding value
	 * 
	 * Acceptable ranges:
	 * 
	 * Date		type of server		phone		VALUE			+,-		comments
	 * 131016 	local				xiaomi		500				0		too low.
	 */
	
	public static final double SYNC_ROUNDING_VALUE = 2000; //round to neares ___ in ms
	//time to wait after that, should be the same as above
	public static final long SYNC_WAIT_TIME = 2000; 
//	public enum SoundTypeChoices{
//		CLICK, SINE
//	};
	//public static SoundTypeChoices SOUND_TYPE = SoundTypeChoices.CLICK;
	

	
	public static final int BEAT_SOUND_FREQ = 2440;
	public static final int NONBEAT_SOUND_FREQ = 6440;
	
	public static final String SERVER_URL = "http://gcstudent.stevenzhang.com/metronome/";


	public static final int DEFAULT_SESSION_ID = 31415;


	public static final boolean FLUSH_PREFERENCES = false;
	
	public static final String TIME_SERVER = "nist1-chi.ustiming.org";
	//public static final String TIME_SERVER = "time-d.nist.gov";
	//public static final String TIME_SERVER = "nisttime.carsoncity.k12.mi.us";
	
}



