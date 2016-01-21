package com.tc.util.io;

import java.io.File;
import java.io.IOException;

/**
 * @author vmad
 */
public class FileUtils {
    public static void forceMkdir(File directory) throws IOException {
        if(directory.exists()) {
            if(!directory.isDirectory()) {
                throw new IOException("A file with given directory name (" + directory + ") exists");
            }
        } else {
            if(!directory.mkdirs()) {
                throw new IOException("Couldn't create directory " + directory);
            }
        }
    }
}
