package eu.icedev.sg;

import eu.icedev.sg.util.DataBuffer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class SGFile {
    static final int SG_HEADER_SIZE = 680;
    static final int SG_BITMAP_RECORD_SIZE = 200;

    public String filename;
    public SGImage[] images;
    public SGBitmap[] bitmaps;

    int sg_filesize;
    int version;
    int unknown1;
    int max_image_records;
    int num_image_records;
    int num_bitmap_records;
    int num_bitmap_records_without_system; /* ? */
    int total_filesize;
    int filesize_555;
    int filesize_external;

    public SGFile(String filename, DataBuffer in) {
        this.filename = filename;
        readHeader(in);
        checkVersion(in.size());

        in.seek(SG_HEADER_SIZE);
        readBitmaps(in);

        int maxBitmapRecords = version == 0xD3 ? 100 : 200;
        in.seek(SG_HEADER_SIZE + maxBitmapRecords * SG_BITMAP_RECORD_SIZE);

        readImages(in, version >= 0xd6);
        applyBitmapImages();
    }

    private void applyBitmapImages() {

        /**
        if(filename.equalsIgnoreCase("SprMain.sg3")) {
            customBitmapRange(
                    // peddler
                    108, // acupuncturist
                    216, // begger
                    324, // emigrant
                    432, // inspector
                    776,// bronzeware vendor
                    880,// vagrant
                    988,
                    1096, // immigrant
                    1312,
                    1416, // HempVendor
                    1520, // watchman
                    1812,
                    1920,
                    2216,
                    2320, // monk
                    2428, // mugger
                    2656,
                    2924, // silk vendor
                    3028, // iron miner
                    3324, // wall sentry
                    3784, // logger
                    4148,
                    4256, // gen warship
                    4424, // clerk ??
                    4528, // tea vendor
                    4632, // LacquerwareVendor
                    4736, //CeramicsVendor
                    4845, // bandit
                    5081, // thief
                    5249, // bireme
                    5417, // trireme
                    5585, // water bearer
                    5689, // laborer
                    6333, // mason
                    6777, // panda
                    7075, // musician
                    7204, // actor
                    7527, // Priest
                    7635, // cart
                    8075, // gen transport
                    8243, // donkey
                    8540, // acrobat
                    8689, // donkey cart
                    8986, // trade ship
                    9338, // announcer
                    9446, // horse
                    9742, // team leader
                    9846, // fishing boat
                    10338, // stone cutter
                    10542, // artisan
                    10790, // antelope
                    10990, // ferry
                    11174, // gobi bear
                    11471, // wolf
                    11767, // diseased walker
                    11875
            );
            return;
        }


        if(filename.equalsIgnoreCase("SprMain2.sg3")) {
            customBitmapRange(
                    // alligator
                    296, // tiger
                    633, // wild pig
                    833, // alligator swim
                    929,
                    1253, // cart incline
                    1693, // buyer helper
                    2233, // water buffalo solo
                    2530, // cart decline
                    2970, // elite couples
                    3604, // wheelbarrow pusher
                    4000, // farmer
                    5584, // tender?
                    6472, // tender arid
                    6576, // ceramist ???
                    7032, // water buffalo cart
                    7328, // water buffalo cart large
                    7624, // water buffalo pair
                    7921, // carpenter
                    8365, // salamander
                    8661, // chinese catapult
                    8969, // chinese cavalry
                    9605, // chinese crossbow man
                    10093, // chinese infantry man
                    10669, // trader humid
                    10773, // trader temp
                    10877, // chinese chariot
                    11338, // chinese chariot horse
                    11558, // hunter
                    12094, // vulture
                    12442, // vulture shadow
                    12790, // salamander swim


                    12887 // end
            );
        }

         */

        if(bitmaps.length == 1) {
            for (var img : images) {
                img.bitmap = bitmaps[0];
                img.bitmap.images.add(img);
            }
            return;
        }

        if(checkIfAllBitmapsEmpty()) {
            return;
        }

        for (var img : images) {
            img.bitmap = bitmaps[img.bitmap_id];
            img.bitmap.images.add(img);
        }
    }

    public static SGFile from(Path path) {
        try {
            FileChannel channel = (FileChannel) Files.newByteChannel(path, StandardOpenOption.READ);
            MappedByteBuffer mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            channel.close();
            return new SGFile(path.getFileName().toString(), new DataBuffer(mapped));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void readHeader(DataBuffer in) {
        sg_filesize = in.readInt();
        version = in.readInt();
        unknown1 = in.readInt();
        max_image_records = in.readInt();
        num_image_records = in.readInt();
        num_bitmap_records = in.readInt();
        num_bitmap_records_without_system = in.readInt();
        total_filesize = in.readInt();
        filesize_555 = in.readInt();
        filesize_external = in.readInt();
    }

    private void checkVersion(int filesize) {
        if (version == 0xD3) {
            if (sg_filesize == 74480 || sg_filesize == 522680) {
                return;
            }
        }

        if (version == 0xD5 || version == 0xD6) {
            if (sg_filesize == 74480 || sg_filesize == filesize) {
                return;
            }
        }

        throw new RuntimeException("Version check failed.");
    }


    private void readBitmaps(DataBuffer in) {
        bitmaps = new SGBitmap[num_bitmap_records];
        for (int i = 0; i < bitmaps.length; i++) {
            bitmaps[i] = new SGBitmap(i, this, in);
        }
    }

    private void readImages(DataBuffer in, boolean includeAlpha) {
        images = new SGImage[num_image_records];

        var dummy = new SGImage(0, in, includeAlpha);

        for(int i=0; i<images.length; i++) {
            images[i] = new SGImage(i, in, includeAlpha);
            images[i].sg = this;

            if (images[i].invert_offset < 0) {
                images[i].invertOf = images[i + images[i].invert_offset];
            }
        }

    }

    private boolean checkIfAllBitmapsEmpty() {
        for(var bitmap : bitmaps) {
            if(bitmap.num_images != 0)
                return false;
        }
        return true;
    }

    public void customBitmapRange(int...range) {
        int imageId = 0;
        int bitmapId = 0;

        for(int i=0; i<range.length; i++) {
            bitmaps[bitmapId].images.clear();
            while(imageId < range[i]) {
                images[imageId].bitmap = bitmaps[bitmapId];
                bitmaps[bitmapId].images.add(images[imageId]);
                imageId++;
            }
            bitmapId++;
        }
    }
}