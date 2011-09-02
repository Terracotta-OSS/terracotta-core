/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
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
  public static BigInteger md5Sum(File source) throws NoSuchAlgorithmException, IOException {
    MessageDigest md = MessageDigest.getInstance("MD5");
    md.reset();
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(source);
      int count = -1;
      byte[] buffer = new byte[4 * 1024];
      while ((count = fis.read(buffer)) != -1) {
        md.update(buffer, 0, count);
      }
    } finally {
      IOUtils.closeQuietly(fis);
    }
    byte[] md5sum = md.digest();
    return new BigInteger(1, md5sum);
  }

  /**
   * Reads the contents of a file. It expects the first line of the file to be a String representation of an MD5 sum in
   * hex format. It will also recognize the default repot format used by the md5sum tool, so the following are
   * considered as valid entries in an md5 file: <code>
   *    950b176dafe16b89cbb3dc3812a70e4a
   *    950b176dafe16b89cbb3dc3812a70e4a  testChecksumData.txt
   * </code>
   * 
   * @param source The input file.
   * @return The value read as a BigInteger
   * @throws IOException If unable to process the input file for any reason.
   * @throws NumberFormatException If the first line of the file does not represent a BigInteger value.
   */
  private static BigInteger readMD5File(File source) throws IOException {
    String data = StringUtils.trim(StringUtils.chomp(FileUtils.readFileToString(source, null))).replaceAll(" .+$", "");
    return new BigInteger(data, 16);
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
