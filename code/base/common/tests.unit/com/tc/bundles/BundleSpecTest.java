/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import junit.framework.TestCase;

public class BundleSpecTest extends TestCase {

  public void testGetRequirementsString() {
    BundleSpec[] reqs; 

    reqs = check(1, "foo.bar.baz.widget");
    reqs = check(2, "foo.bar.baz.widget, foo.bar.baz.gadget");
    reqs = check(1, "foo.bar.baz.widget; bundle-version:=1.0.0 ");
    
    reqs = check(1, "foo.bar.baz.widget;bundle-version:=1.0.0");
    assertEquals("1.0.0", reqs[0].getVersion());

    reqs = check(1, "foo.bar.baz.widget;bundle-version:=\"1.0.0\"");
    assertEquals("1.0.0", reqs[0].getVersion());

    reqs = check(1, "foo.bar.baz.widget;bundle-version:=\"[1.0.0, 2.0.0]\"");
    reqs = check(1, "foo.bar.baz.widget;bundle-version:=\"[1.0.0, 2.0.0)\"");
    reqs = check(1, "foo.bar.baz.widget;bundle-version:=\"(1.0.0, 2.0.0)\"");
    reqs = check(1, "foo.bar.baz.widget;bundle-version:=\"(1.0.0, 2.0.0]\"");
    reqs = check(1, "foo.bar.baz.widget;bundle-version:=\"[1.0.0,]\""); 
    reqs = check(1, "foo.bar.baz.widget;bundle-version:=\"(1.0.0,)\""); 
    reqs = check(1, "foo.bar.baz.widget;resolution:=optional"); 

    reqs = check(1, "org.terracotta.modules.clustered_surefire_2.3;bundle-version:=2.6.0.SNAPSHOT");
    assertEquals("org.terracotta.modules.clustered_surefire_2.3", reqs[0].getSymbolicName());
    assertEquals("org.terracotta.modules", reqs[0].getGroupId());
    assertEquals("clustered_surefire_2.3", reqs[0].getName());
    assertEquals("2.6.0.SNAPSHOT", reqs[0].getVersion());
    
    reqs = check(1, "org.terracotta.modules.excludes_config;bundle-version:=2.6.0.SNAPSHOT");
    assertEquals("org.terracotta.modules.excludes_config", reqs[0].getSymbolicName());
    assertEquals("org.terracotta.modules", reqs[0].getGroupId());
    assertEquals("excludes_config", reqs[0].getName());
    assertEquals("2.6.0.SNAPSHOT", reqs[0].getVersion());
    
    reqs = check(1, "org.terracotta.modules.clustered_cglib_2.1.3;bundle-version:=2.6.0.SNAPSHOT");
    assertEquals("org.terracotta.modules.clustered_cglib_2.1.3", reqs[0].getSymbolicName());
    assertEquals("org.terracotta.modules", reqs[0].getGroupId());
    assertEquals("clustered_cglib_2.1.3", reqs[0].getName());
    assertEquals("2.6.0.SNAPSHOT", reqs[0].getVersion());
  }

  private BundleSpec[] check(int size, String source) {
    String[] requirements = BundleSpec.getRequirements(source);
    assertEquals(size, requirements.length);
    
    BundleSpec[] specs = new BundleSpec[requirements.length];
    for (int i = 0; i < requirements.length; i++) {
      specs[i] = BundleSpec.newInstance(requirements[i]);
    }
    
    return specs;
  }

}
