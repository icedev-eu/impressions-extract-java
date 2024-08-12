import eu.icedev.sg.SGFile;
import eu.icedev.sg.SGImage;
import eu.icedev.sg.util.FileCache;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestExtractImages {

	public static void main(String[] args) throws Exception {

		Path datapath = Path.of("path to game/data directory");
		Path bitpath = Path.of("Bitmaps");
		extractImages(datapath, bitpath);
	}

	public static void extractImages(Path datapath, Path bitpath) throws Exception {
		FileCache cache = new FileCache(datapath);

		Files.createDirectories(bitpath);
		var sgs = Files.list(datapath).filter(p -> p.toString().toLowerCase().endsWith(".sg3")).map(SGFile::from).toList();

		for(var sg : sgs) {
			if(Arrays.stream(sg.bitmaps).mapToInt((b)->b.images.size()).sum() != 0) {
				//continue;
			}
			System.out.println(sg.filename);
			for(var bitmap : sg.bitmaps) {
				System.out.println("    " + bitmap.filename);
			}
			//System.out.println(Arrays.stream(sg.bitmaps).map(b->b.filename).toList());

			for (var img : sg.images) {
				if (img.bitmap != null && img.bitmap.isSystem)
					continue;
				if (img.invertOf != null)
					continue;
				if (img.width <= 0 || img.height <= 0) {
					continue;
				}
				if (img.length <= 0) {
					continue;
				}
				var dataFile = cache.openMaybe(img.find555Filename());
				if (dataFile != null) {
					img.loadImageData(dataFile);
				}
			}

			List<SGImage> images = new ArrayList<>();

			for (var img : sg.images) {

				boolean flagged = img.unknown2[1] != 0;

				if(img.invertOf instanceof SGImage inv) {
					if(inv.sprite != null) {
						images.add(img);
					}
				} else if(img.sprite != null) {
					images.add(img);
				}
			}

			var image = makeImage(images);
			String basename = FileCache.splitFilename(sg.filename)[0];
			if(image == null) {
				System.out.println("no images to save...");
				continue;
			}

			System.out.println("Saving " + basename);

			ImageIO.write(image, "png", new File(bitpath.toString(), basename + ".PNG"));
		}
	}

	public static BufferedImage makeImage(List<SGImage> images) {
		int width = 0;
		int height = 0;

		{
			int currentLineWidth = 0;
			int currentLineHeight = 0;
			for (var image : images) {
				var sprite = image.invertOf != null ? image.invertOf.sprite : image.sprite;
				var newLine = image.unknown2[1] != 0;

				if (newLine || currentLineWidth > (1024*8)) {
					height += currentLineHeight;
					width = Math.max(width, currentLineWidth);
					currentLineHeight = currentLineWidth = 0;
				}
				currentLineHeight = Math.max(sprite.height, currentLineHeight);
				currentLineWidth += sprite.width;
			}

			height += currentLineHeight;
			width = Math.max(width, currentLineWidth);
		}

		if(width <= 0 || height <= 0)
			return null;

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int[] imgpix = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
		var g = image.createGraphics();

		var font = new Font("Consola", Font.PLAIN, 14);
		g.setFont(font);

		int x = 0;
		int y = 0;
		int lineHeight = 0;
		for (var img : images) {
			var sprite = img.invertOf != null ? img.invertOf.sprite : img.sprite;
			var newLine = img.unknown2[1] != 0;

			if(newLine || x > (1024*8)) {
				y += lineHeight;
				x = 0;
				lineHeight = 0;
			}


			if (img.invertOf != null) {
				drawRGBInverted(sprite.pixels, sprite.width, sprite.height, imgpix, width, x, y);
			} else {
				drawRGB(sprite.pixels, sprite.width, sprite.height, imgpix, width, x, y);
			}

			int cx = x + sprite.offsetX;
			int cy = y + sprite.offsetY;

			{
				g.setColor(Color.RED);
				g.drawLine(cx, cy - 5, cx, cy + 5);
				g.drawLine(cx - 5, cy, cx + 5, cy);
			}
			{
				g.setColor(Color.BLACK);
				g.drawString("" + img.index, x + 1, y + 14);
				g.drawString("" + img.index, x - 1, y + 12);
				g.setColor(Color.WHITE);
				g.drawString("" + img.index, x, y + 13);
			}

			lineHeight = Math.max(sprite.height, lineHeight);
			x += Math.max(0, sprite.width);
		}

		return image;
	}

	private static void drawRGB(int[] source, int width, int height, int[] target, int targetWidth, int posx, int posy) {
		for (int y = 0; y < height; y++) {
			int ti = (posy + y) * targetWidth + posx;
			int si = y * width;
			for (int x = 0; x < width; x++) {
				target[ti++] = source[si++];
			}
		}
	}


	private static void drawRGBInverted(int[] source, int width, int height, int[] target, int targetWidth, int posx, int posy) {
		for (int y = 0; y < height; y++) {
			int ti = (posy + y) * targetWidth + posx;
			int si = y * width + width;
			for (int x = 0; x < width; x++) {

//                int ARGB = source[--si];
//                ARGB = (0xFFFFFF - (ARGB&0xFFFFFF)) | 0xFF000000;
				int argb = source[--si];

				if ((argb & 0xFF000000) != 0) {
					argb = argb ^ 0xFFFFFF;
				}

				target[ti++] = argb;
			}
		}
	}
}
