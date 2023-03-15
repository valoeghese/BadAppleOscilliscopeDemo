package valoeghese.badapple;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class BadAppleOscilliscope {
	public static void main(String[] args) throws IOException {
		List<String> nonFlagArgs = new ArrayList<>();
		boolean flagRaw = false;
		boolean flagRawBinary = false;
		boolean flagSpike = false;

		for (String s : args) {
			if (s.startsWith("--")) {
				switch (s) {
				case "--raw":
					flagRaw = true;
					break;
				case "--rawb":
					flagRawBinary = true;
					break;
				case "--spike":
					flagSpike = true;
					break;
				default:
					System.out.println("Unknown flag " + s);
					return;
				}
			}
			else {
				nonFlagArgs.add(s);
			}
		}

		if (flagRawBinary && flagRaw) {
			System.out.println("Cannot specify both raw binary and raw (text).");
			return;
		}

		run(nonFlagArgs.toArray(String[]::new), flagRawBinary ? 2 : (flagRaw ? 1 : 0), flagSpike);
	}

	public static void run(String[] args, int mode, boolean spike) throws IOException {
		if (args.length != 3 && args.length != 4) {
			System.out.println("Usage: badappleosc <file> <output resolution x> <output resolution y> [debug frame] [--raw] [--spike]");
			return;
		}

		Path outputFolder = Path.of(args[0]).toAbsolutePath().getParent().resolve("out");
		try {
			Files.createDirectory(outputFolder);
		} catch (FileAlreadyExistsException ignored) {
		}

		int resolutionX = Integer.parseInt(args[1]);
		int resolutionY = Integer.parseInt(args[2]);

		VideoOutput videoOutput = switch (mode) {
			case 2 -> new Bits8Output(outputFolder.resolve("video.dat"), resolutionX, resolutionY);
			case 1 -> new TextFileOutput(outputFolder.resolve("video.txt"), resolutionX, resolutionY);
			default -> new BufferedImageOutput(outputFolder, resolutionX, resolutionY, spike ? 1 : 0, 1);
		};

		try (ZipFile src = new ZipFile(args[0])) {
			if (args.length == 4) {
				processFrame(Integer.parseInt(args[3]), src, videoOutput, spike);
			}
			else {
				int i = 1;
				long time = System.currentTimeMillis();

				while (processFrame(i, src, videoOutput, spike)) {
					if (i % 100 == 0) {
						System.out.println("Completed " + i + " frames");
					}

					i++;
				}

				time = System.currentTimeMillis() - time;
				System.out.println("Processed " + i + " frames in " + time + " ms.");
			}
		}

		videoOutput.close();
	}

	private static boolean processFrame(int i, ZipFile src, VideoOutput output, boolean spike) throws IOException {
		String id = leftPad(i, 4);
		ZipEntry entry = src.getEntry("frames/output_" + id + ".jpg");

		if (entry == null) {
			return false;
		}

		BufferedImage frame;

		try (InputStream stream = new BufferedInputStream(src.getInputStream(entry))) {
			frame = ImageIO.read(stream);
		}

		detectEdges(frame, output, i & 1, spike);
		return true;
	}

	public static void detectEdges(BufferedImage in, VideoOutput out, int skip, boolean spike) throws IOException {
		final int horizontalResolution = out.getWidth();

		// create output channels
		int[] channel1 = new int[horizontalResolution]; // channel 1, for lower edge
		int[] channel2 = new int[horizontalResolution]; // channel 2, for upper edge

		// detect and find upper and lower edge
		for (int x = 0; x < horizontalResolution; x++) {
			// Lower edge
			double xConversionFactor = (double) in.getWidth() / horizontalResolution;
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

			// convert to output y and write to channel 1
			int eqYOut = (int) (yConversionFactor * y);
			channel1[x] = eqYOut;

			// Upper Edge
			//=================
			passes = skip;
			current = reduce(in.getRGB(eqXIn, 0));

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

			// convert to output y and write to channel 2
			eqYOut = (int) (yConversionFactor * y);
			channel2[x] = eqYOut;
		}

		// if spike, override beginning of frame with spike out put range. This is useful for aligning on the oscilliscope (trigger).
		channel1[0] = -1;
		channel2[0] = -1;

		// write the frame
		out.writeFrame(channel1, channel2);
	}

	static String leftPad(int number, int length) {
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
