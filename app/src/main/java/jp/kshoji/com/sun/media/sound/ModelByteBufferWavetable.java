/*
 * Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package jp.kshoji.com.sun.media.sound;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ShortBuffer;
import jp.kshoji.javax.sound.sampled.AudioFormat;
import jp.kshoji.javax.sound.sampled.AudioInputStream;
import jp.kshoji.javax.sound.sampled.AudioSystem;
import jp.kshoji.javax.sound.sampled.AudioFormat.Encoding;

/**
 * Wavetable oscillator for pre-loaded data.
 *
 * @author Karl Helgason
 */
public class ModelByteBufferWavetable implements ModelWavetable {

	private class Buffer8PlusInputStream extends InputStream {

		private boolean bigendian;
		private int framesize_pc;
		private DataInputStream stream1;
		private DataInputStream stream8;
		private byte[] buff1;
		private byte[] buff8;

		public Buffer8PlusInputStream() {
			framesize_pc = format.getFrameSize() / format.getChannels();
			bigendian = format.isBigEndian();
			stream1 = new DataInputStream(buffer.getInputStream());
			stream8 = new DataInputStream(buffer8.getInputStream());
		}

		public int read(byte[] b, int off, int len) throws IOException {
			int avail = available();
			if (avail <= 0)
				return -1;
			if (len > avail)
				len = avail;
			int len8 = len / (framesize_pc + 1);
			int len1 = len8 * framesize_pc;
			if (buff1 == null || buff1.length < len1)
				buff1 = new byte[len1];
			if (buff8 == null || buff8.length < len1)
				buff8 = new byte[len8];
			stream1.readFully(buff1);
			stream8.readFully(buff8);
			int pos = 0;
			int pos2 = 0;
			if (bigendian) {
				for (int i = 0; i < len; i += (framesize_pc + 1)) {
					System.arraycopy(buff1, pos, b, i, framesize_pc);
					System.arraycopy(buff8, pos2, b, i + framesize_pc, 1);
					pos += framesize_pc;
					pos2 += 1;
				}
			} else {
				for (int i = 0; i < len; i += (framesize_pc + 1)) {
					System.arraycopy(buff8, pos2, b, i, 1);
					System.arraycopy(buff1, pos, b, i + 1, framesize_pc);
					pos += framesize_pc;
					pos2 += 1;
				}
			}
			return len;
		}

		public long skip(long n) throws IOException {
			int avail = available();
			if (avail <= 0)
				return -1;
			if (n > avail)
				n = avail;
			long n8 = n / (framesize_pc + 1);
			long n1 = n8 * framesize_pc;
			return stream1.skip(n1) + stream8.skip(n8);
		}

		public int read(byte[] b) throws IOException {
			return read(b, 0, b.length);
		}

		public int read() throws IOException {
			byte[] b = new byte[1];
			int ret = read(b, 0, 1);
			if (ret == -1)
				return -1;
			return 0 & 0xFF;
		}

		public boolean markSupported() {
			return stream1.markSupported() && stream8.markSupported();
		}

		public int available() throws IOException {
			return stream1.available() + stream8.available();
		}

		public synchronized void mark(int readlimit) {
			int readlimit8 = readlimit / (framesize_pc + 1);
			int readlimit1 = readlimit8 * framesize_pc;
			stream1.mark(readlimit1);
			stream8.mark(readlimit8);
		}

		public synchronized void reset() throws IOException {
			stream1.reset();
			stream8.reset();
		}
	}

	private float loopStart = -1;
	private float loopLength = -1;
	private ModelByteBuffer buffer;
	private ModelByteBuffer buffer8 = null;
	private AudioFormat format = null;
	private float pitchcorrection = 0;
	private float attenuation = 0;
	private int loopType = LOOP_TYPE_OFF;

	public ModelByteBufferWavetable(ModelByteBuffer buffer) {
		this.buffer = buffer;
	}

	public ModelByteBufferWavetable(ModelByteBuffer buffer, float pitchcorrection) {
		this.buffer = buffer;
		this.pitchcorrection = pitchcorrection;
	}

	public ModelByteBufferWavetable(ModelByteBuffer buffer, AudioFormat format) {
		this.format = format;
		this.buffer = buffer;
	}

	public ModelByteBufferWavetable(ModelByteBuffer buffer, AudioFormat format, float pitchcorrection) {
		this.format = format;
		this.buffer = buffer;
		this.pitchcorrection = pitchcorrection;
	}

	public void set8BitExtensionBuffer(ModelByteBuffer buffer) {
		buffer8 = buffer;
	}

	public ModelByteBuffer get8BitExtensionBuffer() {
		return buffer8;
	}

	public ModelByteBuffer getBuffer() {
		return buffer;
	}

	public AudioFormat getFormat() {
		if (format == null) {
			if (buffer == null)
				return null;
			InputStream is = buffer.getInputStream();
			AudioFormat format = null;
			try {
				format = AudioSystem.getAudioFileFormat(is).getFormat();
			} catch (Exception e) {
				// e.printStackTrace();
			}
			try {
				is.close();
			} catch (IOException e) {
				// e.printStackTrace();
			}
			return format;
		}
		return format;
	}

	public AudioFloatInputStream openStream() {
		if (buffer == null)
			return null;
		if (format == null) {
			InputStream is = buffer.getInputStream();
			AudioInputStream ais = null;
			try {
				ais = AudioSystem.getAudioInputStream(is);
			} catch (Exception e) {
				// e.printStackTrace();
				return null;
			}
			return AudioFloatInputStream.getInputStream(ais);
		}

		if (buffer8 != null) {
			if (format.getEncoding().equals(Encoding.PCM_SIGNED) || format.getEncoding().equals(Encoding.PCM_UNSIGNED)) {
				InputStream is = new Buffer8PlusInputStream();
				AudioFormat format2 = new AudioFormat(format.getEncoding(), format.getSampleRate(), format.getSampleSizeInBits() + 8, format.getChannels(), format.getFrameSize() + (1 * format.getChannels()), format.getFrameRate(),
						format.isBigEndian());

				AudioInputStream ais = new AudioInputStream(is, format2, buffer.capacity() / format.getFrameSize());
				return AudioFloatInputStream.getInputStream(ais);
			}
		}

		if (buffer.shortArray() != null) {
			return AudioFloatInputStream.getInputStream(format, buffer.shortArray(), (int) buffer.arrayOffset() / 2, (int) buffer.capacity() / 2);
		}

		if (buffer.array() != null) {
			return AudioFloatInputStream.getInputStream(format, buffer.array(), (int) buffer.arrayOffset(), (int) buffer.capacity());
		}

		if (format.getEncoding().equals(Encoding.PCM_SIGNED))
			if (format.getSampleSizeInBits() == 16) {
				ShortBuffer shortbuffer = buffer.asShortBuffer();
				if (shortbuffer != null)
					return AudioFloatInputStream.getInputStream(format, shortbuffer);
			}

		return AudioFloatInputStream.getInputStream(new AudioInputStream(buffer.getInputStream(), format, buffer.capacity()));
	}

	public int getChannels() {
		return getFormat().getChannels();
	}

	public ModelOscillatorStream open(float samplerate) {
		// ModelWavetableOscillator doesn't support ModelOscillatorStream
		return null;
	}

	// attenuation is in cB
	public float getAttenuation() {
		return attenuation;
	}
	// attenuation is in cB
	public void setAttenuation(float attenuation) {
		this.attenuation = attenuation;
	}

	public float getLoopLength() {
		return loopLength;
	}

	public void setLoopLength(float loopLength) {
		this.loopLength = loopLength;
	}

	public float getLoopStart() {
		return loopStart;
	}

	public void setLoopStart(float loopStart) {
		this.loopStart = loopStart;
	}

	public void setLoopType(int loopType) {
		this.loopType = loopType;
	}

	public int getLoopType() {
		return loopType;
	}

	public float getPitchcorrection() {
		return pitchcorrection;
	}

	public void setPitchcorrection(float pitchcorrection) {
		this.pitchcorrection = pitchcorrection;
	}
}
