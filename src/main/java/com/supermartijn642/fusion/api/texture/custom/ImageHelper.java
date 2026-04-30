package com.supermartijn642.fusion.api.texture.custom;


import java.awt.image.BufferedImage;

/**
 * Contains utility methods for manipulating {@link BufferedImage}s.
 * <p>
 * Created 23/03/2026 by SuperMartijn642
 */
public final class ImageHelper {

    /**
     * Create an empty image with the given width and height.
     */
    public static BufferedImage createEmpty(int width, int height){
        if(width <= 0 || height <= 0)
            throw new IllegalArgumentException("Width and height must be positive!");
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Creates a copy of the given image.
     */
    public static BufferedImage createCopy(BufferedImage image){
        BufferedImage copy = createEmpty(image.getWidth(), image.getHeight());
        copyArea(image, copy, 0, 0, 0, 0, image.getWidth(), image.getHeight());
        return copy;
    }

    /**
     * Creates a copy of the given image with the size and pixel data of the specified area.
     */
    public static BufferedImage createCrop(BufferedImage image, int x, int y, int width, int height){
        if(width < 0 || height < 0)
            throw new IllegalArgumentException("Width and height must be positive!");
        if(x + width > image.getWidth() || y + height > image.getHeight())
            throw new IllegalArgumentException("Given area exceeds given image size!");
        BufferedImage cropped = createEmpty(width, height);
        copyArea(image, cropped, x, y, 0, 0, width, height);
        return cropped;
    }

    /**
     * Creates a copy of the given image with the pixel data of the specified area in each frame.
     */
    public static BufferedImage createCropFramed(BufferedImage image, int x, int y, int width, int height, int frameWidth, int frameHeight){
        if(width < 0 || height < 0)
            throw new IllegalArgumentException("Width and height must be positive!");
        if(x + width > image.getWidth() || y + height > image.getHeight())
            throw new IllegalArgumentException("Given area exceeds given image size!");
        if(frameWidth <= 0 || frameHeight <= 0)
            throw new IllegalArgumentException("Frame width and height must be positive!");
        if(image.getWidth() % frameWidth != 0 || image.getHeight() % frameHeight != 0)
            throw new IllegalArgumentException("Image size must be a multiple of frame width and height!");
        int frameColumns = image.getWidth() / frameWidth;
        int frameRows = image.getHeight() / frameHeight;
        BufferedImage cropped = createEmpty(width * frameColumns, height * frameRows);
        for(int row = 0; row < frameRows; row++){
            for(int column = 0; column < frameColumns; column++){
                copyArea(
                    image, cropped,
                    x + column * frameWidth,
                    y + row * frameHeight,
                    column * width,
                    row * height,
                    width, height);
            }
        }
        return cropped;
    }

    /**
     * Copies the pixel data of the specified area from {@code from} to {@code to}.
     * @param fromX  x-position of the area to copy from
     * @param fromY  y-position of the area to copy from
     * @param toX    x-position of the area to copy to
     * @param toY    x-position of the area to copy to
     * @param width  width of the area to copy
     * @param height height of the area to copy
     */
    public static void copyArea(BufferedImage from, BufferedImage to, int fromX, int fromY, int toX, int toY, int width, int height){
        if(width < 0 || height < 0)
            throw new IllegalArgumentException("Width and height must be positive!");
        if(fromX + width > from.getWidth() || fromY + height > from.getHeight() || toX + width > to.getWidth() || toY + height > to.getHeight())
            throw new IllegalArgumentException("Given area exceeds given image size!");
        for(int u = 0; u < width; u++){
            for(int v = 0; v < height; v++){
                to.setRGB(toX + u, toY + v, from.getRGB(fromX + u, fromY + v));
            }
        }
    }
}
