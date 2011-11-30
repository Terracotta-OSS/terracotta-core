/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.expression.regexp;


import com.tc.aspectwerkz.util.Strings;
import com.tc.aspectwerkz.expression.ExpressionException;

import java.io.ObjectInputStream;

/**
 * Implements the regular expression pattern matcher for names.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class NamePattern extends Pattern {
  /**
   * The name pattern.
   */
  protected transient com.tc.jrexx.regex.Pattern m_namePattern;

  /**
   * The name pattern as a string.
   */
  protected String m_pattern;

  /**
   * Private constructor.
   *
   * @param pattern the pattern
   */
  NamePattern(final String pattern) {
    m_pattern = pattern;
    escape(m_pattern);
  }

  /**
   * Matches a name.
   *
   * @param name the name
   * @return true if we have a matche
   */
  public boolean matches(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("name can not be null");
    }
    if (name.equals("")) {
      return false;
    }
    return m_namePattern.contains(name);
  }

  /**
   * Returns the pattern as a string.
   *
   * @return the pattern
   */
  public String getPattern() {
    return m_pattern;
  }

  /**
   * Escapes the name pattern.
   *
   * @param namePattern the name pattern
   */
  protected void escape(String namePattern) {
    try {
      if (namePattern.equals(REGULAR_WILDCARD)) {
        namePattern = "[a-zA-Z0-9_$.]+";
      } else {
        namePattern = Strings.replaceSubString(namePattern, "*", "[a-zA-Z0-9_$]*");
      }
      m_namePattern = new com.tc.jrexx.regex.Pattern(namePattern);
    } catch (Throwable e) {
      throw new ExpressionException("type pattern is not well formed: " + namePattern, e);
    }
  }

  /**
   * Provides custom deserialization.
   *
   * @param stream the object input stream containing the serialized object
   * @throws Exception in case of failure
   */
  private void readObject(final ObjectInputStream stream) throws Exception {
    ObjectInputStream.GetField fields = stream.readFields();
    m_pattern = (String) fields.get("m_pattern", null);
    escape(m_pattern);
  }

  public int hashCode() {
    int result = 17;
    result = (37 * result) + hashCodeOrZeroIfNull(m_pattern);
    result = (37 * result) + hashCodeOrZeroIfNull(m_namePattern);
    return result;
  }

  protected static int hashCodeOrZeroIfNull(final Object o) {
    if (null == o) {
      return 19;
    }
    return o.hashCode();
  }

  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NamePattern)) {
      return false;
    }
    final NamePattern obj = (NamePattern) o;
    return areEqualsOrBothNull(obj.m_pattern, this.m_pattern)
            && areEqualsOrBothNull(obj.m_namePattern, this.m_namePattern);
  }

  protected static boolean areEqualsOrBothNull(final Object o1, final Object o2) {
    if (null == o1) {
      return (null == o2);
    }
    return o1.equals(o2);
  }
}