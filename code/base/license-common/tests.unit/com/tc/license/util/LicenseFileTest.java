/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.license.util;

import org.apache.commons.io.IOUtils;

import com.tc.license.Capability;
import com.tc.license.License;
import com.tc.license.LicenseFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import junit.framework.TestCase;

public class LicenseFileTest extends TestCase {

  public void testStore() throws Exception {
    File tempLicenseFile = File.createTempFile("license", ".key");
    tempLicenseFile.deleteOnExit();
    FileOutputStream out = new FileOutputStream(tempLicenseFile);

    License license = LicenseFactory.createEnterpriseLicense("Commercial", "123", "Big ole company", "EX", "4",
                                                             "2009-02-03");
    license.setSignature("DIGITIALLY SIGNED");

    try {
      LicenseStore licenseStore = new LicenseFile();
      licenseStore.store(license, out);
      out.close();
    } finally {
      IOUtils.closeQuietly(out);
    }

    FileReader reader = null;
    List actualLines = null;
    try {
      reader = new FileReader(tempLicenseFile);
      actualLines = IOUtils.readLines(reader);
      reader.close();
    } finally {
      IOUtils.closeQuietly(reader);
    }

    List expectedLines = IOUtils.readLines(getClass().getResourceAsStream("/com/tc/license/util/LicenseFileTest.txt"));

    assertEquals(expectedLines, actualLines);
  }

  public void testLoad() throws Exception {
    LicenseStore licenseStore = new LicenseFile();
    License license = licenseStore.load(getClass().getResourceAsStream("/com/tc/license/util/LicenseFileTest.txt"));
    assertEquals("Commercial", license.licenseType());
    assertEquals("123", license.licenseNumber());
    assertEquals("Big ole company", license.licensee());
    assertEquals("EX", license.product());
    assertEquals(4, license.maxClients());
    assertEquals(new SimpleDateFormat(LicenseConstants.DATE_FORMAT).parse("2009-02-03"), license.expirationDate());
    assertEquals("DIGITIALLY SIGNED", license.getSignature());
    assertEquals(2, license.capabilities().licensedCapabilitiesCount());
    assertTrue(license.capabilities().isLicensed(Capability.ROOTS));
    assertTrue(license.capabilities().isLicensed(Capability.SESSIONS));
  }

  public void testMissingMarkings() throws IOException {
    LicenseStore licenseStore = new LicenseFile();
    try {
      licenseStore.load(getClass().getResourceAsStream("/com/tc/license/util/missingMarkings.txt"));
      fail("Should have thrown LicenseException");
    } catch (LicenseException e) {
      System.out.println("expected exception: " + e.getMessage());
    } catch (IOException e) {
      throw e;
    }
  }
}
