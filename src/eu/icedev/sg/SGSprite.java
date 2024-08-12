package eu.icedev.sg;

public class SGSprite {
    public int width, height;
    public int originX, originY;
    public int offsetX, offsetY;
    public int[] pixels;

    public SGSprite(int width, int height) {
        this.width = width;
        this.height = height;
        pixels = new int[width * height];
    }

    public void setRGB(int x, int y, int color) {
        pixels[x + y*width] = color;
    }

    public int getRGB(int x, int y) {
        return pixels[x + y*width];
    }

    public void set555Pixel(int x, int y, int color) {
        if (color == 0xf81f) {
            return;
        }

        if(color == 0x7c00) {
            setRGB(x, y, 0x66000000);
            return;
        }

        int rgb = 0xff000000;
        // Red: bits 11-15, should go to bits 17-24
        rgb |= ((color & 0x7c00) << 9) | ((color & 0x7000) << 4);
        // Green: bits 6-10, should go to bits 9-16
        rgb |= ((color & 0x3e0) << 6) | ((color & 0x300));
        // Blue: bits 1-5, should go to bits 1-8
        rgb |= ((color & 0x1f) << 3) | ((color & 0x1c) >> 2);


        setRGB(x, y, rgb);
    }

    public void setAlphaPixel(int x, int y, int color) {
        /* Only the first five bits of the alpha channel are used */
        int alpha = ((color & 0x1f) << 3) | ((color & 0x1c) >> 2);

        int rgb = getRGB(x, y);
        rgb = (rgb & 0x00FFFFFF) | (alpha << 24);
        setRGB(x, y, rgb);
    }
}
