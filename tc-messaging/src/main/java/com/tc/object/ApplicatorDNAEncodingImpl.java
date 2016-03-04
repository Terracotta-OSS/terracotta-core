/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object;

import com.tc.io.TCDataInput;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.compression.CompressedData;
import com.tc.object.compression.StringCompressionUtil;
import com.tc.object.dna.impl.BaseDNAEncodingImpl;
import com.tc.object.loaders.ClassProvider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class ApplicatorDNAEncodingImpl extends BaseDNAEncodingImpl {
  
  private static final TCLogger    logger = TCLogging.getLogger(ApplicatorDNAEncodingImpl.class);

  /**
   * Used in the Applicators. The policy is set to APPLICATOR.
   */
  public ApplicatorDNAEncodingImpl(ClassProvider classProvider) {
    super(classProvider);
  }

  @Override
  protected boolean useStringEnumRead(byte type) {
    return true;
  }

  @Override
  protected boolean useClassProvider(byte type, byte typeToCheck) {
    return true;
  }

  @Override
  protected boolean useUTF8String(byte type) {
    return true;
  }

  @Override
  protected Object readCompressedString(TCDataInput input) throws IOException {
    final int uncompressedByteLength = input.readInt();
    final byte[] data = readByteArray(input);

    final int stringLength = input.readInt();
    final int stringHash = input.readInt();

    // Pack byte[] into char[] (still compressed)
    final char[] compressedChars = StringCompressionUtil
        .packCompressed(new CompressedData(data, uncompressedByteLength));

    final String s = constructCompressedString(compressedChars, stringLength, stringHash);
    if (STRING_COMPRESSION_LOGGING_ENABLED) {
      logger.info("Read compressed String of compressed size : " + compressedChars.length + ", uncompressed size : "
                  + stringLength + ", hash code : " + stringHash);
    }

    return s;
  }

  private String constructCompressedString(char[] compressedChars, int stringLength, int stringHash) {
    final byte[] utf8bytes = StringCompressionUtil.unpackAndDecompress(compressedChars);
    try {
      return new String(utf8bytes, "UTF-8");
    } catch (final UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

}
