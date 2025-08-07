import java.io.*;
import java.nio.file.*;

public class FileTransfer {
    public static byte[] fileToBytes(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    public static void saveBytesToFile(byte[] data, String fileName) throws IOException {
        File dir = new File("downloads");
        if (!dir.exists()) dir.mkdir();
        File file = new File(dir, fileName);
        Files.write(file.toPath(), data);
    }
}
