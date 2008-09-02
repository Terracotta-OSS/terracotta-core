/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.util;

import org.apache.commons.io.FileUtils;

import com.tc.util.Assert;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import junit.framework.TestCase;

public final class ChecksumUtilTest extends TestCase {

  private static final String TEST_CHECKSUM_DATA_TXT_MD5_BAD_FORMAT = "/testChecksumData.txt.md5-bad.format";
  private static final String TEST_CHECKSUM_DATA_TXT_MD5_ALT_FORMAT = "/testChecksumData.txt.md5-alt.format";
  private static final String TEST_CHECKSUM_DATA_TXT_MD5_NO_MATCH   = "/testChecksumData.txt.md5-no.match";
  private static final String TEST_CHECKSUM_DATA_TXT_MD5            = "/testChecksumData.txt.md5";
  private static final String TEST_CHECKSUM_DATA_TXT                = "/testChecksumData.txt";

  /**
   * Test that the verifyMD5Sum(..) will return true if the content of the MD5 file matches the computed MD5 sum of the
   * file.
   */
  public void testVerifyMD5SumPass() {
    Assert.assertTrue(verifyMD5Sum(TEST_CHECKSUM_DATA_TXT_MD5));
  }

  /**
   * Test that the verifyMD5Sum(..) will still return true if the content of the MD5 file matches the computed MD5 sum
   * of the file and the MD5 file is using the md5sum tool's default report format.
   */
  public void testVerifyMD5SumPassAltFormat() {
    Assert.assertTrue(verifyMD5Sum(TEST_CHECKSUM_DATA_TXT_MD5_ALT_FORMAT));
  }

  /**
   * Test that the verifyMD5Sum(..) will return false if the content of the MD5 file does not match the computed MD5 sum
   * of the file.
   */
  public void testVerifyMD5SumFail() {
    Assert.assertFalse(verifyMD5Sum(TEST_CHECKSUM_DATA_TXT_MD5_NO_MATCH));
  }

  /**
   * Test that a NumberFormatException is thrown when the MD5 file does not use the expected format.
   */
  public void testBadMD5FileFormat() {
    try {
      verifyMD5Sum(TEST_CHECKSUM_DATA_TXT_MD5_BAD_FORMAT);
      Assert.fail("Should have thrown a NumberFormatException");
    } catch (NumberFormatException e) {
      // as expected - so keep quiet
    }
  }

  /**
   * Helper method to setup and call ChecksumUtil.verifyMD5Sum(..)
   * 
   * @param md5Filename The MD5 data file used when invoking ChecksumUtil.verifyMD5Sum(..)
   */
  private boolean verifyMD5Sum(String md5Filename) {
    try {
      File srcFile = FileUtils.toFile(getClass().getResource(TEST_CHECKSUM_DATA_TXT));
      File md5File = FileUtils.toFile(getClass().getResource(md5Filename));
      return ChecksumUtil.verifyMD5Sum(srcFile, md5File);
    } catch (NoSuchAlgorithmException e) {
      Assert.fail("MD5 cryptographic algorithm should be available.");
    } catch (IOException e) {
      Assert.fail("srcFile: and md5File: should be available and readable.");
    }
    return false;
  }
}
