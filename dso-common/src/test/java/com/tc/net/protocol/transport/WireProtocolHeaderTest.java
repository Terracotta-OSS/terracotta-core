/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import org.apache.commons.lang.ArrayUtils;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.util.Conversion;

import java.util.Arrays;
import java.util.zip.Adler32;

import junit.framework.TestCase;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author teck
 */
public class WireProtocolHeaderTest extends TestCase {

  private final static byte[] goodHeader = { // make formatter pretty
                                   (byte) 0x2A, // version == 2, length = 40
                                   (byte) 2, // TOS == 2
                                   (byte) 3, // TTL == 3
                                   (byte) 5, // Protocol == 5
                                   (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, // Magic num
                                   (byte) 0, (byte) 0, (byte) 0, (byte) 0xFF, // totalLen == 255
                                   (byte) 0, (byte) 0, (byte) 0, (byte) 0, // adler initially zero
                                   (byte) 0xFF, (byte) 1, (byte) 0xFF, (byte) 1, // source addr
                                   (byte) 1, (byte) 0xFF, (byte) 1, (byte) 0xFF, // dest addr
                                   (byte) 0xAA, (byte) 0x55, (byte) 0x55, (byte) 0xAA, // src/dest ports
                                   (byte) 0x00, (byte) 0x05, (byte) 0xFF, (byte) 0xFF, // message count=5 and fill
                                   (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, // timestamp
                                   (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, // timestamp
                                   };

  static {
    // IOFlavor.forceJDK13();
    Adler32 adler = new Adler32();
    adler.update(goodHeader);
    System.arraycopy(Conversion.uint2bytes(adler.getValue()), 0, goodHeader, 12, 4);
  }

  private static byte[] getGoodHeader() {
    byte[] rv = new byte[goodHeader.length];
    System.arraycopy(goodHeader, 0, rv, 0, rv.length);
    return rv;
  }

  // XXX: Finish any and all other tests!

  public void testOptions() {
    WireProtocolHeader header = new WireProtocolHeader();

    byte oldLength = header.getHeaderLength();
    header.setOptions(ArrayUtils.EMPTY_BYTE_ARRAY);
    byte newLength = header.getHeaderLength();
    assertTrue(newLength >= oldLength);
    assertEquals(WireProtocolHeader.MIN_LENGTH / 4, newLength);

    byte[] maxOptions = new byte[WireProtocolHeader.MAX_LENGTH - WireProtocolHeader.MIN_LENGTH];
    Arrays.fill(maxOptions, (byte) 0xFF);
    header.setOptions(maxOptions);
    assertEquals(WireProtocolHeader.MAX_LENGTH, header.getHeaderLength() * 4);
    assertArrayEquals(maxOptions, header.getOptions());
    header.setOptions(null);
    assertEquals(WireProtocolHeader.MIN_LENGTH, header.getHeaderLength() * 4);
    assertEquals(0, header.getOptions().length);
  }

  public void testVersion() {
    WireProtocolHeader header = new WireProtocolHeader();

    header.setVersion((byte) 15);
    assertEquals(15, header.getVersion());

    header.setVersion(WireProtocolHeader.VERSION_2);
    assertEquals(WireProtocolHeader.VERSION_2, header.getVersion());

    boolean exception = false;

    try {
      header.setVersion((byte) 0);
    } catch (Exception e) {
      exception = true;
    }
    assertTrue(exception);

    exception = false;
    try {
      header.setVersion((byte) 16);
    } catch (Exception e) {
      exception = true;
    }
    assertTrue(exception);

    exception = false;
    try {
      header.setVersion((byte) -1);
    } catch (Exception e) {
      exception = true;
    }
    assertTrue(exception);
  }

  public void testGoodHeader() {
    byte[] data = getGoodHeader();
    TCByteBuffer buffer = TCByteBufferFactory.getInstance(false, WireProtocolHeader.MAX_LENGTH);
    buffer.put(data);
    buffer.flip();

    WireProtocolHeader header = new WireProtocolHeader(buffer);

    System.out.println(header);

    assertTrue(header.isChecksumValid());

    assertEquals(WireProtocolHeader.VERSION_2, header.getVersion());
    assertEquals(10, header.getHeaderLength());
    assertEquals(2, header.getTypeOfService());
    assertEquals(3, header.getTimeToLive());

    assertEquals(255, header.getTotalPacketLength());
    assertEquals(47057885L, header.getChecksum());
    assertArrayEquals(header.getSourceAddress(), new byte[] { (byte)0xFF, (byte)1, (byte)0xFF, (byte)1 });
    assertArrayEquals(header.getDestinationAddress(), new byte[] { (byte)1, (byte)0xFF, (byte)1, (byte)0xFF });
    assertEquals(43605, header.getSourcePort());
    assertEquals(21930, header.getDestinationPort());
    assertEquals(5, header.getMessageCount());
    assertEquals(0L, header.getTimestamp());
    assertEquals(0, header.getOptions().length);

    try {
      header.validate();
    } catch (WireProtocolHeaderFormatException e) {
      fail(e.getMessage());
    }

    // changing data in the header should cause the checksum to need to be recomputed
    header.setVersion((byte) (header.getVersion() + 1));
    assertFalse(header.isChecksumValid());

    // Fix and validate the checksum
    header.computeChecksum();
    assertTrue(header.isChecksumValid());
  }

}
