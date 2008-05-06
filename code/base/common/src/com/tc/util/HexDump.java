/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.exception.TCRuntimeException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Knows how to dump out byte arrays in the style of emacs' hexl-mode.
 */
public class HexDump {

  private static final int BYTES_PER_LINE = 16;

  public static String dump(byte[] data) {
    return dump(data, 0, data.length);
  }

  public static String dump(byte[] data, int offset, int length) {
    try {
      StringWriter writer = new StringWriter();
      dump(data, offset, length, writer);
      return writer.toString();
    } catch (IOException ioe) {
      throw new TCRuntimeException("How'd we get an IOException with an in-memory stream?", ioe);
    }
  }

  public static void dump(byte[] data, Writer out) throws IOException {
    dump(data, 0, data.length, out);
  }

  public static void dump(byte[] data, int offset, int length, Writer out) throws IOException {
    Assert.assertNotNull(data);
    Assert.assertNotNull(out);
    Assert.eval(offset >= 0);
    Assert.eval(offset + length <= data.length);
    Assert.eval(length >= 0);

    boolean multiline = length > BYTES_PER_LINE;
    byte[] thisLine = new byte[BYTES_PER_LINE];

    out.write(length + " byte" + (length == 1 ? "" : "s") + ":");
    if (multiline) out.write("\n");
    else out.write(" ");

    int linePos = 0;
    for (int i = 0; i < length; ++i) {
      if (i % BYTES_PER_LINE == 0 && multiline) {
        if (i != 0) out.write("\n");
        out.write(padHex(i, 8) + ":");
        linePos = 0;
      }

      byte b = data[offset + i];
      thisLine[linePos++] = b;

      if (i % 2 == 0 && (multiline || i != 0)) out.write(" ");
      out.write(padHex((b & 0x000000FF), 2));

      if (i == (length - 1)) {
        int onLastLine = length - ((length / BYTES_PER_LINE) * BYTES_PER_LINE);
        if (onLastLine == 0) onLastLine = BYTES_PER_LINE;
        int remaining = BYTES_PER_LINE - onLastLine;

        if (multiline) {
          if (remaining == 0) out.write(" ");
          if (remaining % 2 != 0) {
            out.write("   ");
            remaining--;
          }

          Assert.eval(remaining % 2 == 0);

          while (remaining > 0) {
            out.write("     ");
            remaining -= 2;
          }

          out.write(" ");
        } else {
          out.write("  ");
        }

        for (int j = 0; j < onLastLine; ++j) {
          out.write(getChar(thisLine[j]));
        }

        if (multiline) out.write("\n");
      } else if ((i + 1) % BYTES_PER_LINE == 0 && i != 0) {
        out.write("  ");
        for (int j = 0; j < thisLine.length; ++j) {
          out.write(getChar(thisLine[j]));
        }
      }
    }
  }

  /**
   * @return a "raw" hex dump (2 hex characters per byte), not in emacs' hexl mode.
   */
  public static String rawHexDump(byte[] data) {
    StringBuffer hexDump = new StringBuffer();
    for (int pos = 0; pos < data.length; ++pos)
      hexDump.append(padHex(0xff & data[pos], 2));
    return hexDump.toString();
  }

  private static char getChar(byte b) {
    int val = b & 0x000000FF;
    if (val < 0x20 || val > 0x7E) return '.';
    else return (char) b;
  }

  private static String padHex(int num, int totalLength) {
    String out = Integer.toHexString(num);
    while (out.length() < totalLength) {
      out = "0" + out;
    }
    return out;
  }

}