/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
