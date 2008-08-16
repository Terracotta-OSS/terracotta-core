/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassReader;
import com.tc.asm.tree.ClassNode;
import com.tc.util.runtime.Vm;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 *
 */
public class OverridesHashCodeAdapterTest extends TestCase {

  private static final String OVERRIDES_HASHCODE_INTERFACE = OverridesHashCode.class.getName().replace('.', '/');

  public void testEnum() throws Exception {
    if (!Vm.isJDK15Compliant()) { return; }

    String className = "java.lang.Enum";

    assertDoesNotHaveOverridesHashCodeInterface(className);

    List<String> originalInterfaces = new ArrayList();
    for (Class c : Enum.class.getInterfaces()) {
      originalInterfaces.add(c.getName().replace('.', '/'));
    }

    List newInterfaces = instrumentAndGetInterfaces(className);

    if (!originalInterfaces.equals(newInterfaces)) { throw new AssertionError("interfaces not equals, "
                                                                              + originalInterfaces + " != "
                                                                              + newInterfaces); }
  }

  public void testDoesOverride() throws Exception {
    String className = DoesOverride.class.getName();
    assertDoesNotHaveOverridesHashCodeInterface(className);
    List newInterfaces = instrumentAndGetInterfaces(className);
    assertTrue(newInterfaces.toString(), newInterfaces.contains(OVERRIDES_HASHCODE_INTERFACE));
  }

  public void testDoesNotOverride() throws Exception {
    String className = DoesNotOverride.class.getName();
    assertDoesNotHaveOverridesHashCodeInterface(className);
    List newInterfaces = instrumentAndGetInterfaces(className);
    assertFalse(newInterfaces.toString(), newInterfaces.contains(OVERRIDES_HASHCODE_INTERFACE));
  }

  public void testObject() throws Exception {
    String className = "java.lang.Object";
    assertDoesNotHaveOverridesHashCodeInterface(className);

    List newInterfaces = instrumentAndGetInterfaces(className);

    assertFalse(newInterfaces.toString(), newInterfaces.contains(OVERRIDES_HASHCODE_INTERFACE));
  }

  private List instrumentAndGetInterfaces(String className) throws Exception {
    ClassReader cr = new ClassReader(className);
    ClassNode cn = new ClassNode();
    cr.accept(new OverridesHashCodeAdapter(cn), ClassReader.SKIP_FRAMES);

    List newInterfaces = new ArrayList();
    for (Object iface : cn.interfaces) {
      if (!newInterfaces.contains(iface)) {
        newInterfaces.add(iface);
      }
    }

    return newInterfaces;
  }

  private void assertDoesNotHaveOverridesHashCodeInterface(String className) throws Exception {
    List<String> interfaces = getInterfaces(className);

    if (interfaces.contains(OVERRIDES_HASHCODE_INTERFACE)) {
      //
      throw new AssertionError(className + " has interfaces " + interfaces);
    }
  }

  /**
   * Get the interfaces for the actual named class in this VM
   */
  private List<String> getInterfaces(String className) throws Exception {
    ClassReader cr = new ClassReader(className);

    ClassNode cn = new ClassNode();
    cr.accept(cn, ClassReader.SKIP_FRAMES);

    return cn.interfaces;
  }

  private static class DoesOverride {
    @Override
    public int hashCode() {
      return 2;
    }
  }

  private static class DoesNotOverride {
    //
  }

}
