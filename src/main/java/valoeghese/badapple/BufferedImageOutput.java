package valoeghese.badapple;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import static valoeghese.badapple.BadAppleOscilliscope.leftPad;

public final class BufferedImageOutput extends VideoOutput {
	public BufferedImageOutput(Path directory, int resolutionX, int resolutionY, int offset, int startFrameNumber) {
		super(resolutionX, resolutionY);
		this.directory = directory;
		this.frameNumber = startFrameNumber;
		this.offset = offset;
	}

	private final Path directory;
	private final int offset;
	private int frameNumber;

	@Override
	public void writeFrame(int[] channel1, int[] channel2) throws IOException {
		BufferedImage outputFrame = new BufferedImage(this.getWidth(), this.getHeight() + this.offset, BufferedImage.TYPE_INT_RGB);

		// write black image
		Graphics2D graphics2D = outputFrame.createGraphics();
		graphics2D.setColor(Color.BLACK);
		graphics2D.fillRect(0, 0, outputFrame.getWidth(), outputFrame.getHeight());

		// for continuous line
		int prevYCh1 = -1;
		int prevYCh2 = -1;

		for (int x = 0; x < this.getWidth(); x++) {
			drawVerticalLine(outputFrame, x, prevYCh1, channel1[x] + this.offset, Color.WHITE.getRGB());
			prevYCh1 = channel1[x];

			drawVerticalLine(outputFrame, x, prevYCh2, channel2[x] + this.offset, Color.YELLOW.getRGB());
			prevYCh2 = channel2[x];
		}

		// write
		Path outputFile = this.directory.resolve("output_" + leftPad(this.frameNumber, 4) + ".png");

		try {
			Files.createFile(outputFile);
		} catch (FileAlreadyExistsException ignored) {
		}

		try (OutputStream stream = new BufferedOutputStream(Files.newOutputStream(outputFile))) {
			ImageIO.write(outputFrame, "png", stream);
		}

		// next frame
		this.frameNumber++;
	}

	@Override
	public void close() throws IOException {
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
}
