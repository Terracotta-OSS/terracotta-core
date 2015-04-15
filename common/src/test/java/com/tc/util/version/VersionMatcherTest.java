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
package com.tc.util.version;

import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.terracotta.test.categories.CheckShorts;

import com.tc.test.TCTestCase;

@Category(CheckShorts.class)
public class VersionMatcherTest extends TCTestCase {

  public void testExactMatch() {
    VersionMatcher matcher = new VersionMatcher("3.0.0");
    Assert.assertTrue(matcher.matches("3.0.0"));
  }

  public void testExactMisMatch() {
    VersionMatcher matcher = new VersionMatcher("3.0.0");
    Assert.assertFalse(matcher.matches("9.9.9"));
  }

  public void testTcAny() {
    VersionMatcher matcher = new VersionMatcher("3.0.0");
    Assert.assertTrue(matcher.matches("*"));
  }

}
