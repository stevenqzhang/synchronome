Synchronome
===========

A synchronized tactile metronome for Android.

# What does the app do?

* A standard metronome, with
	* __Tactile feedback__. Phone vibrates in sync with audio and visual display.
	* __Sync__. Ability to sync in time with other phones running this app (as long as internet access is available).


# How to use

## Install app
Install the apk found in bin/synchronome.apk, or build it yourself (I used ADT/eclipse, probably affects the folder strcuture...)

## Startup

1. Start the app.

2. Press the start button. If you just want to use Synchronome on one phone, skip to step 4
3. To sync the metronome between multiple phone:
	1. Make sure you're connected to the internet.
	2. Set the BPM on both phones to be the same.
	3. Press "start" on the phones within ~2second of each other, but DON'T press them at the same time. Try to space it out. Otherwise you might not get accurate NTP time on both phones.
		* _You'll see a message popup with  the NTP time offset. This is merely informative and shows the difference between your phone's system time and NTP time. NTP time is used to sync the phones._
	4. The first time you run it for a given BPM and phone combination, you might have to adjust the offset sliders (see below) to make them sync
	5. _If you're consistently getting bad sync results, you can always use the metronomes with sync off, and manually adjust the offset sliders. Wait until I make a better version :)_

4. If you want to stop the metronome, but don't need to change the BPM, use the Mute button. That way multiple metronomes will be kept in sync.

5. If you want to adjust the BPM on just one phone, you can adjust the BPM on the fly. If you want to adjust __and__ sync on multiple phones though, you'll have to press stop on both phones and repeat step 3. 

## Adjustment

Playing sounds, vibrations and visuals accurately and precisely __and__ sync'ing across different phones is tricky and probably impossible to get right (if you think otherwise, I'd love to hear from you!), especially on a non real time OS like Android.

(Plus, ears are extremely sensitive to slight audio lag, [on the order of milliseconds](http://www.silcom.com/~aludwig/EARS.htm))

Two sliders are provided to manually compensate for these problems:

1. __A/V offset__. Use this to minimize the delay between audio and visual+tactile.
2. __Start offset__. Adjust this if your multiple phones are slightly off even while syncing with NTP. (see below)



Time is synced using NTP. I use the [org.apache.commons.net.ntp](commons.apache.org/proper/commons-net/apidocs/org/apache/commons/net/ntp/package-summary.html) package

# Known issues

* __NTP doesn't always the exact time__. Variations between phones and network topology differences means that NTP time will always be a few milliseconds off. To compensate, a slider is provided

* __Localized beat drift__. Synchronome is highly accurate _over time_, not necessarily for a small time period: I've ran this on multiple phones (on Android 2.3 and 4.2) and found that there's very little drift (1 beat per 10 minutes) compared to a standard hardware metronome. There is sometimes tendency for drift to occur within a few beats (vibrate seems laggy, for example), but it always resets itself.

# Thanks

To MasterEx and his [open source BeatKeeper](https://github.com/MasterEx/BeatKeeper) code that provided the initial metronome code.

# Contact

github@firstlast.com, where first = my first name, last = my last name.