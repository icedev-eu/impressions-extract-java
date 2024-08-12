package eu.icedev.sg;

import eu.icedev.sg.util.DataBuffer;

import java.util.ArrayList;
import java.util.List;

public class SGBitmap {
    public String filename; // 65
    public String comment; // 51
    public int width;
    public int height;
    public int num_images;
    public int start_index;
    public int end_index;

    public SGFile sg;
    public int index;
    public boolean isSystem;

    public List<SGImage> images = new ArrayList<>();

    public byte[] unknown;


    public SGBitmap(int index, SGFile sg, DataBuffer in) {
        this.index = index;
        this.sg = sg;

        filename = in.readString(65);
        comment = in.readString(51);

        width = in.readInt();
        height = in.readInt();
        num_images = in.readInt();
        start_index = in.readInt();
        end_index = in.readInt();

        unknown = in.readBytes(64);

        isSystem = filename.equalsIgnoreCase("system.bmp") || filename.equalsIgnoreCase("zeus_system.bmp") ;
    }
}
