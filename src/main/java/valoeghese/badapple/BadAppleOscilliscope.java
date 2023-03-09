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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class BadAppleOscilliscope {
	public static void main(String[] args) throws IOException {
		if (args.length != 3 && args.length != 4) {
			System.out.println("Usage: badappleosc <file> <output resolution x> <output resolution y> [debug frame]");
			return;
		}

		Path outputFolder = Path.of(args[0]).toAbsolutePath().getParent().resolve("out");
		try {
			Files.createDirectory(outputFolder);
		} catch (FileAlreadyExistsException ignored) {
		}

		int resolutionX = Integer.parseInt(args[1]);
		int resolutionY = Integer.parseInt(args[2]);

		try (ZipFile src = new ZipFile(args[0])) {
			if (args.length == 4) {
				processFrame(Integer.parseInt(args[3]), src, outputFolder, resolutionX, resolutionY);
			}
			else {
				int i = 1;
				long time = System.currentTimeMillis();

				while (processFrame(i, src, outputFolder, resolutionX, resolutionY)) {
					if (i % 100 == 0) {
						System.out.println("Completed " + i + " frames");
					}

					i++;
				}

				time = System.currentTimeMillis() - time;
				System.out.println("Processed " + i + " frames in " + time + " ms.");
			}
		}
	}

	private static boolean processFrame(int i, ZipFile src, Path outputFolder, int resolutionX, int resolutionY) throws IOException {
		String id = leftPad(i, 4);
		ZipEntry entry = src.getEntry("frames/output_" + id + ".jpg");

		if (entry == null) {
			return false;
		}

		BufferedImage frame;

		try (InputStream stream = new BufferedInputStream(src.getInputStream(entry))) {
			frame = ImageIO.read(stream);
		}

		BufferedImage outputFrame = new BufferedImage(resolutionX, resolutionY, BufferedImage.TYPE_INT_RGB);
		detectEdges(frame, outputFrame, i & 1);

		Path outputFile = outputFolder.resolve("output_" + id + ".png");

		try {
			Files.createFile(outputFile);
		} catch (FileAlreadyExistsException ignored) {
		}

		try (OutputStream stream = new BufferedOutputStream(Files.newOutputStream(outputFile))) {
			ImageIO.write(outputFrame, "png", stream);
		}

		return true;
	}

	public static void detectEdges(BufferedImage in, BufferedImage out, int skip) {
		// write black image
		Graphics2D graphics2D = out.createGraphics();
		graphics2D.setColor(Color.BLACK);
		graphics2D.fillRect(0, 0, out.getWidth(), out.getHeight());

		// for continuous line
		int prevYUpper = -1;
		int prevYLower = -1;

		// detect and find upper and lower edge

		for (int x = 0; x < out.getWidth(); x++) {
			// Lower edge
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
			if (passes >= 0) y = in.getHeight() - 1;

			// convert to output y
			int eqYOut = (int) (yConversionFactor * y);
			drawVerticalLine(out, x, prevYLower, eqYOut, Color.WHITE.getRGB());
			prevYLower = eqYOut;

			// Upper Edge
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
			if (passes >= 0) y = in.getHeight() - 1;

			// convert to output y
			eqYOut = (int) (yConversionFactor * y);
			drawVerticalLine(out, x, prevYUpper, eqYOut, Color.YELLOW.getRGB());
			prevYUpper = eqYOut;
		}
	}

	private static void drawVerticalLine(BufferedImage image, int x, int prevY, int nextY, int rgb) {
		if (prevY == -1) {
			image.setRGB(x, nextY, rgb);
		}
		else if (prevY > nextY) {
			for (int y = nextY; y <= prevY; y++) {
				image.setRGB(x, y, rgb);
			}
		}
		else {
			for (int y = prevY; y <= nextY; y++) {
				image.setRGB(x, y, rgb);
			}
		}
	}

	private static String leftPad(int number, int length) {
		StringBuilder result = new StringBuilder(String.valueOf(number));

		while (result.length() < length) {
			result.insert(0, '0');
		}

		return result.toString();
	}

	/**
	 * Reduce a colour in the full rgb spectrum to two 'colours', represented as a boolean.
	 * @param rgbColour the rgb colour.
	 * @return the boolean colour representation.
	 */
	private static boolean reduce(int rgbColour) {
		return rgbColour == -1;
	}
}
