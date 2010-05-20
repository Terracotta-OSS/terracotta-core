/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.io.TCDataInput;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.compression.CompressedData;
import com.tc.object.compression.StringCompressionUtil;
import com.tc.object.dna.impl.BaseDNAEncodingImpl;
import com.tc.object.loaders.ClassProvider;
import com.tc.util.Assert;
import com.tc.util.StringTCUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;

public class ApplicatorDNAEncodingImpl extends BaseDNAEncodingImpl {

  private static final Constructor COMPRESSED_STRING_CONSTRUCTOR;
  private static final TCLogger    logger = TCLogging.getLogger(ApplicatorDNAEncodingImpl.class);

  static {
    Constructor cstr = null;

    try {
      cstr = String.class
          .getDeclaredConstructor(new Class[] { Boolean.TYPE, char[].class, Integer.TYPE, Integer.TYPE });
    } catch (final NoSuchMethodException e) {
      logger.info("Compressed String constructor not present");
    }

    COMPRESSED_STRING_CONSTRUCTOR = cstr;
  }

  /**
   * Used in the Applicators. The policy is set to APPLICATOR.
   */
  public ApplicatorDNAEncodingImpl(final ClassProvider classProvider) {
    super(classProvider);
  }

  @Override
  protected boolean useStringEnumRead(final byte type) {
    return true;
  }

  @Override
  protected boolean useClassProvider(final byte type, final byte typeToCheck) {
    return true;
  }

  @Override
  protected boolean useUTF8String(final byte type) {
    return true;
  }

  @Override
  protected Object readCompressedString(final TCDataInput input) throws IOException {
    final boolean isInterned = input.readBoolean();

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

    if (isInterned) {
      if (STRING_COMPRESSION_LOGGING_ENABLED) {
        logger.info("Interning string.");
      }
      return StringTCUtil.intern(s);
    } else {
      return s;
    }
  }

  private String constructCompressedString(final char[] compressedChars, final int stringLength, final int stringHash) {
    if (COMPRESSED_STRING_CONSTRUCTOR == null) {
      final byte[] utf8bytes = StringCompressionUtil.unpackAndDecompress(compressedChars);
      try {
        return new String(utf8bytes, "UTF-8");
      } catch (final UnsupportedEncodingException e) {
        throw new AssertionError(e);
      }
    }

    try {
      return (String) COMPRESSED_STRING_CONSTRUCTOR.newInstance(new Object[] { Boolean.TRUE, compressedChars,
          new Integer(stringLength), new Integer(stringHash) });
    } catch (final Exception e) {
      throw Assert.failure(e.getMessage(), e);
    }
  }

}
