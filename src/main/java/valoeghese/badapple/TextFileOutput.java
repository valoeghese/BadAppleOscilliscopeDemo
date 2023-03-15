package valoeghese.badapple;

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
	public void writeFrame(int[] channel1, int[] channel2) throws IOException {
		StringBuilder builder = new StringBuilder();

		for (int x = 0; x < channel1.length; x++) {
			builder.append(this.getHeight() - channel1[x]).append("\t")
					.append(this.getHeight() - channel2[x]).append("\n");
		}

		this.writer.write(builder.toString());
	}

	@Override
	public void close() throws IOException {
		this.writer.flush();
		this.writer.close();
	}
}
