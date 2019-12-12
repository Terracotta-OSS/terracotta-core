/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.tripwire;

import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class JFRAppenderTest {
  
  @Test
  public void testRegex() throws Exception {
    Pattern p = Pattern.compile("Server dump:|dump on exit");
    Assert.assertFalse(p.matcher("Server dump: this is a test").matches());
    Assert.assertFalse( p.matcher("dump on exit   this is a test").matches());
    Assert.assertFalse(p.matcher("this is a test").matches());
    Assert.assertTrue(p.matcher("Server dump: this is a test").find());
    Assert.assertTrue(p.matcher("dump on exit   this is a test").find());
    Assert.assertFalse(p.matcher("this is a test").find());
  }
}
