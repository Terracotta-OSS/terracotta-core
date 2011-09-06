/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;

/**
 * Class to facilitate printing out binary data or opaque objects in some sort of other usable form.
 */
public final class Dumper {

  /**
   * Does nothing
   */
  private Dumper() {
    super();
  }

  /**
   * Calls <code>dump(buffer, 10, 16, 2, 8, new OutputStreamWriter(outs))</code>.
   */
  public static final void hexDump(TCByteBuffer buffer, OutputStream outs) throws IOException {
    dump(new ByteishWrapper(null, buffer), 10, 16, 2, 8, new OutputStreamWriter(outs));
  }

  /**
   * Calls <code>dump(buffer, 10, 16, 2, 8, new OutputStreamWriter(outs))</code>.
   */
  public static final void hexDump(TCByteBuffer buffer, Writer writer) throws IOException {
    dump(new ByteishWrapper(null, buffer), 10, 16, 2, 8, writer);
  }

  /**
   * Calls <code>dump(buffer, 10, 16, 2, 8, new OutputStreamWriter(outs))</code>.
   */
  public static final void hexDump(ByteBuffer buffer, OutputStream outs) throws IOException {
    dump(new ByteishWrapper(buffer, null), 10, 16, 2, 8, new OutputStreamWriter(outs));
  }

  /**
   * Calls <code>dump(buffer, 10, 16, 2, 8, new OutputStreamWriter(outs))</code>.
   */
  public static final void hexDump(ByteBuffer buffer, Writer writer) throws IOException {
    dump(new ByteishWrapper(buffer, null), 10, 16, 2, 8, writer);
  }

  /**
   * Calls <code>dump(buffer, 10, 8, 2, 8, new OutputStreamWriter(outs))</code>.
   */
  public static final void octalDump(TCByteBuffer buffer, OutputStream outs) throws IOException {
    dump(new ByteishWrapper(null, buffer), 10, 8, 2, 8, new OutputStreamWriter(outs));
  }

  /**
   * Calls <code>dump(buffer, 10, 8, 2, 8, new OutputStreamWriter(outs))</code>.
   */
  public static final void octalDump(TCByteBuffer buffer, Writer writer) throws IOException {
    dump(new ByteishWrapper(null, buffer), 10, 8, 2, 8, writer);
  }

  /**
   * Calls <code>dump(buffer, 10, 8, 2, 8, new OutputStreamWriter(outs))</code>.
   */
  public static final void octalDump(ByteBuffer buffer, OutputStream outs) throws IOException {
    dump(new ByteishWrapper(buffer, null), 10, 8, 2, 8, new OutputStreamWriter(outs));
  }

  /**
   * Calls <code>dump(buffer, 10, 8, 2, 8, new OutputStreamWriter(outs))</code>.
   */
  public static final void octalDump(ByteBuffer buffer, Writer writer) throws IOException {
    dump(new ByteishWrapper(buffer, null), 10, 8, 2, 8, writer);
  }

  /**
   * Calls
   * <code>dump(buffer, offsetRadix, dataRadix, bytesPerColumn, columnsPerLine, new OutputStreamWriter(outs))</code>.
   */
  public static final void dump(TCByteBuffer buffer, int offsetRadix, int dataRadix, int bytesPerColumn,
                                int columnsPerLine, OutputStream outs) throws IOException {
    dump(new ByteishWrapper(null, buffer), offsetRadix, dataRadix, bytesPerColumn, columnsPerLine,
         new OutputStreamWriter(outs));
  }

  /**
   * Calls <code>dump(buffer, offsetRadix, dataRadix, bytesPerColumn, columnsPerLine, writer)</code>.
   */
  public static final void dump(TCByteBuffer buffer, int offsetRadix, int dataRadix, int bytesPerColumn,
                                int columnsPerLine, Writer writer) throws IOException {
    dump(new ByteishWrapper(null, buffer), offsetRadix, dataRadix, bytesPerColumn, columnsPerLine, writer);
  }

  /**
   * Calls
   * <code>dump(buffer, offsetRadix, dataRadix, bytesPerColumn, columnsPerLine, new OutputStreamWriter(outs))</code>.
   */
  public static final void dump(ByteBuffer buffer, int offsetRadix, int dataRadix, int bytesPerColumn,
                                int columnsPerLine, OutputStream outs) throws IOException {
    dump(new ByteishWrapper(buffer, null), offsetRadix, dataRadix, bytesPerColumn, columnsPerLine,
         new OutputStreamWriter(outs));
  }

  /**
   * Calls <code>dump(buffer, offsetRadix, dataRadix, bytesPerColumn, columnsPerLine, writer)</code>.
   */
  public static final void dump(ByteBuffer buffer, int offsetRadix, int dataRadix, int bytesPerColumn,
                                int columnsPerLine, Writer writer) throws IOException {
    dump(new ByteishWrapper(buffer, null), offsetRadix, dataRadix, bytesPerColumn, columnsPerLine, writer);
  }

  /**
   * Dumps the contents of the buffer in the tradition of the "od" and "hexdump" UNIX utilities.
   * 
   * @param buffer the byte-ish buffer to read from
   * @param offsetRadix the radix to use when printing the offset
   * @param dataRadix the radix to use when printing the data
   * @param bytesPerColumn the number of bytes to concatenate in radix <code>dataRadix</code> in each column (2 is
   *        normal)
   * @param columnsPerLine the number of columns of groups of bytes to have (8 is normal for bases 8 and 16, probably
   *        want less for smaller bases)
   * @param writer the writer to spit the output to
   */
  private static final void dump(ByteishBuffer buffer, int offsetRadix, int dataRadix, int bytesPerColumn,
                                 int columnsPerLine, Writer writer) throws IOException {
    // Do some basic sanity checking
    if (buffer == null || offsetRadix < 2 || dataRadix < 2 || bytesPerColumn < 1 || columnsPerLine < 1
        || writer == null) { return; }
    int dataPadding = 2;
    // Increase the padding per byte for any radix lower than 16
    if (dataRadix < 16) {
      if (dataRadix > 6) {
        dataPadding = 3;
      } else if (dataRadix > 3) {
        dataPadding = 4;
      } else if (dataRadix == 3) {
        dataPadding = 6;
      } else if (dataRadix == 2) {
        dataPadding = 8;
      }
    }
    int bytesPerLine = bytesPerColumn * columnsPerLine;
    int bytesPrintedOnLine = 0;
    for (int pos = 0; pos < buffer.limit(); ++pos) {
      // See if we need to start a new line
      if (bytesPrintedOnLine == bytesPerLine) {
        writer.write("\n");
        writer.flush();
        bytesPrintedOnLine = 0;
      }
      // See if we need to print out the offset
      if (bytesPrintedOnLine == 0) {
        // Print the offset
        writer.write(StringUtil.toPaddedString(pos, offsetRadix, 7));
      }
      // See if we need to print a column break, either after the offset
      // is printed
      // or we've finished a column
      if (bytesPrintedOnLine % bytesPerColumn == 0) {
        writer.write(StringUtil.SPACE_STRING);
      }
      // Print out the next byte in the specified radix with a certain
      // padding
      writer.write(StringUtil.toPaddedString(0x00000000000000ff & buffer.get(pos), dataRadix, dataPadding));
      ++bytesPrintedOnLine;
    }
    // If we didn't fill up the line, fill it with spaces

    // Print the offset of the buffer length + 1 on a line by itself
    writer.write("\n" + StringUtil.toPaddedString(buffer.limit() + 1, offsetRadix, 7) + "\n");
    writer.flush();
  }

  public static String hexDump(byte[] bytes, int length) {
    StringWriter sw = new StringWriter();
    sw.write("\n");
    byte[] dumpBytes = new byte[length];
    for (int pos = 0; pos < length; ++pos)
      dumpBytes[pos] = bytes[pos];
    try {
      Dumper.hexDump(TCByteBufferFactory.copyAndWrap(dumpBytes), sw);
    } catch (IOException ioe) {
      sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      sw.write("Unable to generate hex dump, exception was: ");
      ioe.printStackTrace(pw);
      pw.flush();
    }
    return sw.toString();
  }

}