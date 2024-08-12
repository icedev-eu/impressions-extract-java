package eu.icedev.sg;

import eu.icedev.sg.util.DataBuffer;

import java.io.IOException;

public class SGImage {
    static final int ISOMETRIC_TILE_WIDTH = 58;
    static final int ISOMETRIC_TILE_HEIGHT = 30;
    static final int ISOMETRIC_TILE_BYTES = 1800;
    static final int ISOMETRIC_LARGE_TILE_WIDTH = 78;
    static final int ISOMETRIC_LARGE_TILE_HEIGHT = 40;
    static final int ISOMETRIC_LARGE_TILE_BYTES = 3200;



    public int offset;
    public int length;
    public int uncompressed_length;
    /* 4 zero bytes: */
    public int invert_offset;
    public int width;
    public int height;
    /* 26 unknown bytes, mostly zero, first four are 2 shorts */
    public int type;
    /** 4 flag/option-like bytes: */
    public byte[] flags;
    public int bitmap_id;
    /* 3 bytes + 4 zero bytes */
    /* For D6 and up SG3 versions: alpha masks */
    public int alpha_offset;
    public int alpha_length;

    public int origX;
    public int origY;
    public int offX;
    public int offY;

    public SGFile sg;
    public SGImage invertOf;
    public SGBitmap bitmap;
    public SGSprite sprite;
    public int index;

    public boolean dontLoad;

    public int unknown_q;
    public int unknown_w;
    public short[] unknown1;
    public byte[] unknown2;

    public SGImage(int index, DataBuffer in, boolean includeAlpha) {
        this.index = index;

        offset = in.readInt();
        length = in.readInt();
        uncompressed_length = in.readInt();

        //in.skip(4);
        unknown_q = in.readUShort();
        unknown_w = in.readUShort();

        invert_offset = in.readInt();
        width = in.readShort();
        height = in.readShort();

        origX = in.readShort(); // ar[0];
        origY = in.readShort(); // [1];

        unknown1 = in.readShorts(11);

        offX = unknown1[3] & 0xFFFF;
        offY = unknown1[4] & 0xFFFF;

//		in.skip(26-8*2);
        type = in.readUShort();
        flags = in.readBytes(4);
        bitmap_id = in.readUByte();
//		in.skip(7);

        unknown2 = in.readBytes(7);

        if (includeAlpha) {
            alpha_offset = in.readInt();
            alpha_length = in.readInt();
        }
    }

    public boolean isExtern() {
        return flags[0] != 0;
    }

    public void loadImageData(DataBuffer file555) throws IOException {
        if(width <=0 || height <= 0 || length <= 0) {
            throw new IOException("Invalid image data: "+width+"x"+height + " len:" + length);
        }

        DataBuffer bufferZ = extractBuffer(file555);

        var data = new SGSprite(width, height);
        //data.pixels = new BufferedImage(workRecord.width, workRecord.height, BufferedImage.TYPE_INT_ARGB_PRE);
        //data.width = workRecord.width;
        //data.height = workRecord.height;
        data.originX = origX;
        data.originY = origY;
        data.offsetX = offX;
        data.offsetY = offY;

        DataBuffer buffer1 = bufferZ.extract(0, this.length);

        switch(this.type) {
            case 0:
            case 1:
            case 10:
            case 12:
            case 13:
                loadPlainImage(data, buffer1);
                break;

            case 30:
                loadIsometricImage(data, buffer1);
                break;

            case 256:
            case 257:
            case 276:
                loadSpriteImage(data, buffer1);
                break;

            default:
                throw new IOException("Unknown image type: "+ this.type);
        }

//        if(invert) {
//            mirrorResult(data.image);
//        }

        if(this.alpha_length != 0) {
            //DataBuffer buffer2 = bufferZ.extract(workRecord.alpha_offset, workRecord.alpha_length);
            var alpha_buffer = bufferZ.extract(this.length, -1);
            loadAlphaMask(data, alpha_buffer);
        }
        this.sprite = data;
    }

    private DataBuffer extractBuffer(DataBuffer file) throws IOException {
        int data_length  = this.length + this.alpha_length;

        if(data_length <= 0) {
            throw new IOException("Data length invalid " + data_length);
        }

        int offset = this.offset - (this.flags[0] & 0xFF);

        return file.extract(offset, data_length);
    }


    private void loadPlainImage(SGSprite data, DataBuffer buffer) throws IOException {
        if (this.height * this.width * 2 != this.length) {
            throw new IOException("Image data length doesn't match image size " + this.width+"x"+this.height + " != " + this.length);
        }

        int i = 0;
        int x, y;
        for (y = 0; y < this.height; y++) {
            for (x = 0; x < this.width; x++, i+= 2) {
                data.set555Pixel(x, y, buffer.getByte(i) | (buffer.getByte(i+1) << 8));
            }
        }
    }

    private void loadIsometricImage(SGSprite pixels, DataBuffer buffer) {
        writeIsometricBase(pixels, buffer);
        writeTransparentImage(pixels, buffer.extract(this.uncompressed_length, this.length - this.uncompressed_length));
    }

    private void loadSpriteImage(SGSprite pixels, DataBuffer buffer) {
        writeTransparentImage(pixels, buffer);
    }

    private void loadAlphaMask(SGSprite pixels, DataBuffer buffer) {
        int i = 0;
        int x = 0, y = 0, j;
        int width = this.width;
        int length = this.alpha_length;

        while (i < length) {
            int c = buffer.get(i++);
            if (c == 255) {
                /* The next byte is the number of pixels to skip */
                x += buffer.get(i++);
                while (x >= width) {
                    y++;
                    x -= width;
                }
            } else {
                /* `c' is the number of image data bytes */
                for (j = 0; j < c; j++, i++) {
                    pixels.setAlphaPixel(x, y, buffer.get(i));
                    x++;
                    if (x >= width) {
                        y++;
                        x = 0;
                    }
                }
            }
        }
    }


    private void writeIsometricBase(SGSprite pixels, DataBuffer buffer) {
        int i = 0, x, y;
        int width, height, height_offset;
        int size = this.flags[3] & 0xFF;
        int x_offset, y_offset;
        int tile_bytes, tile_height, tile_width;

        width = this.width;
        height = (width + 2) / 2; /* 58 -> 30, 118 -> 60, etc */
        height_offset = this.height - height;
        y_offset = height_offset;

        if (size == 0) {
            /* Derive the tile size from the height (more regular than width) */
            /* Note that this causes a problem with 4x4 regular vs 3x3 large: */
            /* 4 * 30 = 120; 3 * 40 = 120 -- give precedence to regular */
            if (height % ISOMETRIC_TILE_HEIGHT == 0) {
                size = height / ISOMETRIC_TILE_HEIGHT;
            } else if (height % ISOMETRIC_LARGE_TILE_HEIGHT == 0) {
                size = height / ISOMETRIC_LARGE_TILE_HEIGHT;
            }
        }

        /* Determine whether we should use the regular or large (emperor) tiles */
        if (ISOMETRIC_TILE_HEIGHT * size == height) {
            /* Regular tile */
            tile_bytes  = ISOMETRIC_TILE_BYTES;
            tile_height = ISOMETRIC_TILE_HEIGHT;
            tile_width  = ISOMETRIC_TILE_WIDTH;
        } else if (ISOMETRIC_LARGE_TILE_HEIGHT * size == height) {
            /* Large (emperor) tile */
            tile_bytes  = ISOMETRIC_LARGE_TILE_BYTES;
            tile_height = ISOMETRIC_LARGE_TILE_HEIGHT;
            tile_width  = ISOMETRIC_LARGE_TILE_WIDTH;
        } else {
            throw new RuntimeException("Unknown tile size " + this.width +"x"+this.height);
        }

        /* Check if buffer length is enough: (width + 2) * height / 2 * 2bpp */
        if ((width + 2) * height != this.uncompressed_length) {
            throw new RuntimeException("Data length doesn't match footprint size");
        }

        i = 0;
        for (y = 0; y < (size + (size - 1)); y++) {
            x_offset = (y < size ? (size - y - 1) : (y - size + 1)) * tile_height;
            for (x = 0; x < (y < size ? y + 1 : 2 * size - y - 1); x++, i++) {

                writeIsometricTile(pixels, buffer.extract(i * tile_bytes, -1),
                        x_offset, y_offset, tile_width, tile_height);
                x_offset += tile_width + 2;
            }
            y_offset += tile_height / 2;
        }
    }

    private void writeIsometricTile(SGSprite pixels, DataBuffer buffer,
                                    int offset_x, int offset_y, int tile_width, int tile_height) {
        int half_height = tile_height / 2;
        int x, y, i = 0;

        for (y = 0; y < half_height; y++) {
            int start = tile_height - 2 * (y + 1);
            int end = tile_width - start;
            for (x = start; x < end; x++, i += 2) {
                pixels.set555Pixel(offset_x + x, offset_y + y,
                        (buffer.get(i+1) << 8) | buffer.get(i));
            }
        }
        for (y = half_height; y < tile_height; y++) {
            int start = 2 * y - tile_height;
            int end = tile_width - start;
            for (x = start; x < end; x++, i += 2) {
                pixels.set555Pixel(offset_x + x, offset_y + y,
                        (buffer.get(i+1) << 8) | buffer.get(i));
            }
        }
    }

    private void writeTransparentImage(SGSprite pixels, DataBuffer buffer) {
        int i = 0;
        int x = 0, y = 0, j;
        int width = this.width;

        while (i < buffer.size()) {
            int c = buffer.get(i++);
            if (c == 255) {
                /* The next byte is the number of pixels to skip */
                x += buffer.get(i++);
                while (x >= width) {
                    y++;
                    x -= width;
                }
            } else {
                /* `c' is the number of image data bytes */
                for (j = 0; j < c; j++, i += 2) {
                    pixels.set555Pixel(x, y, buffer.get(i) | (buffer.get(i+1) << 8));
                    x++;
                    if (x >= width) {
                        y++;
                        x = 0;
                    }
                }
            }
        }
    }

    public String find555Filename() {
        boolean isExtern = isExtern();

        String basename;

        if(isExtern) {
            basename = bitmap.filename;
        } else {
            basename = sg.filename;
        }

        int dot = basename.lastIndexOf('.');

        if(dot >= 0) {
            basename = basename.substring(0, dot);
        }

        return basename + ".555";
    }

    public boolean isInverted() {
        return invertOf != null;
    }
}
