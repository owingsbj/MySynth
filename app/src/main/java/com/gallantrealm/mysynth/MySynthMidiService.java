package com.gallantrealm.mysynth;

import android.content.Context;
import android.media.midi.MidiDeviceService;
import android.media.midi.MidiReceiver;

import java.io.IOException;

/**
 * This version of MIDI support is to be used within a MidiDeviceService.  Create an instance
 * of this class and use it within your MidiDeviceService subclass.
 */
public class MySynthMidiService extends MySynthMidi {

    MidiReceiver mySynthMidiReceiver;

    /**
     * Constructor.
     * @param service The containing service, a MidiDeviceService subclass.
     * @param synth an instance of MySynth that provides the synthesis
     * @param callbacks  callbacks to handle MIDI related events
     */
    public MySynthMidiService(MidiDeviceService service, MySynth synth, Callbacks callbacks) {
        super(service, synth, callbacks);
        mySynthMidiReceiver = new MySynthMidiReceiver(this);
    }

    /**
     * Use this method within the MidiDeviceService.onGetInputPortReceivers to obtain
     * the MidiReceiver for MySynthMidi.
     */
    public MidiReceiver getMidiReceiver() {
        return mySynthMidiReceiver;
    }

}
