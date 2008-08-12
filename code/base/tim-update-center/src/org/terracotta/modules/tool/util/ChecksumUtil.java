/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for calculating and verifying the MD5 sum of a file.
 */
public class ChecksumUtil {

  /**
   * Calculates an MD5 sum based on the contents of a file.
   * 
   * @param source The input file whose contents is used to calculate an MD5 sum.
   * @return The MD5 sum of the contents of the file.
   * @throws NoSuchAlgorithmException If the MD5 cryptographic algorithm is requested but is not available in the
   *         environment.
   * @throws IOException If unable to process the input file for any reason.
   */
  private static BigInteger md5Sum(File source) throws NoSuchAlgorithmException, IOException {
    MessageDigest md = MessageDigest.getInstance("MD5");
    md.reset();
    md.update(FileUtils.readFileToByteArray(source));
    byte[] md5sum = md.digest();
    return new BigInteger(1, md5sum);
  }

  /**
   * Reads the contents of a file. It expects the first line of the file to be a String representation of an MD5 sum in
   * hex format.
   * 
   * @param source The input file.
   * @return The value read as a BigInteger
   * @throws IOException If unable to process the input file for any reason.
   * @throws NumberFormatException If the first line of the file does not represent a BigInteger value.
   */
  private static BigInteger readMD5File(File source) throws IOException {
    String hexStr = StringUtils.trim(StringUtils.chomp(FileUtils.readFileToString(source, null)));
    return new BigInteger(hexStr, 16);
  }

  /**
   * Verify that the MD5 sum of the content of a file matches that of the MD5 sum value stored in another file.
   * <p>
   * It expects the first line of the file referenced by the md5file parameter to be a String representation of an MD5
   * sum in hex format.
   * 
   * @return True if matching.
   * @throws IOException If unable to process either the srcfile or the md5file
   * @throws NoSuchAlgorithmException If the MD5 cryptographic algorithm is requested but is not available in the
   *         environment.
   * @throws NumberFormatException If the first line of the md5file does not represent a BigInteger value.
   */
  public static boolean verifyMD5Sum(File srcFile, File md5File) throws NoSuchAlgorithmException, IOException {
    BigInteger actual = md5Sum(srcFile);
    BigInteger expected = readMD5File(md5File);
    return actual.equals(expected);
  }

  private ChecksumUtil() {
    // This is a utility class, we don't allow instances of it.
  }

}
