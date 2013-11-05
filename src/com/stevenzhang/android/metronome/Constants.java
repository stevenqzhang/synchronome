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
	
	/*
	 * Sync rounding value
	 * 
	 * Acceptable ranges:
	 * 
	 * Date		type of server		phone		VALUE			+,-		comments
	 * 131016 	local				xiaomi		500				0		too low.
	 */
	
	public static final double SYNC_ROUNDING_VALUE = 5000; //round to neares ___ in ms
	//time to wait after that, should be the same as above
	public static final long SYNC_WAIT_TIME = 2000; 
//	public enum SoundTypeChoices{
//		CLICK, SINE
//	};
	//public static SoundTypeChoices SOUND_TYPE = SoundTypeChoices.CLICK;
	

	
	public static final int BEAT_SOUND_FREQ = 2440;
	public static final int NONBEAT_SOUND_FREQ = 6440;

	public static final boolean FLUSH_PREFERENCES = false;
	
	
	//pool.ntp.org would be ideal to avoid busy servers, but we need to pick the SAME server.
	public static final String TIME_SERVER = "wwv.nist.gov";
	
	//public static final String TIME_SERVER = "nist1-chi.ustiming.org";
	//public static final String TIME_SERVER = "time-d.nist.gov";
	//public static final String TIME_SERVER = "nisttime.carsoncity.k12.mi.us";
	
}



