/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.io.TCDataInput;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLoggingService;
import com.tc.object.compression.CompressedData;
import com.tc.object.compression.StringCompressionUtil;
import com.tc.object.dna.impl.BaseDNAEncodingImpl;
import com.tc.object.loaders.ClassProvider;
import com.tc.util.ServiceUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class ApplicatorDNAEncodingImpl extends BaseDNAEncodingImpl {
  
  private static final TCLogger    logger = ServiceUtil.loadService(TCLoggingService.class)
                                              .getLogger(ApplicatorDNAEncodingImpl.class);

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
