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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is a pointer to a binary array either in memory or on disk.
 *
 * @author Karl Helgason
 * @modifier Brian Owings to get it to use direct byte buffers
 */
public class ModelByteBuffer {

	public static class ShortFormat {
		protected boolean bigEndian;
		protected boolean signed;
		public ShortFormat(boolean signed, boolean bigEndian) {
			this.bigEndian = bigEndian;
			this.signed = signed;
		}
		public boolean isBigEndian() {
			return bigEndian;
		}
		public boolean isSigned() {
			return signed;
		}
	}
	private ModelByteBuffer root = this;
	private File file;
	private long fileoffset;
	private byte[] buffer;
	private short[] sbuffer;
	private ShortFormat sformat;
	private long offset;
	private final long len;
	private MappedByteBuffer mappedBytebuffer;
	private ByteBuffer bytebuffer;
	private ShortBuffer shortbuffer;

	private class ShortArrayInputStream extends InputStream {

		private int pos = (int) arrayOffset();
		private int endpos = pos + (int) capacity();
		private int mark = pos;

		public int available() throws IOException {
			return endpos - pos;
		}

		public int read() throws IOException {
			byte[] b = new byte[1];
			int ret = read(b, 0, 1);
			if (ret == -1)
				return -1;
			return 0 & 0xFF;
		}

		private byte[] twobuffer;
		public int read(byte[] b, int off, int len) throws IOException {
			int avail = available();
			if (avail <= 0)
				return -1;
			if (len > avail)
				len = avail;
			if (len == 0)
				return 0;
			int o = 0;
			if (pos % 2 != 0) {
				pos--;
				if (twobuffer == null)
					twobuffer = new byte[2];
				read(twobuffer);
				b[o] = twobuffer[1];
				o++;
			}
			int oend = off + len;
			boolean extra_needed = oend % 2 == 1;
			oend -= oend % 2;
			int i = pos / 2;
			if (sformat.isBigEndian()) {
				if (sformat.isSigned()) {
					while (o < oend) {
						short x = sbuffer[i++];
						b[o++] = (byte) (x >>> 8);
						b[o++] = (byte) x;
					}
				} else {
					while (o < oend) {
						short x = sbuffer[i++];
						x += 32767;
						b[o++] = (byte) (x >>> 8);
						b[o++] = (byte) x;
					}
				}
			} else {
				if (sformat.isSigned()) {
					while (o < oend) {
						short x = sbuffer[i++];
						b[o++] = (byte) x;
						b[o++] = (byte) (x >>> 8);
					}
				} else {
					while (o < oend) {
						short x = sbuffer[i++];
						x += 32767;
						b[o++] = (byte) x;
						b[o++] = (byte) (x >>> 8);
					}
				}

			}
			pos = i * 2;
			if (extra_needed) {
				if (twobuffer == null)
					twobuffer = new byte[2];
				read(twobuffer);
				pos--;
				b[o] = twobuffer[0];
				o++;
			}
			return len;
		}

		public int read(byte[] b) throws IOException {
			return read(b, 0, b.length);
		}

		public long skip(long n) throws IOException {
			if (n < 0)
				return 0;
			int avail = available();
			if (n > avail)
				n = avail;
			pos += n;
			return n;
		}

		public synchronized void mark(int readlimit) {
			mark = pos;
		}

		public boolean markSupported() {
			return true;
		}

		public synchronized void reset() throws IOException {
			pos = mark;
		}

	}

	private class ByteBufferInputStream extends InputStream {

		private ByteBuffer bytebuffer = asByteBuffer();

		public int available() throws IOException {
			return bytebuffer.limit() - bytebuffer.position();
		}

		public void close() throws IOException {
		}

		public synchronized void mark(int readlimit) {
			bytebuffer.mark();
		}

		public boolean markSupported() {
			return true;
		}

		public int read() throws IOException {
			byte[] b = new byte[1];
			int ret = read(b, 0, 1);
			if (ret == -1)
				return -1;
			return 0 & 0xFF;
		}

		public int read(byte[] b, int off, int len) throws IOException {
			int avail = available();
			if (avail <= 0)
				return -1;
			if (len > avail)
				len = avail;
			bytebuffer.get(b, off, len);
			return len;
		}

		public int read(byte[] b) throws IOException {
			return read(b, 0, b.length);
		}

		public synchronized void reset() throws IOException {
			bytebuffer.reset();
		}

		public long skip(long n) throws IOException {
			int avail = available();
			if (avail <= 0)
				return -1;
			if (n > avail)
				n = avail;
			bytebuffer.position(bytebuffer.position() + (int) n);
			return n;
		}
	}

	private class RandomFileInputStream extends InputStream {

		private RandomAccessFile raf;
		private long left;
		private long mark = 0;
		private long markleft = 0;

		public RandomFileInputStream() throws IOException {
			raf = new RandomAccessFile(root.file, "r");
			raf.seek(root.fileoffset + arrayOffset());
			left = capacity();
			markleft = left;
		}

		public int available() throws IOException {
			if (left > Integer.MAX_VALUE)
				return Integer.MAX_VALUE;
			return (int) left;
		}

		public synchronized void mark(int readlimit) {
			try {
				mark = raf.getFilePointer();
				markleft = left;
			} catch (IOException e) {
				// e.printStackTrace();
			}
		}

		public boolean markSupported() {
			return true;
		}

		public synchronized void reset() throws IOException {
			raf.seek(mark);
			left = markleft;
		}

		public long skip(long n) throws IOException {
			if (n < 0)
				return 0;
			if (n > left)
				n = left;
			long p = raf.getFilePointer();
			raf.seek(p + n);
			left -= n;
			return n;
		}

		public int read(byte b[], int off, int len) throws IOException {
			if (len > left)
				len = (int) left;
			if (left == 0)
				return -1;
			len = raf.read(b, off, len);
			if (len == -1)
				return -1;
			left -= len;
			return len;
		}

		public int read(byte[] b) throws IOException {
			int len = b.length;
			if (len > left)
				len = (int) left;
			if (left == 0)
				return -1;
			len = raf.read(b, 0, len);
			if (len == -1)
				return -1;
			left -= len;
			return len;
		}

		public int read() throws IOException {
			if (left == 0)
				return -1;
			int b = raf.read();
			if (b == -1)
				return -1;
			left--;
			return b;
		}

		public void close() throws IOException {
			raf.close();
		}
	}

	private ModelByteBuffer(ModelByteBuffer parent, long beginIndex, long endIndex, boolean independent) {
		this.root = parent.root;
		this.offset = 0;
		long parent_len = parent.len;
		if (beginIndex < 0)
			beginIndex = 0;
		if (beginIndex > parent_len)
			beginIndex = parent_len;
		if (endIndex < 0)
			endIndex = 0;
		if (endIndex > parent_len)
			endIndex = parent_len;
		if (beginIndex > endIndex)
			beginIndex = endIndex;
		offset = beginIndex;
		len = endIndex - beginIndex;
		if (independent) {
			buffer = root.buffer;
			sbuffer = root.sbuffer;
			sformat = root.sformat;
			if (root.file != null) {
				file = root.file;
				fileoffset = root.fileoffset + arrayOffset();
				offset = 0;
			} else
				offset = arrayOffset();
			root = this;
		}
	}

	public ModelByteBuffer(byte[] buffer) {
		this.buffer = buffer;
		this.offset = 0;
		this.len = buffer.length;
	}

	public ModelByteBuffer(byte[] buffer, int offset, int len) {
		this.buffer = buffer;
		this.offset = offset;
		this.len = len;
	}

	public ModelByteBuffer(ShortFormat sformat, short[] sbuffer) {
		this.sformat = sformat;
		this.sbuffer = sbuffer;
		this.offset = 0;
		this.len = sbuffer.length * 2;
	}

	public ModelByteBuffer(ShortFormat sformat, short[] sbuffer, int offset, int len) {
		this.sformat = sformat;
		this.sbuffer = sbuffer;
		this.offset = offset * 2;
		this.len = len * 2;
	}

	public ModelByteBuffer(ShortFormat sformat, File file) {
		this.sformat = sformat;
		this.file = file;
		this.fileoffset = 0;
		this.len = file.length();
	}

	public ModelByteBuffer(ShortFormat sformat, File file, long offset, long len) {
		this.sformat = sformat;
		this.file = file;
		this.fileoffset = offset;
		this.len = len;
	}

	public ModelByteBuffer(File file) {
		this.file = file;
		this.fileoffset = 0;
		this.len = file.length();
	}

	public ModelByteBuffer(File file, long offset, long len) {
		this.file = file;
		this.fileoffset = offset;
		this.len = len;
	}

	public void writeTo(OutputStream out) throws IOException {
		byte[] data = array();
		if (data == null) {
			InputStream is = getInputStream();
			if (is == null)
				return;
			byte[] buff = new byte[1024];
			int ret;
			while ((ret = is.read(buff)) != -1)
				out.write(buff, 0, ret);
		} else
			out.write(data, (int) arrayOffset(), (int) capacity());
	}

	public InputStream getInputStream() {
		if (root.mappedBytebuffer != null) {
			return new ByteBufferInputStream();
		}
		if (root.sformat != null && root.sbuffer != null) {
			return new ShortArrayInputStream();
		}
		if (root.file != null && root.buffer == null) {
			try {
				return new BufferedInputStream(new RandomFileInputStream());
			} catch (IOException e) {
				// e.printStackTrace();
				return null;
			}
		}
		return new ByteArrayInputStream(array(), (int) arrayOffset(), (int) capacity());
	}

	public ModelByteBuffer subbuffer(long beginIndex) {
		return subbuffer(beginIndex, capacity());
	}

	public ModelByteBuffer subbuffer(long beginIndex, long endIndex) {
		return subbuffer(beginIndex, endIndex, false);
	}

	public ModelByteBuffer subbuffer(long beginIndex, long endIndex, boolean independent) {
		return new ModelByteBuffer(this, beginIndex, endIndex, independent);
	}

	private void initByteBuffer() {
		if (bytebuffer == null) {
			if (root.bytebuffer == null) {
				byte[] data = array();
				if (data != null) {
					bytebuffer = ByteBuffer.wrap(data, (int) arrayOffset(), (int) capacity());
				}
				return;
			}
			bytebuffer = root.bytebuffer;
			bytebuffer.position((int) arrayOffset());
			bytebuffer = bytebuffer.slice();
			bytebuffer.limit((int) capacity());
		}
	}

	public ByteBuffer asByteBuffer() {
		if (bytebuffer == null) {
			initByteBuffer();
			if (bytebuffer == null)
				return null;
		}
		ByteBuffer ret = bytebuffer.duplicate();
		ret.position(0);
		return ret;
	}

	public ShortBuffer asShortBuffer() {
		if (sformat == null)
			return null;
		if (shortbuffer == null) {
			if (!sformat.isSigned())
				return null;
			if (bytebuffer == null) {
				initByteBuffer();
				if (bytebuffer == null)
					return null;
			}
			if (sformat.isBigEndian()) {
				shortbuffer = bytebuffer.order(ByteOrder.BIG_ENDIAN).asShortBuffer();
			} else {
				shortbuffer = bytebuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
			}
		}
		ShortBuffer ret = shortbuffer.duplicate();
		ret.position(0);
		return ret;
	}

	public short[] shortArray() {
		// bjo -- added
		if (sbuffer != null) {
			return sbuffer;
		}
		// end bjo
		return root.sbuffer;
	}

	public byte[] array() {
		return root.buffer;
	}

	public long arrayOffset() {
		if (root != this)
			return root.arrayOffset() + offset;
		return offset;
	}

	public long capacity() {
		return len;
	}

	public ModelByteBuffer getRoot() {
		return root;
	}

	public File getFile() {
		return file;
	}

	public long getFilePointer() {
		return fileoffset;
	}

	public static void loadAll(Collection<ModelByteBuffer> col) throws IOException {
		loadAll(col, -1);
	}

	public static void loadAll(Collection<ModelByteBuffer> col, long memory_map_threshold) throws IOException {

		Map<File, Set<ModelByteBuffer>> buffers_per_file = new HashMap<File, Set<ModelByteBuffer>>();

		for (ModelByteBuffer mbuff : col) {
			mbuff = mbuff.root;
			if (mbuff.file == null)
				continue;
			if (mbuff.buffer != null)
				continue;
			if (mbuff.mappedBytebuffer != null)
				continue;
			Set<ModelByteBuffer> bufferlist = buffers_per_file.get(mbuff.file);
			if (bufferlist == null) {
				bufferlist = new HashSet<ModelByteBuffer>();
				buffers_per_file.put(mbuff.file, bufferlist);
			}
			bufferlist.add(mbuff);
		}

		for (Map.Entry<File, Set<ModelByteBuffer>> entry : buffers_per_file.entrySet()) {
			File file = entry.getKey();
			Set<ModelByteBuffer> bufferlist = entry.getValue();
			long tobeloaded = 0;
			int min_fileoffset = -1;
			int max_fileendoffset = 0;
			for (ModelByteBuffer mbuff : bufferlist) {
				tobeloaded += mbuff.capacity();
				if (min_fileoffset == -1 || min_fileoffset > mbuff.fileoffset)
					min_fileoffset = (int) mbuff.fileoffset;
				long fileendoffset = mbuff.fileoffset + mbuff.capacity();
				if (fileendoffset > max_fileendoffset)
					max_fileendoffset = (int) fileendoffset;
			}
			if (min_fileoffset == -1)
				min_fileoffset = 0;
			// Load into byte[] arrays if less than memory_map_threshold
			if ((tobeloaded < memory_map_threshold) && (memory_map_threshold != -1)) {
				RandomAccessFile raf = new RandomAccessFile(file, "r");
				try {
					for (ModelByteBuffer mbuff : bufferlist) {
						raf.seek(mbuff.fileoffset);
						mbuff.load(raf);
					}
				} finally {
					raf.close();
				}
				continue;
			}
			// Do not try to map more than 2 GB
			if (tobeloaded > Integer.MAX_VALUE)
				continue;
			double loadratio = ((double) tobeloaded / (double) (max_fileendoffset - min_fileoffset));
			if (loadratio > 0.90) {
				try {
					RandomAccessFile raf = new RandomAccessFile(file, "r");
					MappedByteBuffer mappedBytebuffer;
					try {
						FileChannel filechannel = raf.getChannel();
						mappedBytebuffer = filechannel.map(MapMode.READ_ONLY, min_fileoffset, max_fileendoffset - min_fileoffset);
					} finally {
						raf.close();
					}
					for (ModelByteBuffer mbuff : bufferlist) {
						mbuff.mappedBytebuffer = mappedBytebuffer;
						mbuff.mappedBytebuffer.position((int) mbuff.fileoffset - min_fileoffset);
						mbuff.bytebuffer = mappedBytebuffer.slice();
						mbuff.bytebuffer.limit((int) mbuff.capacity());
					}
					mappedBytebuffer.load();
					System.out.println("LOADING INTO FILE MAPPED BYTE BUFFER "+file+" "+min_fileoffset+" "+(max_fileendoffset - min_fileoffset));
				} catch (IOException t) {
					// Map failed, stream from disk instead
				}
			} else {
				try {
					for (ModelByteBuffer mbuff : bufferlist) {
						mbuff.map();
					}
				} catch (IOException t) {
					// Map failed, stream from disk instead
				}
			}
		}

	}

	private void load(DataInput input) throws IOException {
		if (sformat != null) {

			System.out.println("LOADING INTO A DIRECT BYTE BUFFER: "+this.file+" "+this.fileoffset+" "+this.len);

			bytebuffer = ByteBuffer.allocateDirect((int) capacity());
			shortbuffer = bytebuffer.asShortBuffer();

//			short[] sbuffer = new short[(int) (capacity() / 2)];
			byte[] buff = new byte[1024];
//			int avail = sbuffer.length * 2;
			int avail = (int) capacity();
			int o = 0;
			if (sformat.isBigEndian()) {
				while (avail != 0) {
					int readlen = (avail >= 1024) ? 1024 : avail;
					input.readFully(buff, 0, readlen);
					for (int i = 0; i < readlen; i += 2) {
//						sbuffer[o++] = ((short) ((buff[i + 1] & 0xFF) | (buff[i] << 8)));
						shortbuffer.put(((short) ((buff[i + 1] & 0xFF) | (buff[i] << 8))));
					}
					avail -= readlen;
				}
			} else {
				while (avail != 0) {
					int readlen = (avail >= 1024) ? 1024 : avail;
					input.readFully(buff, 0, readlen);
					for (int i = 0; i < readlen; i += 2) {
//						sbuffer[o++] = ((short) ((buff[i] & 0xFF) | (buff[i + 1] << 8)));
						shortbuffer.put(((short) ((buff[i] & 0xFF) | (buff[i + 1] << 8))));
					}
					avail -= readlen;
				}
			}
			if (!sformat.isSigned()) {
				for (int i = 0; i < buff.length; i++) {
					buff[i] -= 32767;
				}
			}
			offset = 0;
//			this.sbuffer = sbuffer;
		} else {

			System.out.println("LOADING INTO A SHORT ARRAY.. MEMORY!!");

			byte[] buffer = new byte[(int) capacity()];
			int read = 0;
			int avail = buffer.length;
			while (read != avail) {
				if (avail - read > 8192) {
					input.readFully(buffer, read, 8192);
					read += 8192;
				} else {
					input.readFully(buffer, read, avail - read);
					read = avail;
				}
			}
			offset = 0;
			this.buffer = buffer;
		}
	}

	public void load() throws IOException {
		if (root != this) {
			root.load();
			return;
		}
		if (mappedBytebuffer != null) {
			mappedBytebuffer.load();
			return;
		}
		if (buffer != null)
			return;
		if (file == null) {
			throw new IllegalStateException("No file associated with this ByteBuffer!");
		}

		DataInputStream is = new DataInputStream(getInputStream());
		load(is);
		is.close();

	}

	public void map() throws IOException {
		if (root != this) {
			root.map();
			return;
		}

		RandomAccessFile raf = new RandomAccessFile(file, "r");
		try {
			mappedBytebuffer = raf.getChannel().map(MapMode.READ_ONLY, fileoffset, len);
			bytebuffer = mappedBytebuffer;
		} finally {
			raf.close();
		}
		System.out.println("LOADED VIA FILE MAPPED BYTE BUFFER! "+file+" "+fileoffset+" "+len);
	}

	public void unload() {
		if (root != this) {
			root.unload();
			return;
		}
		if (file == null) {
			throw new IllegalStateException("No file associated with this ByteBuffer!");
		}
		root.buffer = null;
		shortbuffer = null;
		if (bytebuffer != null && bytebuffer.isDirect()) {
			destroyDirectByteBuffer(bytebuffer);
			bytebuffer = null;
		}
	}

	/**
	 * Invoke the private method "free" to destroy the direct byte buffer on Android
	 */
	public void destroyDirectByteBuffer(ByteBuffer directByteBuffer) {
		try {
			try {
				Method directByteBufferFreeMethod = directByteBuffer.getClass().getMethod("free");
				directByteBufferFreeMethod.setAccessible(true);
				directByteBufferFreeMethod.invoke(directByteBuffer);
			} catch (Exception e) {
				Field cleanerField = directByteBuffer.getClass().getDeclaredField("cleaner");
				cleanerField.setAccessible(true);
				Object cleaner = cleanerField.get(directByteBuffer);
				if (cleaner != null) {
					System.out.println("Cleaner class is " + cleaner.getClass().getName());
					System.out.println("Methods:");
					for (Method method : cleaner.getClass().getMethods()) {
						System.out.println("  " + method.getName());
					}

					Method cleanMethod = cleaner.getClass().getMethod("clean");
					cleanMethod.setAccessible(true);
					cleanMethod.invoke(cleaner);
				} else {
					System.out.println("COULDN'T FIND A WAY TO FREE A NATIVE BUFFER, OH WELL..");
//					Method directByteBufferFreeMethod = directByteBuffer.getClass().getMethod("finalize");
//					directByteBufferFreeMethod.setAccessible(true);
//					directByteBufferFreeMethod.invoke(directByteBuffer);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
