/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.util.regex.Matcher;

public class IBMJDKVersionTest extends TCTestCase {

  public void testPatternMatch() {
    Matcher serviceReleaseMatcher = VmVersion.IBM_SERVICE_RELEASE_PATTERN.matcher("pap6460-20070819_01");
    Assert.assertTrue(serviceReleaseMatcher.matches());

    serviceReleaseMatcher = VmVersion.IBM_SERVICE_RELEASE_PATTERN.matcher("pxa6460sr8-20100409_01(SR8)");
    Assert.assertTrue(serviceReleaseMatcher.matches());

    serviceReleaseMatcher = VmVersion.IBM_SERVICE_RELEASE_PATTERN.matcher("pxi32devifx-20100511a (SR11 FP2 )");
    Assert.assertTrue(serviceReleaseMatcher.matches());

    serviceReleaseMatcher = VmVersion.IBM_SERVICE_RELEASE_PATTERN.matcher("pxa6460sr8-20100409_01      (SR8)");
    Assert.assertTrue(serviceReleaseMatcher.matches());

    serviceReleaseMatcher = VmVersion.IBM_SERVICE_RELEASE_PATTERN.matcher("pxa64devifx-20100511a (SR11 FP2 )");
    Assert.assertTrue(serviceReleaseMatcher.matches());

    serviceReleaseMatcher = VmVersion.IBM_SERVICE_RELEASE_PATTERN.matcher("pxi3260sr8-20100409_01(SR8)");
    Assert.assertTrue(serviceReleaseMatcher.matches());

    serviceReleaseMatcher = VmVersion.IBM_SERVICE_RELEASE_PATTERN.matcher("pap32devifx-20070725 (SR5a)");
    Assert.assertTrue(serviceReleaseMatcher.matches());
  }
}
