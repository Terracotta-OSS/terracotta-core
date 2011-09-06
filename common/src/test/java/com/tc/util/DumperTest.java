/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;

import junit.framework.TestCase;

public class DumperTest extends TestCase {

  public final void testHexDumpByteBufferWriter() throws IOException {
    String expectedString =   "0000000 0001 0203 0405 0607 0809 0a0b 0c0d 0e0f\n"
                            + "0000016 1011 1213 1415 1617 1819 1a1b 1c1d 1e1f\n"
                            + "0000032 2021 2223 2425 2627 2829 2a2b 2c2d 2e2f\n"
                            + "0000048 3031 3233 3435 3637 3839 3a3b 3c3d 3e3f\n"
                            + "0000064 4041 4243 4445 4647 4849 4a4b 4c4d 4e4f\n"
                            + "0000080 5051 5253 5455 5657 5859 5a5b 5c5d 5e5f\n"
                            + "0000096 6061 6263 6465 6667 6869 6a6b 6c6d 6e6f\n"
                            + "0000112 7071 7273 7475 7677 7879 7a7b 7c7d 7e7f\n"
                            + "0000128 8081 8283 8485 8687 8889 8a8b 8c8d 8e8f\n"
                            + "0000144 9091 9293 9495 9697 9899 9a9b 9c9d 9e9f\n"
                            + "0000160 a0a1 a2a3 a4a5 a6a7 a8a9 aaab acad aeaf\n"
                            + "0000176 b0b1 b2b3 b4b5 b6b7 b8b9 babb bcbd bebf\n"
                            + "0000192 c0c1 c2c3 c4c5 c6c7 c8c9 cacb cccd cecf\n"
                            + "0000208 d0d1 d2d3 d4d5 d6d7 d8d9 dadb dcdd dedf\n"
                            + "0000224 e0e1 e2e3 e4e5 e6e7 e8e9 eaeb eced eeef\n"
                            + "0000240 f0f1 f2f3 f4f5 f6f7 f8f9 fafb fcfd feff\n"
                            + "0000256 0001 0203 0405 0607 0809 0a0b 0c0d 0e0f\n"
                            + "0000272 1011 1213 1415 1617 1819 1a1b 1c1d 1e1f\n"
                            + "0000288 2021 2223 2425 2627 2829 2a2b 2c2d 2e2f\n"
                            + "0000304 3031 3233 3435 3637 3839 3a3b 3c3d 3e3f\n"
                            + "0000320 4041 4243 4445 4647 4849 4a4b 4c4d 4e4f\n"
                            + "0000336 5051 5253 5455 5657 5859 5a5b 5c5d 5e5f\n"
                            + "0000352 6061 6263 6465 6667 6869 6a6b 6c6d 6e6f\n"
                            + "0000368 7071 7273 7475 7677 7879 7a7b 7c7d 7e7f\n"
                            + "0000384 8081 8283 8485 8687 8889 8a8b 8c8d 8e8f\n"
                            + "0000400 9091 9293 9495 9697 9899 9a9b 9c9d 9e9f\n"
                            + "0000416 a0a1 a2a3 a4a5 a6a7 a8a9 aaab acad aeaf\n"
                            + "0000432 b0b1 b2b3 b4b5 b6b7 b8b9 babb bcbd bebf\n"
                            + "0000448 c0c1 c2c3 c4c5 c6c7 c8c9 cacb cccd cecf\n"
                            + "0000464 d0d1 d2d3 d4d5 d6d7 d8d9 dadb dcdd dedf\n"
                            + "0000480 e0e1 e2e3 e4e5 e6e7 e8e9 eaeb eced eeef\n"
                            + "0000496 f0f1 f2f3 f4f5 f6f7 f8f9 fafb fcfd feff\n"
                            + "0000512 0001 0203 0405 0607 0809 0a0b 0c0d 0e0f\n"
                            + "0000529\n";
    ByteBuffer buffer = ByteBuffer.allocate(528);
    for (int pos = 0; pos < buffer.limit(); ++pos) {
      buffer.put((byte) pos);
    }
    StringWriter sw = new StringWriter();
    Dumper.hexDump(buffer, sw);
    assertNotNull("StringWriter.toString() returned null for some reason", sw.toString());
    assertEquals("hexDump of ByteBuffer did not return expected results", expectedString, sw.toString());
  }

}
