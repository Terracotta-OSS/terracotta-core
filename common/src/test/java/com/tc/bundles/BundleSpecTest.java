/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import org.osgi.framework.BundleException;

import junit.framework.TestCase;

public class BundleSpecTest extends TestCase {

  public void testValidateBundleSpec() {
    BundleSpec[] reqs; 

    reqs = make("foo.bar.baz.widget");
    validate(reqs[0], false);
    
    reqs = make("foo.bar.baz.widget, foo.bar.baz.gadget");
    validate(reqs[0], false);
    validate(reqs[1], false);
    
    reqs = make("foo.bar.baz.widget;bundle-version:=1.0.0, foo.bar.baz.gadget");
    validate(reqs[0], true);
    validate(reqs[1], false);

    reqs = make("foo.bar.baz.widget;bundle-version:=1.0.0, foo.bar.baz.gadget;bundle-version:=1.0.0");
    validate(reqs[0], true);
    validate(reqs[1], true);

    reqs = make("foo.bar.baz.widget;bundle-version:=\"[1.0.0, 2.0.0]\"");
    validate(reqs[0], true);
    
    reqs = make("foo.bar.baz.widget;bundle-version:=\"[1.0.0,]\""); 
    validate(reqs[0], true);

    reqs = make("foo.bar.baz.widget;bundle-version:=\"(1.0.0,)\""); 
    validate(reqs[0], true);

    reqs = make("foo.bar.baz.widget;resolution:=optional"); 
    validate(reqs[0], false);
    
    reqs = make("org.terracotta.modules.clustered-cglib-2.1.3;bundle-version:=1.0.0");
    validate(reqs[0], true);
  }
  
  private void validate(BundleSpec spec, boolean expected) {
    boolean flag = true;
    try { 
      Resolver.validateBundleSpec(spec); 
    } catch (BundleException ex) {
      flag = false;
    }
    assertEquals(flag, expected);
  }

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

    reqs = check(1, "org.terracotta.modules.clustered-surefire-2.3;bundle-version:=1.0.0");
    assertEquals("org.terracotta.modules.clustered-surefire-2.3", reqs[0].getSymbolicName());
    assertEquals("org.terracotta.modules", reqs[0].getGroupId());
    assertEquals("clustered-surefire-2.3", reqs[0].getName());
    assertEquals("1.0.0", reqs[0].getVersion());
    
    reqs = check(1, "org.terracotta.modules.excludes-config;bundle-version:=1.0.0");
    assertEquals("org.terracotta.modules.excludes-config", reqs[0].getSymbolicName());
    assertEquals("org.terracotta.modules", reqs[0].getGroupId());
    assertEquals("excludes-config", reqs[0].getName());
    assertEquals("1.0.0", reqs[0].getVersion());
    
    reqs = check(1, "org.terracotta.modules.clustered-cglib-2.1.3;bundle-version:=1.0.0");
    assertEquals("org.terracotta.modules.clustered-cglib-2.1.3", reqs[0].getSymbolicName());
    assertEquals("org.terracotta.modules", reqs[0].getGroupId());
    assertEquals("clustered-cglib-2.1.3", reqs[0].getName());
    assertEquals("1.0.0", reqs[0].getVersion());
  }

  private BundleSpec[] check(int size, String source) {
    BundleSpec[] specs = make(source);
    assertEquals(size, specs.length);
    return specs;
  }

  private BundleSpec[] make(String source) {
    String[] requirements = BundleSpec.getRequirements(source);
    BundleSpec[] specs = new BundleSpec[requirements.length];
    for (int i = 0; i < requirements.length; i++) {
      specs[i] = BundleSpec.newInstance(requirements[i]);
    }
    return specs;
  }
  
  public void testVersionCheck() {
    BundleSpec spec = new BundleSpecImpl("org.terracotta.modules.modules-base;bundle-version:=\"[1.0.0.SNAPSHOT,1.1.0.SNAPSHOT)\"");
    assertEquals(true, spec.isVersionSpecified());
    assertEquals(false, spec.isVersionSpecifiedAbsolute());
    assertEquals(true, spec.isCompatible("org.terracotta.modules.modules-base", "1.0.0.SNAPSHOT"));
  }
}
