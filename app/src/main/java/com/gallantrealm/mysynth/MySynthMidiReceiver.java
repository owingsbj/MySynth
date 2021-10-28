package com.gallantrealm.mysynth;

import android.media.midi.MidiReceiver;

import java.io.IOException;

class MySynthMidiReceiver extends MidiReceiver {
    private final MySynthMidi mySynthMidi;
    byte[] messageBytes = new byte[3];
    int messageByteIndex = 0;
    int messageLength = 0;

    public MySynthMidiReceiver(MySynthMidi mySynthMidi) {
        this.mySynthMidi = mySynthMidi;
    }

    public void onSend(byte[] data, int offset, int count, long timestamp)
            throws IOException {
        for (int i = 0; i < count; i++) {
            byte b = data[offset + i];
            messageBytes[messageByteIndex] = b;
            if (messageByteIndex == 0) {
                messageLength = mySynthMidi.getMidiMessageLength(b);
            }
            messageByteIndex += 1;
            if (messageByteIndex >= messageLength) {
                if (mySynthMidi.midiLogStream != null) {
                    if ((messageBytes[0] & 0xff) != 0xf8
                            && (messageBytes[0] & 0xff) != 0xfe) { // don't log
                        // timing clocks
                        // and active
                        // sensing
                        mySynthMidi.midiLogStream.format("%02x%02x%02x\n", messageBytes[0],
                                messageBytes[1], messageBytes[2]);
                    }
                }
                mySynthMidi.processMidi(messageBytes[0], messageBytes[1], messageBytes[2]);
                messageByteIndex = 0;
                messageBytes[0] = 0;
                messageBytes[1] = 0;
                messageBytes[2] = 0;
            }
        }
    }
}
