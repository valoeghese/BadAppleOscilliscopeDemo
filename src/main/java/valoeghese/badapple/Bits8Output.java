package valoeghese.badapple;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Bits8Output extends VideoOutput {
	public Bits8Output(Path file, int width, int height) throws IOException {
		super(width, height);

		if (!Files.exists(file)) {
			Files.createFile(file);
		}

		this.outputStream = new DataOutputStream(Files.newOutputStream(file));
	}

	private DataOutputStream outputStream;

	@Override
	public void writeFrame(int[] channel1, int[] channel2) throws IOException {
		for (int x = 0; x < channel1.length; x++) {
			this.outputStream.writeByte(channel1[x] - 1);
			this.outputStream.writeByte(channel2[x] - 1);
		}
	}

	@Override
	public void close() throws IOException {
		this.outputStream.flush();
		this.outputStream.close();
	}
}
