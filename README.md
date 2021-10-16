# MySynth
Common Android Synthesizer Framework

Use MySynth to create your own Android music synthesizer app.  All of the JNI for using either OpenSL or AAudio is provided.
All you need to do is implement a subclass of AbstractInstrument and then create and control the synthesizer using the MySynth class.
See the [MySynth wiki](https://github.com/owingsbj/MySynth/wiki) for more details.

Note:  Even though MySynth has minSdkVersion=28, it can actually support down to version 14.  Use the following in a containing app to ignore
the sdk version:

    <uses-sdk tools:overrideLibrary="com.gallantrealm.mysynth" />

You should also look at MyAndroid for common widgets for building a synthesizer app (such as the keyboard).

## Acknowledgments

MySynth contains modified versions of the following open source:

- _android-midi-lib_ by Alex Leffelman.  The current original is at https://github.com/LeffelMania/android-midi-lib.  MIT License overall, Apache License, Version 2.0 in individual files.
- _USB-MIDI-Driver_ by Kaoru Shoji.  The current original is at https://github.com/kshoji/USB-MIDI-Driver.   Apache License, Version 2.0
- _javax.sound.midi porting for Android_ also by Kaoru Shoji.  The current original is at https://github.com/kshoji/javax.sound.midi-for-Android.  Apache License, Version 2.0
- _Java Wav File IO_ by Dr. Andrew Greensted.  At http://www.labbookpages.co.uk/audio/javaWavFiles.html.  License at http://www.labbookpages.co.uk/home/licences.html.

I've left the original package names should you want to compare and update.  I highly appreciate the efforts of these developers and their generous donation to the open source community.
