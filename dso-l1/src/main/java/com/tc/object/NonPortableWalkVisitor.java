/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.aspectwerkz.reflect.impl.java.JavaClassInfo;
import com.tc.logging.TCLogger;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.walker.MemberValue;
import com.tc.object.walker.PrintVisitor;
import com.tc.object.walker.PrintVisitor.OutputSink;
import com.tc.object.walker.PrintVisitor.ValueFormatter;
import com.tc.object.walker.Visitor;
import com.tc.object.walker.WalkTest;

import java.lang.reflect.Field;

public class NonPortableWalkVisitor implements Visitor, ValueFormatter, WalkTest, OutputSink {

  public static final String          MARKER       = "!!";
  private static final String         NON_PORTABLE = MARKER + " ";
  private static final String         PORTABLE     = spaces(NON_PORTABLE.length());

  private final PrintVisitor          delegate;
  private final ClientObjectManager   objMgr;
  private final DSOClientConfigHelper config;
  private final TCLogger              logger;
  private StringBuffer                buffer       = new StringBuffer();

  public NonPortableWalkVisitor(TCLogger logger, ClientObjectManager objMgr, DSOClientConfigHelper config, Object root) {
    this.logger = logger;
    this.objMgr = objMgr;
    this.config = config;
    delegate = new PrintVisitor(this, this, this);

    logger.warn("Dumping object graph of non-portable instance of type " + root.getClass().getName()
                + ". Lines that start with " + NonPortableWalkVisitor.MARKER + " are non-portable types.");
  }

  public void output(String line) {
    logger.warn(buffer.toString() + line);
    buffer = new StringBuffer();
  }

  private static String spaces(int n) {
    StringBuilder sb = new StringBuilder(n);
    for (int i = 0; i < n; i++) {
      sb.append(" ");
    }
    return sb.toString();
  }

  public void visitMapEntry(int index, int depth) {
    buffer.append(PORTABLE);
    delegate.visitMapEntry(index, depth);
  }

  public void visitRootObject(MemberValue value) {
    indicatePortability(value);
    delegate.visitRootObject(value);
  }

  public void visitValue(MemberValue value, int depth) {
    if (skipVisit(value)) { return; }
    indicatePortability(value);
    delegate.visitValue(value, depth);
  }

  public String format(Object value) {
    if (value == null) { return "null"; }

    LiteralValues type = LiteralValues.valueFor(value);
    switch (type) {
      case OBJECT: {
        return "(" + value.getClass().getName() + ")";
      }
      case STRING: {
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

  private boolean isNeverAdaptable(Class type) {
    while (!type.equals(Object.class)) {
      if (config.isNeverAdaptable(JavaClassInfo.getClassInfo(type))) return true;
      type = type.getSuperclass();
    }
    return false;
  }

  private boolean isNeverAdaptable(MemberValue value) {
    Object o = value.getValueObject();
    if (o != null) return isNeverAdaptable(o.getClass());
    return false;
  }

  private boolean isPortable(MemberValue value) {
    Object valueObject = value.getValueObject();
    if (valueObject != null) return objMgr.isPortableInstance(valueObject);
    return true;
  }

  private boolean isSystemType(MemberValue value) {
    Object o = value.getValueObject();
    if (o != null) {
      return (o.getClass().getClassLoader() == null);
    } else {
      Field field = value.getSourceField();
      if (field != null) return (field.getType().getClassLoader() == null);
    }
    return false;
  }

  public boolean shouldTraverse(MemberValue value) {
    if (value.isRepeated()) { return true; }

    if (skipVisit(value) || isNeverAdaptable(value) || isTransient(value)) { return false; }

    if (!isPortable(value) && isSystemType(value)) return false;

    Field field = value.getSourceField();
    if (field != null) {
      Class type = field.getType();
      if (type.isArray() && type.getComponentType().isPrimitive()) { return false; }
    }

    return !isLiteralInstance(value.getValueObject());
  }

  private boolean isLiteralInstance(Object obj) {
    return LiteralValues.isLiteralInstance(obj);
  }

  private boolean isTransient(MemberValue val) {
    Field f = val.getSourceField();
    if (f == null) { return false; }

    return config.isTransient(f.getModifiers(), JavaClassInfo.getClassInfo(f.getDeclaringClass()), f.getName());
  }

  public boolean includeFieldsForType(Class type) {
    return !config.isLogical(type.getName());
  }

  private boolean skipVisit(MemberValue value) {
    Field field = value.getSourceField();
    if (field != null) { return (field.getType().getName().startsWith("com.tc.")); }
    return false;
  }

  private void indicatePortability(MemberValue value) {
    if (objMgr.isPortableInstance(value.getValueObject())) {
      buffer.append(PORTABLE);
    } else {
      buffer.append(NON_PORTABLE);
    }
  }

}
