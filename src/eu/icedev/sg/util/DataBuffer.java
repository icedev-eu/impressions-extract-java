package eu.icedev.sg.util;

import java.nio.*;

public class DataBuffer {
	private ByteBuffer data;
	
	public DataBuffer(ByteBuffer buffer) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		this.data = buffer;
	}
	
//	public DataBuffer(File f) {
//		try {
//			FileChannel channel = (FileChannel) Files.newByteChannel(f.toPath(), StandardOpenOption.READ);
//			MappedByteBuffer mapped = channel.map(MapMode.READ_ONLY, 0, channel.size());
//			mapped.order(ByteOrder.LITTLE_ENDIAN);
//			data = mapped;
//			
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//	}
	
	public int readInt() {
		return data.getInt();
	}
	
	public int readUShort() {
		return data.getShort() & 0xFFFF;
	}

	public short readShort() {
		return data.getShort();
	}
	
	public short readUByte() {
		return (short) (data.get() & 0xFF);
	}

	public int getInt(int position) {
		return data.getInt(position);
	}

	public int getUShort(int position) {
		return data.getShort(position) & 0xFFFF;
	}
	
	public short getByte(int position) {
		return (short) (data.get(position) & 0xFF);
	}
	
	public short get(int position) {
		return (short) (data.get(position) & 0xFF);
	}
	
	public String readString(int len) {
		byte[] bytes = new byte[len];
		data.get(bytes);
		
		int ln = 0;
		for(; ln<len; ln++) {
			if((bytes[ln] & 0xFF) == 0) {
				break;
			}
		}
		
		return new String(bytes, 0, ln).strip();
	}

	public void seek(int pos) {
		data.position(pos);
	}

	public void skip(int num) {
		data.position(num + data.position());
	}

	public short[] readUBytes(int n) {
		short[] ubytes = new short[n];
		for(int i=0;i<n;i++) {
			ubytes[i] = (short) readUByte();
		}
		return ubytes;
	}

	public byte[] readBytes(int n) {
		byte[] bytes = new byte[n];
		for(int i=0;i<n;i++) {
			bytes[i] = (byte) readUByte();
		}
		return bytes;
	}

	public int[] readUShorts(int n) {
		int[] ubytes = new int[n];
		for(int i=0;i<n;i++) {
			ubytes[i] = readUShort();
		}
		return ubytes;
	}

	public short[] readShorts(int n) {
		short[] ubytes = new short[n];
		for(int i=0;i<n;i++) {
			ubytes[i] = (short) readUShort();
		}
		return ubytes;
	}

	public int size() {
		return data.capacity();
	}

	public DataBuffer extract(int offset, int length) {
		if(length == -1) {
			length = data.capacity() - offset;
		}
		ByteBuffer slice = data.slice(offset, length);
		return new DataBuffer(slice);
	}

	public void reset() {
		data.clear();
	}

}
