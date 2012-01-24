/*
 * All content copyright (c) 2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package com.tc.object.config;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.object.BaseDSOTestCase;

public class IncludeExcludeOrderingTest extends BaseDSOTestCase {

  /**
   * Verify that relative order of includes and excludes is preserved: a specific include placed after a more general
   * exclude will win, and similarly a specific exclude will win after a general include.
   */
  public void testMoreSpecificOrdering() throws ConfigurationSetupException {
    DSOClientConfigHelper config = createClientConfigHelper();

    config.addIncludePattern("p.*");
    config.addExcludePattern("p.q.*");
    config.addIncludePattern("p.q.C");
    config.addIncludePattern("p.q.r.*");
    config.addExcludePattern("p.q.r.D");

    System.out.println("The following warnings about unloadable classes [p/A*] are expected.");
    ClassInfo classInfoA = AsmClassInfo.getClassInfo("p.A", getClass().getClassLoader());
    assertTrue(config.shouldBeAdapted(classInfoA));

    ClassInfo classInfoB = AsmClassInfo.getClassInfo("p.q.B", getClass().getClassLoader());
    assertFalse(config.shouldBeAdapted(classInfoB));

    ClassInfo classInfoC = AsmClassInfo.getClassInfo("p.q.C", getClass().getClassLoader());
    assertTrue(config.shouldBeAdapted(classInfoC));

    ClassInfo classInfoD = AsmClassInfo.getClassInfo("p.q.r.D", getClass().getClassLoader());
    assertFalse(config.shouldBeAdapted(classInfoD));

    ClassInfo classInfoE = AsmClassInfo.getClassInfo("p.q.r.E", getClass().getClassLoader());
    assertTrue(config.shouldBeAdapted(classInfoE));
  }

  /**
   * Verify that relative order of includes and excludes is preserved: a more general include placed after a more
   * specific exclude will win, and similarly a more general exclude after a specific include. Note that this sort of
   * content in a tc-config.xml is not very useful, and indeed at some point we might want to issue warnings.
   */
  public void testMoreGeneralOrdering() throws ConfigurationSetupException {
    DSOClientConfigHelper config = createClientConfigHelper();

    config.addIncludePattern("A*");
    config.addExcludePattern("ABC");
    config.addIncludePattern("AB*"); // this include overrides the previous exclude
    config.addIncludePattern("ZY");
    config.addExcludePattern("Z*"); // this exclude overrides the previous include

    System.out.println("The following warnings about unloadable classes [A*] and [Z*] are expected.");
    ClassInfo classInfoA = AsmClassInfo.getClassInfo("A", getClass().getClassLoader());
    assertTrue(config.shouldBeAdapted(classInfoA));

    ClassInfo classInfoC = AsmClassInfo.getClassInfo("ABC", getClass().getClassLoader());
    assertTrue(config.shouldBeAdapted(classInfoC));

    ClassInfo classInfoZ = AsmClassInfo.getClassInfo("Z", getClass().getClassLoader());
    assertFalse(config.shouldBeAdapted(classInfoZ));

    ClassInfo classInfoY = AsmClassInfo.getClassInfo("ZY", getClass().getClassLoader());
    assertFalse(config.shouldBeAdapted(classInfoY));
  }
}
