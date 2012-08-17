/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import com.tc.test.TCTestCase;
import com.terracotta.toolkit.express.URLConfigUtil;

import junit.framework.Assert;

public class URLConfigTranslationTest extends TCTestCase {

  public void testBasic() {

    Assert.assertEquals("localhost:9510", URLConfigUtil.translateSystemProperties("localhost:9510"));

    System.setProperty("tc.active1", "active1");
    System.setProperty("tc.active2", "active2");
    System.setProperty("tc.passive1", "passive1");
    System.setProperty("tc.passive2", "passive2");

    Assert.assertEquals("active1, active2, passive1, passive2", URLConfigUtil
        .translateSystemProperties("${tc.active1}, ${tc.active2}, ${tc.passive1}, ${tc.passive2}"));
    Assert.assertEquals("active1, active2, passive1, passive2",
                        URLConfigUtil.translateSystemProperties("${tc.active1}, active2, ${tc.passive1}, passive2"));
    Assert.assertEquals("active1-active1, active2, passive1-passive1, passive2", URLConfigUtil
        .translateSystemProperties("${tc.active1}-${tc.active1}, active2, ${tc.passive1}-${tc.passive1}, passive2"));

    // no replacements
    Assert.assertEquals("${}, ${no-sys-prop-active2}, ${, $",
                        URLConfigUtil.translateSystemProperties("${}, ${no-sys-prop-active2}, ${,  $"));
    Assert.assertEquals("", URLConfigUtil.translateSystemProperties("   "));
    System.out.println("XXX Test success");
  }

}
