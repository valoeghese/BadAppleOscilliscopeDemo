package valoeghese.badapple;

import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static valoeghese.badapple.BadAppleOscilliscope.leftPad;

public final class BufferedImageOutput extends VideoOutput {
	public BufferedImageOutput(Path directory, int resolutionX, int resolutionY, int offset, int startFrameNumber, boolean noVert) {
		super(resolutionX, resolutionY);
		this.directory = directory;
		this.frameNumber = startFrameNumber;
		this.offset = offset;
		this.noVert = noVert;
	}

	private final Path directory;
	private final int offset;
	private final boolean noVert;
	private int frameNumber;

	private enum Channels {
		CH_1(Color.YELLOW),
		CH_2(Color.GREEN),
		CH_3(Color.CYAN),
		CH_4(Color.MAGENTA),
		CH_5(Color.WHITE),
		CH_6(Color.ORANGE),
		CH_7(Color.RED),
		CH_8(Color.LIGHT_GRAY)
		;
		Channels(Color colour) {
			this.colour = colour;
		}
		private final Color colour;
	}

	@Override
	public void writeFrame(int[] ...channels) throws IOException {
		BufferedImage outputFrame = new BufferedImage(this.getWidth(), this.getHeight() + this.offset, BufferedImage.TYPE_INT_RGB);

		// write black image
		Graphics2D graphics2D = outputFrame.createGraphics();
		graphics2D.setColor(Color.BLACK);
		graphics2D.fillRect(0, 0, outputFrame.getWidth(), outputFrame.getHeight());

		// for continuous line
		int[] prevYChN = new int[channels.length];
		Arrays.fill(prevYChN, -1);

		for (int x = 0; x < this.getWidth(); x++) {
			for (int ch = 0; ch < channels.length; ch++) {
				int[] channel = channels[ch];

				int nextY = channel[x] + this.offset;
				drawVerticalLine(outputFrame, x, this.noVert ? nextY : prevYChN[ch], nextY, Channels.values()[ch & 7].colour.getRGB());
				prevYChN[ch] = channel[x]; // note to self: why does this not have +offset too? Issue? Pretty sure I would have commented why
			}
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
