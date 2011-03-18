/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.walker;

import com.tc.util.ClassUtils;

import java.lang.reflect.Field;

/**
 * Produces a textual representation of the type visited
 */
public class PrintVisitor implements Visitor {
  private static final String  INDENT = "  ";
  private final OutputSink     out;
  private final WalkTest       walkTest;
  private final ValueFormatter formatter;

  public PrintVisitor(OutputSink sink, WalkTest walkTest, ValueFormatter formatter) {
    this.out = sink;
    this.walkTest = walkTest;
    this.formatter = formatter;
  }

  public void visitRootObject(MemberValue value) {
    outputLine(typeDisplay(value.getValueObject().getClass()) + " (id " + value.getId() + ")");
  }

  private void outputLine(String line) {
    out.output(line);
  }

  public void visitValue(MemberValue value, int depth) {
    StringBuffer buf = new StringBuffer();
    boolean isLiteral = !walkTest.shouldTraverse(value);

    indent(depth, buf);

    if (value.isMapKey()) {
      buf.append("key ");
    } else if (value.isMapValue()) {
      buf.append("value ");
    }

    Field field = value.getSourceField();
    if (field != null) {
      buf.append(typeDisplay(field.getType()) + " ");
      boolean shadowed = value.isShadowed();
      if (shadowed) {
        buf.append(field.getDeclaringClass().getName() + ".");
      }

      buf.append(field.getName() + " ");
    }

    if (value.isElement()) {
      buf.append("[" + value.getIndex() + "] ");
    }

    buf.append("= ");

    Object o = value.getValueObject();

    if (isLiteral || o == null) {
      buf.append(formatter.format(value.getValueObject()));
    } else {
      if (value.isRepeated()) {
        buf.append("(ref id " + value.getId() + ")");
      } else {
        if ((field != null) && (o.getClass().equals(field.getType()))) {
          buf.append("(id " + value.getId() + ")");
        } else {
          buf.append("(" + typeDisplay(o.getClass()) + ", id " + value.getId() + ")");
        }
      }
    }

    String adorn = formatter.valueAdornment(value);
    if (adorn != null) {
      buf.append(adorn);
    }

    outputLine(buf.toString());
  }

  private static final String JAVA_LANG     = "java.lang.";
  private static final int    JAVA_LANG_LEN = JAVA_LANG.length();

  private static final String JAVA_UTIL     = "java.util.";
  private static final int    JAVA_UTIL_LEN = JAVA_UTIL.length();

  private static String typeDisplay(Class c) {
    String type;
    if (c.isArray()) {
      type = ClassUtils.baseComponentType(c).getName();
    } else {
      type = c.getName();
    }

    if (type.startsWith(JAVA_LANG) && type.lastIndexOf('.') + 1 == JAVA_LANG_LEN) {
      type = type.substring(JAVA_LANG_LEN);
    } else if (type.startsWith(JAVA_UTIL) && type.lastIndexOf('.') + 1 == JAVA_UTIL_LEN) {
      type = type.substring(JAVA_UTIL_LEN);
    }

    if (c.isArray()) {
      StringBuilder arrayType = new StringBuilder(type);
      int dim = ClassUtils.arrayDimensions(c);
      for (int i = 0; i < dim; i++) {
        arrayType.append("[]");
      }
      return arrayType.toString();
    } else {
      return type;
    }
  }

  public void visitMapEntry(int index, int depth) {
    StringBuffer buf = new StringBuffer();
    indent(depth, buf);
    buf.append("[entry ").append(index).append("]");
    outputLine(buf.toString());
  }

  private static void indent(int currentDepth, StringBuffer buffer) {
    while (currentDepth-- > 0) {
      buffer.append(INDENT);
    }
  }

  public interface OutputSink {
    void output(String line);
  }

  public interface ValueFormatter {
    String format(Object value);

    String valueAdornment(MemberValue value);
  }

}