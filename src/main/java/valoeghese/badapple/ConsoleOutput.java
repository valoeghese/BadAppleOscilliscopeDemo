package valoeghese.badapple;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConsoleOutput extends VideoOutput {
    public ConsoleOutput() {
        super(64, 24);
        try{
            this.writer = new BufferedWriter(Files.newBufferedWriter(Path.of("out", "apple.cpp")));
            this.writer.write("#include <iostream>");
            this.writer.newLine();
            this.writer.write("#include <chrono>");
            this.writer.newLine();
            this.writer.write("#include <thread>");
            this.writer.newLine();
            this.writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);//bad practise
        }

    }

    final BufferedWriter writer;
    int i = 0;

    @Override
    public void writeFrame(int[][] channels) throws IOException {
        this.writer.write("void frame" + (i++) + "() {");
        this.writer.newLine();
        for (int[] row : channels) {
            this.writer.write("  ");
            this.writer.write("std::cout << \"");
            for (int c : row) this.writer.write(c);
            this.writer.write("\\n\";");
            this.writer.newLine();
        }
        this.writer.write("}");
        this.writer.newLine();
    }

    @Override
    public void close() throws IOException {
        this.writer.write("int main() {");
        this.writer.newLine();
        for (int ii = 0; ii < i; ii++) {
            this.writer.write("  ");
            this.writer.write("frame" + (ii) + "();");
            this.writer.newLine();
            this.writer.write("std::this_thread::sleep_for(std::chrono::nanoseconds(18333300));");
            this.writer.newLine();
        }
        this.writer.write("}");
        this.writer.newLine();
        this.writer.close();
    }
}
