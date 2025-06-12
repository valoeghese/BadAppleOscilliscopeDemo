package valoeghese.badapple;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TextFileOutput extends VideoOutput {
	public TextFileOutput(Path file, int width, int height) throws IOException {
		super(width, height);

		if (!Files.exists(file)) {
			Files.createFile(file);
		}

		this.writer = Files.newBufferedWriter(file);
	}

	private BufferedWriter writer;

	@Override
	public void writeFrame(int[] ...channels) throws IOException {
		StringBuilder builder = new StringBuilder();
		final int len = channels[0].length;
		final int[] lastChannel = channels[channels.length-1];

		for (int x = 0; x < len; x++) {
			for (int[] channel : channels) {
				builder.append(this.getHeight() - channel[x] - 1)
						.append(channel == lastChannel ? '\n' : '\t');
			}
		}

		this.writer.write(builder.toString());
	}

	@Override
	public void close() throws IOException {
		this.writer.flush();
		this.writer.close();
	}
}
