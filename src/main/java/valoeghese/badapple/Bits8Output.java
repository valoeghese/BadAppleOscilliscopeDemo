package valoeghese.badapple;

import org.jetbrains.annotations.Nullable;

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
	public void writeFrame(int[] ...channels) throws IOException {
		for (int x = 0; x < channels[0].length; x++) {
            for (int[] channel : channels) {
                this.outputStream.writeByte(this.getHeight() - channel[x] - 1);
            }
		}
	}

	@Override
	public void close() throws IOException {
		this.outputStream.flush();
		this.outputStream.close();
	}
}
