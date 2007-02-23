/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.walker;

import com.tc.util.ClassUtils;

import java.io.PrintStream;
import java.lang.reflect.Field;

/**
 * Produces a textual representation of the type visited
 */
public class PrintVisitor implements Visitor {
  private static final String  INDENT = "  ";
  private final PrintStream    out;
  private final WalkTest       walkTest;
  private final ValueFormatter formatter;

  public PrintVisitor(PrintStream out, WalkTest walkTest, ValueFormatter formatter) {
    this.out = out;
    this.walkTest = walkTest;
    this.formatter = formatter;
  }

  public void visitRootObject(MemberValue value) {
    out.println(typeDisplay(value.getValueObject().getClass()) + " (id " + value.getId() + ")");
  }

  public void visitValue(MemberValue value, int depth) {
    boolean isLiteral = !walkTest.shouldTraverse(value);

    indent(depth);

    if (value.isMapKey()) {
      out.print("key ");
    } else if (value.isMapValue()) {
      out.print("value ");
    }

    Field field = value.getSourceField();
    if (field != null) {
      out.print(typeDisplay(field.getType()) + " ");
      boolean shadowed = value.isShadowed();
      if (shadowed) {
        out.print(field.getDeclaringClass().getName() + ".");
      }

      out.print(field.getName() + " ");
    }

    if (value.isElement()) {
      out.print("[" + value.getIndex() + "] ");
    }

    out.print("= ");

    Object o = value.getValueObject();

    if (isLiteral || o == null) {
      out.print(formatter.format(value.getValueObject()));
    } else {
      if (value.isRepeated()) {
        out.print("(ref id " + value.getId() + ")");
      } else {
        if ((field != null) && (o.getClass().equals(field.getType()))) {
          out.print("(id " + value.getId() + ")");
        } else {
          out.print("(" + typeDisplay(o.getClass()) + ", id " + value.getId() + ")");
        }
      }
    }

    String adorn = formatter.valueAdornment(value);
    if (adorn != null) {
      out.print(adorn);
    }
    out.println();

  }

  private static final String JAVA_LANG     = "java.lang.";
  private static final int    JAVA_LANG_LEN = JAVA_LANG.length();

  private static final String JAVA_UTIL     = "java.util.";
  private static final int    JAVA_UTIL_LEN = JAVA_UTIL.length();

  private static String typeDisplay(Class c) {
    String type = c.getName();
    int dim = 0;
    if (c.isArray()) {
      dim = ClassUtils.arrayDimensions(c);
      type = ClassUtils.baseComponetType(c).getName();
    }

    if (type.startsWith(JAVA_LANG) && type.lastIndexOf('.') + 1 == JAVA_LANG_LEN) {
      type = type.substring(JAVA_LANG_LEN);
    } else if (type.startsWith(JAVA_UTIL) && type.lastIndexOf('.') + 1 == JAVA_UTIL_LEN) {
      type = type.substring(JAVA_UTIL_LEN);
    }

    for (int i = 0; i < dim; i++) {
      type = type + "[]";
    }

    return type;
  }

  public void visitMapEntry(int index, int depth) {
    indent(depth);
    out.println("[entry " + index + "]");
  }

  private void indent(int currentDepth) {
    while (currentDepth-- > 0) {
      out.print(INDENT);
    }
  }

  public interface ValueFormatter {
    String format(Object value);

    String valueAdornment(MemberValue value);
  }

}