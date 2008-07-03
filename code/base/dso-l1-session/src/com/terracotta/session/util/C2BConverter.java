/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

/**
 * Efficient conversion of character to bytes. This uses the standard JDK mechansim - a writer - but provides mechanisms
 * to recycle all the objects that are used. It is compatible with JDK1.1 and up, ( nio is better, but it's not
 * available even in 1.2 or 1.3 )
 */
public final class C2BConverter {

  private final IntermediateOutputStream ios;
  private final WriteConvertor           conv;
  private ByteChunk                      bb;
  private final String                   enc;

  /**
   * Create a converter, with bytes going to a byte buffer
   */
  public C2BConverter(ByteChunk output, String encoding) throws IOException {
    this.bb = output;
    ios = new IntermediateOutputStream(output);
    conv = new WriteConvertor(ios, encoding);
    this.enc = encoding;
  }

  /**
   * Create a converter
   */
  public C2BConverter(String encoding) throws IOException {
    this(new ByteChunk(1024), encoding);
  }

  public ByteChunk getByteChunk() {
    return bb;
  }

  public String getEncoding() {
    return enc;
  }

  public void setByteChunk(ByteChunk bb) {
    this.bb = bb;
    ios.setByteChunk(bb);
  }

  /**
   * Reset the internal state, empty the buffers. The encoding remain in effect, the internal buffers remain allocated.
   */
  public final void recycle() {
    conv.recycle();
    bb.recycle();
  }

  /**
   * Generate the bytes using the specified encoding
   */
  public final void convert(char c[], int off, int len) throws IOException {
    conv.write(c, off, len);
  }

  /**
   * Generate the bytes using the specified encoding
   */
  public final void convert(String s, int off, int len) throws IOException {
    conv.write(s, off, len);
  }

  /**
   * Generate the bytes using the specified encoding
   */
  public final void convert(String s) throws IOException {
    conv.write(s);
  }

  /**
   * Generate the bytes using the specified encoding
   */
  public final void convert(char c) throws IOException {
    conv.write(c);
  }

  /**
   * Flush any internal buffers into the ByteOutput or the internal byte[]
   */
  public final void flushBuffer() throws IOException {
    conv.flush();
  }

}

// -------------------- Private implementation --------------------

/**
 * Special writer class, where close() is overritten. The default implementation would set byteOutputter to null, and
 * the writter can't be recycled. Note that the flush method will empty the internal buffers _and_ call flush on the
 * output stream - that's why we use an intermediary output stream that overrides flush(). The idea is to have full
 * control: flushing the char->byte converter should be independent of flushing the OutputStream. When a WriteConverter
 * is created, it'll allocate one or 2 byte buffers, with a 8k size that can't be changed ( at least in JDK1.1 -> 1.4 ).
 * It would also allocate a ByteOutputter or equivalent - again some internal buffers. It is essential to keep this
 * object around and reuse it. You can use either pools or per thread data - but given that in most cases a converter
 * will be needed for every thread and most of the time only 1 ( or 2 ) encodings will be used, it is far better to keep
 * it per thread and eliminate the pool overhead too.
 */
final class WriteConvertor extends OutputStreamWriter {
  // stream with flush() and close(). overriden.
  private final IntermediateOutputStream ios;

  // Has a private, internal byte[8192]

  /**
   * Create a converter.
   */
  public WriteConvertor(IntermediateOutputStream out, String enc) throws UnsupportedEncodingException {
    super(out, enc);
    ios = out;
  }

  /**
   * Overriden - will do nothing but reset internal state.
   */
  public final void close() {
    // NOTHING
    // Calling super.close() would reset out and cb.
  }

  /**
   * Flush the characters only
   */
  public final void flush() throws IOException {
    // Will flushBuffer and out()
    // flushBuffer put any remaining chars in the byte[]
    super.flush();
  }

  public final void write(char cbuf[], int off, int len) throws IOException {
    // will do the conversion and call write on the output stream
    super.write(cbuf, off, len);
  }

  /**
   * Reset the buffer
   */
  public final void recycle() {
    ios.disable();
    try {
      // System.out.println("Reseting writer");
      flush();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ios.enable();
  }

}

/**
 * Special output stream where close() is overriden, so super.close() is never called. This allows recycling. It can
 * also be disabled, so callbacks will not be called if recycling the converter and if data was not flushed.
 */
final class IntermediateOutputStream extends OutputStream {
  private ByteChunk tbuff;
  private boolean   enabled = true;

  public IntermediateOutputStream(ByteChunk tbuff) {
    this.tbuff = tbuff;
  }

  public final void close() throws IOException {
    // shouldn't be called - we filter it out in writer
    throw new IOException("close() called - shouldn't happen ");
  }

  public final void flush() {
    // nothing - write will go directly to the buffer,
    // we don't keep any state
  }

  public final void write(byte cbuf[], int off, int len) throws IOException {
    // will do the conversion and call write on the output stream
    if (enabled) {
      tbuff.append(cbuf, off, len);
    }
  }

  public final void write(int i) throws IOException {
    throw new IOException("write( int ) called - shouldn't happen ");
  }

  // -------------------- Internal methods --------------------

  void setByteChunk(ByteChunk bb) {
    tbuff = bb;
  }

  /**
   * Temporary disable - this is used to recycle the converter without generating an output if the buffers were not
   * flushed
   */
  final void disable() {
    enabled = false;
  }

  /**
   * Reenable - used to recycle the converter
   */
  final void enable() {
    enabled = true;
  }
}
