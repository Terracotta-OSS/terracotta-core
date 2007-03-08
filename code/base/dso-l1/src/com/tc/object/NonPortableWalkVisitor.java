/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.aspectwerkz.reflect.impl.java.JavaClassInfo;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.walker.MemberValue;
import com.tc.object.walker.PrintVisitor;
import com.tc.object.walker.Visitor;
import com.tc.object.walker.WalkTest;
import com.tc.object.walker.PrintVisitor.ValueFormatter;

import java.io.PrintStream;
import java.lang.reflect.Field;

public class NonPortableWalkVisitor implements Visitor, ValueFormatter, WalkTest {

  public static final String          MARKER       = "!!";
  private static final String         NON_PORTABLE = MARKER + " ";
  private static final String         PORTABLE     = spaces(NON_PORTABLE.length());
  private static final LiteralValues  literals     = new LiteralValues();

  private final PrintVisitor          delegate;
  private final ClientObjectManager   objMgr;
  private final PrintStream           out;
  private final DSOClientConfigHelper config;

  public NonPortableWalkVisitor(PrintStream out, ClientObjectManager objMgr, DSOClientConfigHelper config) {
    this.out = out;
    this.objMgr = objMgr;
    this.config = config;
    delegate = new PrintVisitor(out, this, this);
  }

  private static String spaces(int n) {
    String s = "";
    for (int i = 0; i < n; i++) {
      s += " ";
    }
    return s;
  }

  public void visitMapEntry(int index, int depth) {
    out.print(PORTABLE);
    delegate.visitMapEntry(index, depth);
  }

  public void visitRootObject(MemberValue value) {
    indicatePortability(value);
    delegate.visitRootObject(value);
  }

  public void visitValue(MemberValue value, int depth) {
    if (skipVisit(value.getSourceField())) { return; }
    indicatePortability(value);
    delegate.visitValue(value, depth);
  }

  public String format(Object value) {
    if (value == null) { return "null"; }

    int type = literals.valueFor(value);
    switch (type) {
      case LiteralValues.OBJECT: {
        return "(" + value.getClass().getName() + ")";
      }
      case LiteralValues.JAVA_LANG_CLASSLOADER: {
        return "Classloader (" + value.getClass().getName() + ")";
      }
      case LiteralValues.STRING: {
        return "\"" + value + "\"";
      }
      default: {
        return String.valueOf(value);
      }
    }
  }

  public String valueAdornment(MemberValue value) {
    if (isTransient(value)) { return " (transient)"; }

    Object o = value.getValueObject();
    if (o != null && config.isNeverAdaptable(JavaClassInfo.getClassInfo(o.getClass()))) { return " (never portable)"; }

    return null;
  }

  public boolean shouldTraverse(MemberValue val) {
    if (literals.isLiteralInstance(val.getValueObject())) { return false; }
    if (isTransient(val)) { return false; }

    Object o = val.getValueObject();
    if (o != null && config.isNeverAdaptable(JavaClassInfo.getClassInfo(o.getClass()))) { return false; }

    return true;
  }

  private boolean isTransient(MemberValue val) {
    Field f = val.getSourceField();
    if (f == null) { return false; }

    TransparencyClassSpec spec = config.getSpec(f.getDeclaringClass().getName());
    if (spec != null) { return spec.isTransient(f.getModifiers(), f.getName()); }

    return false;
  }

  private static boolean skipVisit(Field field) {
    return (field != null) && field.getType().getName().startsWith("com.tc.");
  }

  private void indicatePortability(MemberValue value) {
    if (objMgr.isPortableInstance(value.getValueObject())) {
      out.print(PORTABLE);
    } else {
      out.print(NON_PORTABLE);
    }
  }

}
