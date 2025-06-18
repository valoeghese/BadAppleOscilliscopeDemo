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
		boolean noVert = false;
		Mode mode = null;
		Threshold threshold = null;

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
				case "--novert":
					noVert = true;
					break;
				case "--mode=2i":
					if (mode != null) {
						System.err.println("You cannot specify the mode twice.");
						System.exit(1);
					}
					mode = Mode.CH_2_INTERLACING;
					break;
				case "--mode=2pi8":
					if (mode != null) {
						System.err.println("You cannot specify the mode twice.");
						System.exit(1);
					}
					mode = Mode.CH_2_PIXEL_INTERLACE_8;
					break;
				case "--mode=3x":
					if (mode != null) {
						System.err.println("You cannot specify the mode twice.");
						System.exit(1);
					}
					mode = Mode.CH_3_NO_INTERLACE;
					break;
				case "--mode=4x":
					if (mode != null) {
						System.err.println("You cannot specify the mode twice.");
						System.exit(1);
					}
					mode = Mode.CH_4_NO_INTERLACE;
					break;
				case "--threshold=hysteretic":
					if (threshold != null) {
						System.err.println("You cannot specify the threshold twice.");
						System.exit(1);
					}
					threshold = Threshold.THRESHOLD_HYSTERETIC;
					break;
				case "--threshold=white":
					if (threshold != null) {
						System.err.println("You cannot specify the threshold twice.");
						System.exit(1);
					}
					threshold = Threshold.THRESHOLD_WHITE;
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

		if (mode == null) {
			mode = Mode.CH_2_INTERLACING; // default
		}
		if (threshold == null) {
			threshold = Threshold.THRESHOLD_HYSTERETIC; // default
		}

		if (flagRawBinary && flagRaw) {
			System.out.println("Cannot specify both raw binary and raw (text).");
			return;
		}

		run(nonFlagArgs.toArray(String[]::new), flagRawBinary ? 2 : (flagRaw ? 1 : 0), flagSpike, noVert, mode, threshold);
	}

	public enum Mode {
		CH_2_INTERLACING("2-channel interlacing"),
		CH_2_PIXEL_INTERLACE_8("2-channel pixel-interlace (8 edges)"),
		CH_3_NO_INTERLACE("3-channel no interlacing"),
		CH_4_NO_INTERLACE("4-channel no interlacing");

		Mode(String name) {
			this.name = name;
		}
		private final String name;

		@Override
		public String toString() {
			return this.name;
		}
	}
	public enum Threshold {
		// new thresholding
		THRESHOLD_HYSTERETIC {
			@Override
			boolean threshold(int rgba, boolean current, boolean first) {
				// greyscale image: just take the blue
				int grey = (rgba >> 8) & 0xFF;
				if (first) return grey > 127; // first colour
				if (current) return grey > 10;// last white: need low brightness to switch to black
				return grey > 250;//last black: need high brightness to switch
			}
		},
		// old thresholding
		THRESHOLD_WHITE {
			@Override
			boolean threshold(int rgba, boolean current, boolean first) {
				return rgba == -1;
			}
		};

		/**
		 * Return true for white, false for black
		 */
		abstract boolean threshold(int rgba, boolean current, boolean first);
	}

	public static void run(String[] args, int exportType, boolean spike, boolean noVert, Mode channelMode,
						   Threshold threshold) throws IOException {
		if (args.length != 3 && args.length != 4) {
			System.out.println("Usage: badappleosc <file> <output resolution x> <output resolution y> [debug frame] [--raw/--rawb] [--mode=...] [--threshold=...] [--spike] [--novert]");
			System.out.println("   Modes: (--mode=2i [DEFAULT] 2 channel, interlacing) (--mode=2pi8 2 channel, pixel interlace, 8 edges) (--mode=3x 3 channel, no interlacing) (--mode=4x 4 channel, no interlacing)");
			System.out.println("   Thresholds: (--threshold=hysteretic [DEFAULT] large hysteresis) (--threshold=white split image into white and not white)");
			return;
		}

		System.out.println("%% BadAppleOscilloscope %%");
		System.out.println(" > Export Type: " + (exportType == 0 ? "Video" : exportType == 1 ? "Raw (text)" : "Raw (binary)"));
		System.out.println(" > Channels: " + channelMode);
		System.out.println(" > Threshold: " + threshold.name());
		if (spike) System.out.println(" > Spike Enabled");

		Path outputFolder = Path.of(args[0]).toAbsolutePath().getParent().resolve("out");
		try {
			Files.createDirectory(outputFolder);
		} catch (FileAlreadyExistsException ignored) {
		}

		int resolutionX = Integer.parseInt(args[1]);
		int resolutionY = Integer.parseInt(args[2]);

		VideoOutput videoOutput = switch (exportType) {
			case 2 -> new Bits8Output(outputFolder.resolve("video.dat"), resolutionX, resolutionY);
			case 1 -> new TextFileOutput(outputFolder.resolve("video.txt"), resolutionX, resolutionY);
			default -> new BufferedImageOutput(outputFolder, resolutionX, resolutionY, spike ? 1 : 0, 1, noVert);
		};

		try (ZipFile src = new ZipFile(args[0])) {
			if (args.length == 4) {
				processFrame(Integer.parseInt(args[3]), src, videoOutput, spike, channelMode, threshold);
			}
			else {
				int i = 1;
				long time = System.currentTimeMillis();

				while (processFrame(i, src, videoOutput, spike, channelMode, threshold)) {
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

	private static boolean processFrame(int i, ZipFile src, VideoOutput output, boolean spike, Mode channelMode,
										Threshold threshold) throws IOException {
		String id = leftPad(i, 4);
		ZipEntry entry = src.getEntry("frames/output_" + id + ".jpg");

		if (entry == null) {
			return false;
		}

		BufferedImage frame;

		try (InputStream stream = new BufferedInputStream(src.getInputStream(entry))) {
			frame = ImageIO.read(stream);
		}

		EdgeResult edges, secondEdges;

		switch (channelMode) {
		case CH_2_INTERLACING:
			// write the frame
			edges = detectEdges(frame, output, threshold, i & 1, spike);
			output.writeFrame(edges.bottom, edges.top);
			break;
		case CH_2_PIXEL_INTERLACE_8:
			edges = detectEdges(frame, output, threshold, 0, spike);
			secondEdges = detectEdges(frame, output, threshold, 1, spike);
		{
			EdgeResult thirdEdges = detectEdges(frame, output, threshold, 2, spike);
			EdgeResult fourthEdges = detectEdges(frame, output, threshold, 3, spike);

			pixelInterlace(edges.top, edges.bottom, secondEdges.top, secondEdges.bottom, thirdEdges.top, thirdEdges.bottom, fourthEdges.top, fourthEdges.bottom);
			output.writeFrame(edges.bottom, edges.top);
		}
			break;
		case CH_3_NO_INTERLACE:
			// write the frame
			edges = detectEdges(frame, output, threshold, 0, spike);
			secondEdges = detectEdges(frame, output, threshold, 1, spike);

			// if ch3 value is larger (lower) than bottom clamp ch3 to bottom!
			output.writeFrame(edges.bottom, edges.top, ArrayMaths.clampMax(secondEdges.top, edges.bottom));
			break;
		case CH_4_NO_INTERLACE:
			// write the frame
			edges = detectEdges(frame, output, threshold, 0, spike);
			secondEdges = detectEdges(frame, output, threshold, 1, spike);

			// if ch4 value is smaller (higher) than top clamp ch4 to top!
			output.writeFrame(edges.bottom, edges.top, ArrayMaths.clampMax(secondEdges.top, edges.bottom), ArrayMaths.clampMin(secondEdges.bottom, edges.top));
			break;
		}

		return true;
	}

	private static void pixelInterlace(int[] top, int[] bottom, int[] ...alternating) {
		int depth = (alternating.length+1) / 2;
		for (int x = 0; x < top.length; x++) {
			int colDepth = x % (depth+1);
			int b = bottom[x];
			int t = top[x];

			for (int dh = 0; dh < alternating.length; dh++) {
				int inDepth = (dh+2)/2;
				if (inDepth > colDepth) break; // Don't go deeper than we have to
				int v = alternating[dh][x];
				if ((dh & 1)==1) {// bottom edge
					if (v > top[x]) {
						bottom[x] = v;
					} else if ((inDepth & 1) == 0) {
						bottom[x] = b;//prioritise the outer edges
					}
				} else {
					if (v < bottom[x]) {
						top[x] = v;
					} else {
						top[x] = t;//prioritise the outer edges
					}
				}
			}
		}
	}

	public record EdgeResult(int[] bottom, int[] top) {
	}

	public static EdgeResult detectEdges(BufferedImage in, VideoOutput out, Threshold threshold, int skip, boolean spike) throws IOException {
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
			boolean current = threshold.threshold(in.getRGB(eqXIn, in.getHeight() - 1), false, true);

			for (y = in.getHeight() - 1; y >= 0; y--) {
				if (threshold.threshold(in.getRGB(eqXIn, y), current, false) != current) {
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
			current = threshold.threshold(in.getRGB(eqXIn, 0), false, true);

			for (y = 0; y < in.getHeight(); y++) {
				if (threshold.threshold(in.getRGB(eqXIn, y), current, false) != current) {
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
		if (spike) {
			channel1[0] = -1;
			channel2[0] = -1;
		}

		return new EdgeResult(channel1, channel2);
	}

	static String leftPad(int number, int length) {
		StringBuilder result = new StringBuilder(String.valueOf(number));

		while (result.length() < length) {
			result.insert(0, '0');
		}

		return result.toString();
	}
}
