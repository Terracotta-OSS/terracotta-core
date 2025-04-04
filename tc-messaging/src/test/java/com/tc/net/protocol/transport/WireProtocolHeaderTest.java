/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.protocol.transport;

import java.util.Arrays;
import java.util.zip.Adler32;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.util.Conversion;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author teck
 */
public class WireProtocolHeaderTest {

  private static byte[] goodHeader = { // make formatter pretty
                                   (byte) 0x28, // version == 2, length = 32
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
                                   };

  static {
    // IOFlavor.forceJDK13();
    Adler32 adler = new Adler32();
    adler.update(goodHeader);
    System.arraycopy(Conversion.uint2bytes(adler.getValue()), 0, goodHeader, 12, 4);
  }

  private byte[] getGoodHeader() {
    byte[] rv = new byte[goodHeader.length];
    System.arraycopy(goodHeader, 0, rv, 0, rv.length);
    return rv;
  }

  // XXX: Finish any and all other tests!
  
  @Test
  public void testOptions() {
    WireProtocolHeader header = new WireProtocolHeader();

    byte oldLength = header.getHeaderLength();
    header.setOptions(new byte[] {});
    byte newLength = header.getHeaderLength();
    assertTrue(newLength >= oldLength);
    assertTrue(newLength == (WireProtocolHeader.MIN_LENGTH / 4));

    byte[] maxOptions = new byte[WireProtocolHeader.MAX_LENGTH - WireProtocolHeader.MIN_LENGTH];
    Arrays.fill(maxOptions, (byte) 0xFF);
    header.setOptions(maxOptions);
    assertTrue(header.getHeaderLength() * 4 == WireProtocolHeader.MAX_LENGTH);
    Arrays.equals(maxOptions, header.getOptions());

    header.setOptions(null);
    assertTrue(header.getHeaderLength() * 4 == WireProtocolHeader.MIN_LENGTH);
    assertTrue(header.getOptions().length == 0);
  }

  @Test
  public void testVersion() {
    WireProtocolHeader header = new WireProtocolHeader();

    header.setVersion((byte) 15);
    assertTrue(15 == header.getVersion());

    header.setVersion(WireProtocolHeader.VERSION_2);
    assertTrue(WireProtocolHeader.VERSION_2 == header.getVersion());

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

  @Test
  public void testGoodHeader() {
    byte[] data = getGoodHeader();
    TCByteBuffer buffer = TCByteBufferFactory.getInstance(WireProtocolHeader.MAX_LENGTH);
    buffer.put(data);
    buffer.flip();

    WireProtocolHeader header = new WireProtocolHeader(buffer);

    assertTrue(header.isChecksumValid());

    assertTrue(header.getVersion() == WireProtocolHeader.VERSION_2);
    assertTrue(header.getHeaderLength() == 8);
    assertTrue(header.getTypeOfService() == 2);
    assertTrue(header.getTimeToLive() == 3);

    assertTrue(header.getTotalPacketLength() == 255);
    assertTrue(header.getChecksum() == 2744585179L);
    assertTrue(Arrays.equals(header.getSourceAddress(), new byte[] { (byte) 0xFF, (byte) 1, (byte) 0xFF, (byte) 1 }));
    assertTrue(Arrays.equals(header.getDestinationAddress(),
                             new byte[] { (byte) 1, (byte) 0xFF, (byte) 1, (byte) 0xFF }));
    assertTrue(header.getSourcePort() == 43605);
    assertTrue(header.getDestinationPort() == 21930);
    assertTrue(header.getMessageCount() == 5);
    assertTrue(header.getOptions().length == 0);

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
