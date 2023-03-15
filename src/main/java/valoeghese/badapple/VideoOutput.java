package valoeghese.badapple;

import java.io.IOException;

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
	 * @param channel1 the data to write to channel 1.
	 * @param channel2 the data to write to channel 2.
	 * @throws IOException if an IOException occurs during frame writing.
	 */
	public abstract void writeFrame(int[] channel1, int[] channel2) throws IOException;

	/**
	 * Called upon the end of the video output, to do anything that needs to be handled then, such as closing an output stream.
	 * @throws IOException if an IOException occurs during handling.
	 */
	public abstract void close() throws IOException;
}
