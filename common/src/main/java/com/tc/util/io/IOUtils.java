package com.tc.util.io;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * @author vmad
 */
public class IOUtils {
    public static int copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[1024]; // use some default size buffer
        int nr = 0, n;
        while((n = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, n);
            nr += n;
        }
        return nr;
    }

    public static void closeQuietly(InputStream inputStream) {
        if(inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException ignore) {
            }
        }
    }

    public static void closeQuietly(OutputStream outputStream) {
        if(outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException ignore) {
            }
        }
    }

    public static void closeQuietly(Reader reader) {
        if(reader != null) {
            try {
                reader.close();
            } catch (IOException ignore) {
            }
        }
    }

    public static void closeQuietly(Writer writer) {
        if(writer != null) {
            try {
                writer.close();
            } catch (IOException ignore) {
            }
        }
    }
}
