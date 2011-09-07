/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

import junit.framework.TestCase;

public class ProductInfoTest extends TestCase {

  public void testOpenSourceEditionWithPatch() {
    try {
      InputStream buildData = ProductInfo.getData("TestBuildData.txt");
      InputStream patchData = ProductInfo.getData("TestPatchData.txt");
      ProductInfo info = new ProductInfo(buildData, patchData);
      verifyOpenSourceBuildData(info);
      verifyPatchInfo(info);
      assertEquals("20080620-235959 (Revision 12112 by thepatchuser@thepatchhost from thepatchbranch)",
                   info.patchBuildID());
      assertEquals("Patch Level 5, as of 20080620-235959 (Revision 12112 by thepatchuser@thepatchhost from thepatchbranch)",
                   info.toLongPatchString());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  public void testOpenSourceEditionWithoutPatch() {
    try {
      InputStream buildData = ProductInfo.getData("TestBuildData.txt");
      ProductInfo info = new ProductInfo(buildData, null);
      verifyOpenSourceBuildData(info);
      verifyNoPatchInfo(info);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  public void testEnterpriseEditionWithPatch() {
    try {
      InputStream buildData = ProductInfo.getData("TestEnterpriseBuildData.txt");
      InputStream patchData = ProductInfo.getData("TestPatchData.txt");
      ProductInfo info = new ProductInfo(buildData, patchData);
      verifyEnterpriseBuildData(info);
      verifyPatchInfo(info);
      assertEquals("20080620-235959 (Revision 12112 by thepatchuser@thepatchhost from thepatchbranch)",
                   info.patchBuildID());
      assertEquals("Patch Level 5, as of 20080620-235959 (Revision 12112 by thepatchuser@thepatchhost from thepatchbranch)",
                   info.toLongPatchString());

    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  private void verifyOpenSourceBuildData(ProductInfo info) {
    assertEquals("thebranch", info.buildBranch());
    assertEquals("thehost", info.buildHost());
    assertEquals("20080616-130651 (Revision 12345 by theuser@thehost from thebranch)", info.buildID());
    assertEquals("12345", info.buildRevision());
    assertEquals(ProductInfo.UNKNOWN_VALUE, info.buildRevisionFromEE());
    assertEquals(toDate(2008, 5, 16, 13, 6, 51), info.buildTimestamp());
    assertEquals("20080616-130651", info.buildTimestampAsString());
    assertEquals("theuser", info.buildUser());

    String copyright = info.copyright();
    assertNotNull(copyright);
    assertTrue(copyright.indexOf("Copyright (c)") >= 0);
    assertTrue(copyright.indexOf("Terracotta, Inc.") >= 0);
    assertTrue(copyright.indexOf("All rights reserved.") >= 0);

    assertEquals("Opensource", info.edition());
    assertTrue(info.isOpenSource());
    assertFalse(info.isEnterprise());
    assertEquals("1.2.3", info.kitID());
    assertEquals("Unlimited development", info.license());
    assertEquals("1.2.3-SNAPSHOT", info.mavenArtifactsVersion());
    assertEquals("Terracotta", info.moniker());
    System.out.println(info.toLongString());
    assertEquals("Terracotta 1.2.3-SNAPSHOT, as of 20080616-130651 (Revision 12345 by theuser@thehost from thebranch)",
                 info.toLongString());
    assertEquals("Terracotta 1.2.3-SNAPSHOT", info.toShortString());
    assertEquals("1.2.3-SNAPSHOT", info.version());
  }

  private void verifyEnterpriseBuildData(ProductInfo info) {
    assertEquals("thebranch", info.buildBranch());
    assertEquals("thehost", info.buildHost());
    assertEquals("20080616-130651 (Revision 12345 by theuser@thehost from thebranch)", info.buildID());
    assertEquals("12345", info.buildRevision());
    assertEquals("98765", info.buildRevisionFromEE());
    assertEquals("20080616-130651", info.buildTimestamp());
    assertEquals("20080616-130651", info.buildTimestampAsString());
    assertEquals("theuser", info.buildUser());

    String copyright = info.copyright();
    assertNotNull(copyright);
    assertTrue(copyright.indexOf("Copyright (c)") >= 0);
    assertTrue(copyright.indexOf("Terracotta, Inc.") >= 0);
    assertTrue(copyright.indexOf("All rights reserved.") >= 0);

    assertEquals("Opensource", info.edition());
    assertFalse(info.isEnterprise());
    assertTrue(info.isOpenSource());
    assertEquals("1.2.3", info.kitID());
    assertEquals("Unlimited development", info.license());
    assertEquals("1.2.3-SNAPSHOT", info.mavenArtifactsVersion());
    assertEquals("Terracotta", info.moniker());
    assertEquals("Terracotta 1.2.3-SNAPSHOT, as of 20080616-130651 (Revision 12345 by theuser@thehost from thebranch)",
                 info.toLongString());
    assertEquals("Terracotta 1.2.3-SNAPSHOT", info.toShortString());
    assertEquals("1.2.3-SNAPSHOT", info.version());
  }

  private void verifyPatchInfo(ProductInfo info) {
    assertEquals(true, info.isPatched());
    assertEquals("thepatchbranch", info.patchBranch());
    assertEquals("thepatchhost", info.patchHost());
    assertEquals("5", info.patchLevel());
    assertEquals("12112", info.patchRevision());
    assertEquals("9999", info.patchEERevision());
    assertEquals(toDate(2008, 5, 20, 23, 59, 59), info.patchTimestamp());
    assertEquals("20080620-235959", info.patchTimestampAsString());
    assertEquals("thepatchuser", info.patchUser());
    assertEquals("Patch Level 5", info.toShortPatchString());
  }

  private void verifyNoPatchInfo(ProductInfo info) {
    assertEquals(false, info.isPatched());
    assertEquals(ProductInfo.UNKNOWN_VALUE, info.patchBranch());
    assertEquals("[unknown] (Revision [unknown] by [unknown]@[unknown] from [unknown])", info.patchBuildID());
    assertEquals(ProductInfo.UNKNOWN_VALUE, info.patchHost());
    assertEquals(ProductInfo.UNKNOWN_VALUE, info.patchLevel());
    assertEquals(ProductInfo.UNKNOWN_VALUE, info.patchRevision());
    assertEquals(null, info.patchTimestamp());
    assertEquals(ProductInfo.UNKNOWN_VALUE, info.patchTimestampAsString());
    assertEquals(ProductInfo.UNKNOWN_VALUE, info.patchUser());
    assertEquals(ProductInfo.UNKNOWN_VALUE, info.patchEERevision());
    assertEquals("Patch Level [unknown], as of [unknown] (Revision [unknown] by [unknown]@[unknown] from [unknown])",
                 info.toLongPatchString());
    assertEquals("Patch Level [unknown]", info.toShortPatchString());
  }

  private Date toDate(int year, int month, int day, int hour, int min, int sec) {
    Calendar cal = Calendar.getInstance();
    cal.set(year, month, day, hour, min, sec);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTime();
  }
}
