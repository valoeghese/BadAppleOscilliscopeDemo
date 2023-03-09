package valoeghese.badapple;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class BadAppleOscilliscope {
	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			System.out.println("Usage: badappleosc <file> <output resolution x> <output resolution y>");
			return;
		}

		Path outputFolder = Path.of(args[0]).toAbsolutePath().getParent().resolve("out");
		Files.createDirectory(outputFolder);

		int resolutionX = Integer.parseInt(args[1]);
		int resolutionY = Integer.parseInt(args[2]);

		try (ZipFile src = new ZipFile(args[0])) {
			int i = 0;
			long time = System.currentTimeMillis();

			while (true) {
				ZipEntry entry = src.getEntry("frames/output_" + i);

				if (entry == null) {
					break;
				}

				BufferedImage frame;

				try (InputStream stream = new BufferedInputStream(src.getInputStream(entry))) {
					frame = ImageIO.read(stream);
				}

				BufferedImage outputFrame = new BufferedImage(resolutionX, resolutionY, BufferedImage.TYPE_INT_RGB);
				detectEdges(frame, outputFrame, i & 1);

				Path outputFile = outputFolder.resolve("output_" + i);
				Files.createFile(outputFile);

				try (OutputStream stream = new BufferedOutputStream(Files.newOutputStream(outputFile))) {
					ImageIO.write(outputFrame, "png", stream);
				}

				i++;
			}

			time = System.currentTimeMillis() - time;
			System.out.println("Processed " + i + " frames in " + time + " ms.");
		}
	}

	public static void detectEdges(BufferedImage in, BufferedImage out, int skip) {
		// write black image
		Graphics2D graphics2D = out.createGraphics();
		graphics2D.setColor(Color.BLACK);
		graphics2D.fillRect(0, 0, out.getWidth(), out.getHeight());

		// detect and find upper and lower edge
		for (int x = 0; x < out.getWidth(); x++) {
			// upper edge
			double xConversionFactor = (double) in.getWidth() / out.getWidth();
			double yConversionFactor = (double) out.getHeight() / in.getHeight();

			int eqXIn = (int) (xConversionFactor * x);
			int passes = skip;

			int y;
			boolean current = reduce(in.getRGB(eqXIn, in.getHeight() - 1));

			for (y = in.getHeight() - 1; y >= 0; y--) {
				if (reduce(in.getRGB(eqXIn, y)) != current) {
					current = !current;

					// when passes was 0 exit. That is, tolerate <passes> number of edges before exiting.
					if (passes-- == 0) {
						break;
					}
				}
			}

			// if didn't find enough edges, lock to bottom
			if (passes >= 0) y = 0;

			// convert to output y
			int eqYOut = (int) (yConversionFactor * y);
			out.setRGB(x, eqYOut, Color.WHITE.getRGB());

			// Lower Edge
			//=================
			passes = skip;
			current = reduce(in.getRGB(eqXIn, in.getHeight() - 1));

			for (y = 0; y < in.getHeight(); y++) {
				if (reduce(in.getRGB(eqXIn, y)) != current) {
					current = !current;

					// when passes was 0 exit. That is, tolerate <passes> number of edges before exiting.
					if (passes-- == 0) {
						break;
					}
				}
			}

			// if didn't find enough edges, lock to bottom
			if (passes >= 0) y = 0;

			// convert to output y
			eqYOut = (int) (yConversionFactor * y);
			out.setRGB(x, eqYOut, Color.YELLOW.getRGB());
		}
	}

	/**
	 * Reduce a colour in the full rgb spectrum to two 'colours', represented as a boolean.
	 * @param rgbColour the rgb colour.
	 * @return the boolean colour representation.
	 */
	private static boolean reduce(int rgbColour) {
		return rgbColour == 0;
	}
}
