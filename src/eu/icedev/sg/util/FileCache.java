package eu.icedev.sg.util;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.*;
import java.util.*;

public class FileCache {
	Map<String, DataBuffer> streams = new HashMap<>();
	
	public final Path directory;
	
	public FileCache(File dir) {
		this.directory = dir.toPath();
	}

	public FileCache(Path dir) {
		this.directory = dir;
	}

	public DataBuffer openMaybe(String filename) {
		try {
			return open(filename);
		} catch (Exception e) {
			System.err.println(e);
		}
		return null;
	}

	public DataBuffer open(String filename) throws IOException {
		DataBuffer buffer = streams.get(filename);
		
		if(buffer == null) {
			FileChannel channel = (FileChannel) Files.newByteChannel(directory.resolve(filename), StandardOpenOption.READ);
			MappedByteBuffer mapped = channel.map(MapMode.READ_ONLY, 0, channel.size());
			buffer = new DataBuffer(mapped);
			streams.put(filename, buffer);
			channel.close();
		}
		
		buffer.reset();
		
		return buffer;
	}
	
	public void cleanup() {
		streams.clear();
	}



	public static String[] splitFilename(Path file) {
		return splitFilename(file.getFileName().toString());
	}

	public static String[] splitFilename(String name) {
		int last = name.lastIndexOf('.');
		if (last >= 0)
			return new String[]{name.substring(0, last), name.substring(last + 1)};
		return new String[]{name, ""};
	}
}
