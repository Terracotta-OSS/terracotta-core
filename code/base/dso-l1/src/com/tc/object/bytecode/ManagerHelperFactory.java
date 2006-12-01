/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A helper class to assist in adding Manager method calls into instrumented classes <br>
 * <br>
 * Actually, this class could probably be generalized as some form of interface caller for instrumented classes
 */
public class ManagerHelperFactory {
  private final Class managerUtilClass;
  private final Map   methods;

  public ManagerHelperFactory() {
    this.managerUtilClass = ManagerUtil.class;
    this.methods = findMethods();
  }

  private Map findMethods() {
    Map map = new HashMap();

    Method[] mgrUtilMethods = this.managerUtilClass.getDeclaredMethods();

    for (int i = 0; i < mgrUtilMethods.length; i++) {
      Method m = mgrUtilMethods[i];
      String name = m.getName();
      MethodDetail md = new MethodDetail(m);

      if (map.containsKey(name)) {
        throw new RuntimeException("Duplicated method name [" + name + "] on interface " + managerUtilClass.getName());
      } else {
        map.put(name, md);
      }
    }

    return Collections.unmodifiableMap(map);
  }

  public ManagerHelper createHelper() {
    return new ManagerHelperImpl();
  }

  private static class MethodDetail implements Opcodes {
    final String  name;
    final String  desc;
    final boolean takesManagerAsAnArg;

    MethodDetail(Method m) {
      desc = Type.getMethodDescriptor(m);
      name = m.getName();
      takesManagerAsAnArg = hasManagerClass(m.getParameterTypes());
    }

    private boolean hasManagerClass(Class[] parameterTypes) {
      if (parameterTypes == null) return false;
      for (int i = 0; i < parameterTypes.length; i++) {
        if (parameterTypes[i].getName().equals(Manager.class.getName())) { return true; }
      }
      return false;
    }

  }

  private class ManagerHelperImpl implements ManagerHelper, Opcodes {
    public ManagerHelperImpl() {
      //
    }

    public void callManagerMethod(String name, MethodVisitor mv) {
      MethodDetail md = findMethodDetail(name);
      callMethod(md, mv);
    }

    private MethodDetail findMethodDetail(String name) {
      MethodDetail md = (MethodDetail) methods.get(name);
      if (md == null) {
        // make formatter work
        throw new RuntimeException("No such method [" + name + "] on manager class: " + managerUtilClass.getName());
      }
      return md;
    }

    private void callMethod(MethodDetail md, MethodVisitor mv) {
      // TODO: We can easily optimize for manager methods that take 0/1/2? arguments. Provided that a few swap
      // instructions are faster than an invokestatic
      mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, md.name, md.desc);
    }

  }

}