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

public class ChecksumUtilTest extends TestCase {

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
    File srcFile = FileUtils.toFile(getClass().getResource(TEST_CHECKSUM_DATA_TXT));
    File md5File = FileUtils.toFile(getClass().getResource(TEST_CHECKSUM_DATA_TXT_MD5));
    try {
      Assert.assertTrue(ChecksumUtil.verifyMD5Sum(srcFile, md5File));
    } catch (NoSuchAlgorithmException e) {
      Assert.fail("MD5 cryptographic algorithm should be available.");
    } catch (IOException e) {
      Assert.fail("srcFile: and md5File: should be available and readable.");
    }
  }

  public void testVerifyMD5SumPassAltFormat() {
    File srcFile = FileUtils.toFile(getClass().getResource(TEST_CHECKSUM_DATA_TXT));
    File md5File = FileUtils.toFile(getClass().getResource(TEST_CHECKSUM_DATA_TXT_MD5_ALT_FORMAT));
    try {
      Assert.assertTrue(ChecksumUtil.verifyMD5Sum(srcFile, md5File));
    } catch (NoSuchAlgorithmException e) {
      Assert.fail("MD5 cryptographic algorithm should be available.");
    } catch (IOException e) {
      Assert.fail("srcFile: and md5File: should be available and readable.");
    }
  }

  /**
   * Test that the verifyMD5Sum(..) will return false if the content of the MD5 file does not match the computed MD5 sum
   * of the file.
   */
  public void testVerifyMD5SumFail() {
    File srcFile = FileUtils.toFile(getClass().getResource(TEST_CHECKSUM_DATA_TXT));
    File md5File = FileUtils.toFile(getClass().getResource(TEST_CHECKSUM_DATA_TXT_MD5_NO_MATCH));
    try {
      Assert.assertFalse(ChecksumUtil.verifyMD5Sum(srcFile, md5File));
    } catch (NoSuchAlgorithmException e) {
      Assert.fail("MD5 cryptographic algorithm should be available.");
    } catch (IOException e) {
      Assert.fail("srcFile: and md5File: should be available and readable.");
    }
  }

  /**
   * Test that a NumberFormatException is thrown when the MD5 file does not use the expected format.
   */
  public void testBadMD5FileFormat() {
    File srcFile = FileUtils.toFile(getClass().getResource(TEST_CHECKSUM_DATA_TXT));
    File md5File = FileUtils.toFile(getClass().getResource(TEST_CHECKSUM_DATA_TXT_MD5_BAD_FORMAT));
    try {
      ChecksumUtil.verifyMD5Sum(srcFile, md5File);
      Assert.fail("Should have thrown a NumberFormatException");
    } catch (NumberFormatException e) {
      // as expected - so keep quiet
    } catch (NoSuchAlgorithmException e) {
      Assert.fail("MD5 cryptographic algorithm should be available.");
    } catch (IOException e) {
      Assert.fail("srcFile: and md5File: should be available and readable.");
    }
  }
}
