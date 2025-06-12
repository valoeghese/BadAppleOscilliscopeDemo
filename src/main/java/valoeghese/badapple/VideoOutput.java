package valoeghese.badapple;

import java.io.IOException;
import org.jetbrains.annotations.Nullable;

/**
 * Writes two-channel video output of channel X by Y. Subclasses represent different methods of storing this video output.
 */
public abstract class VideoOutput {
	public VideoOutput(int width, int height) {
		this.width = width;
		this.height = height;
	}

	private final int width;
	private final int height;

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	/**
	 * Write a frame of data to this output. The arrays for channel 1 and channel 2 are guaranteed to be the same length.
	 * @param channels the data to write to each channel
	 * @throws IOException if an IOException occurs during frame writing.
	 */
	public abstract void writeFrame(int[] ...channels) throws IOException;

	/**
	 * Called upon the end of the video output, to do anything that needs to be handled then, such as closing an output stream.
	 * @throws IOException if an IOException occurs during handling.
	 */
	public abstract void close() throws IOException;
}
