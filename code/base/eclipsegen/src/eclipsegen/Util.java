package eclipsegen;

import java.io.File;

public class Util {

  public static void ensureFile(File file) {
    if (!file.isFile()) { throw new RuntimeException("File does not exist or is not a file: " + file.getAbsolutePath()); }

    if (!file.canRead()) { throw new RuntimeException("File cannot be read: " + file.getAbsolutePath()); }
  }

  public static void ensureFile(String file) {
    ensureFile(new File(file));
  }

  public static void ensureDir(File dir) {
    if (!dir.isDirectory()) { throw new RuntimeException("Dir does not exist or is not a directory: "
                                                         + dir.getAbsolutePath()); }

    if (!dir.canRead()) { throw new RuntimeException("Dir cannot be read: " + dir.getAbsolutePath()); }

  }

}
