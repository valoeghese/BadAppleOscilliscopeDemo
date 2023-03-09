package valoeghese.badapple;

import javax.imageio.ImageIO;
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

				BufferedImage outputFrame = detectEdges(frame);

				Path outputFile = outputFolder.resolve("output_" + i);
				Files.createFile(outputFile);

				try (OutputStream stream = new BufferedOutputStream(Files.newOutputStream(outputFile))) {
					ImageIO.write(outputFrame, "png", stream);
				}
			}

			time = System.currentTimeMillis() - time;
			System.out.println("Processed " + i + " frames in " + time + " ms.");
		}
	}
}
