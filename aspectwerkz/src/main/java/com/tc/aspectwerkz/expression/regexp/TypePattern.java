/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.aspectwerkz.expression.regexp;

import com.tc.aspectwerkz.expression.ExpressionException;
import com.tc.aspectwerkz.expression.SubtypePatternType;
import com.tc.aspectwerkz.proxy.ProxyDelegationStrategy;
import com.tc.aspectwerkz.proxy.ProxySubclassingStrategy;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.util.Strings;

import java.io.ObjectInputStream;

/**
 * Implements the regular expression pattern matcher for types.
 * 
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public class TypePattern extends Pattern {

  /**
   * The fully qualified type name.
   */
  protected transient com.tc.jrexx.regex.Pattern m_typeNamePattern;

  /**
   * The pattern as a string.
   */
  protected String                               m_pattern;

  /**
   * The subtype pattern type.
   */
  private final SubtypePatternType               m_subtypePatternType;

  /**
   * Private constructor.
   * 
   * @param pattern the pattern
   * @param subtypePatternType the subtype pattern type
   */
  TypePattern(final String pattern, final SubtypePatternType subtypePatternType) {
    m_pattern = pattern;
    m_subtypePatternType = subtypePatternType;
    escape(m_pattern);
  }

  /**
   * Matches a type name.
   * 
   * @param typeName the name of the type
   * @return true if we have a matche
   */
  public boolean matches(String typeName) {
    // regular match
    if (m_typeNamePattern.contains(typeName)) { return true; }

    // fallback on subclassing proxy match and Cglib extension
    int awProxySuffixStart1 = typeName.indexOf(ProxySubclassingStrategy.PROXY_SUFFIX);
    int awProxySuffixStart2 = typeName.indexOf(ProxyDelegationStrategy.PROXY_SUFFIX);
    if (awProxySuffixStart1 > 0) {
      typeName = typeName.substring(0, awProxySuffixStart1);
    } else if (awProxySuffixStart2 > 0) {
      typeName = typeName.substring(0, awProxySuffixStart2);
    } else {
      int cglibFastClassSuffixStarg = typeName.indexOf("$$FastClassByCGLIB$$");
      if (cglibFastClassSuffixStarg > 0) {
        // always filter away cglib fast class classes
        return false;
      }
      int cglibEnhancerSuffixStart = typeName.indexOf("$$EnhancerByCGLIB$$");
      if (cglibEnhancerSuffixStart > 0) {
        typeName = typeName.substring(0, cglibEnhancerSuffixStart);
      }
    }
    if (typeName == null) { return false; }
    if (typeName.equals("")) { return false; }
    return m_typeNamePattern.contains(typeName);
  }

  /**
   * Matches a type.
   * 
   * @param classInfo the info of the class
   * @return
   */
  public boolean matchType(final ClassInfo classInfo) {
    SubtypePatternType type = getSubtypePatternType();
    if (type.equals(SubtypePatternType.MATCH_ON_ALL_METHODS)) {
      return matchSuperClasses(classInfo);
    } else if (type.equals(SubtypePatternType.MATCH_ON_BASE_TYPE_METHODS_ONLY)) {
      // TODO: matching on methods ONLY in base type needs to be completed
      // TODO: needs to work together with the method and field matching somehow
      return matchSuperClasses(classInfo);
    } else {
      return classInfo != null && matches(classInfo.getName());
    }
  }

  /**
   * Tries to finds a parse at some superclass in the hierarchy.
   * <p/>
   * Only checks for a class parse to allow early filtering.
   * <p/>
   * Recursive.
   * 
   * @param classInfo the class info
   * @return boolean
   */
  public boolean matchSuperClasses(final ClassInfo classInfo) {
    if ((classInfo == null)) { return false; }

    // parse the class/super class
    if (matches(classInfo.getName())) {
      return true;
    } else {
      // parse the interfaces for the class
      if (matchInterfaces(classInfo.getInterfaces(), classInfo)) { return true; }

      if (classInfo.getSuperclass() == classInfo) { return false; }

      // no parse; getClass the next superclass
      return matchSuperClasses(classInfo.getSuperclass());
    }
  }

  /**
   * Tries to finds a parse at some interface in the hierarchy.
   * <p/>
   * Only checks for a class parse to allow early filtering.
   * <p/>
   * Recursive.
   * 
   * @param interfaces the interfaces
   * @param classInfo the class info
   * @return boolean
   */
  public boolean matchInterfaces(final ClassInfo[] interfaces, final ClassInfo classInfo) {
    if ((interfaces.length == 0) || (classInfo == null)) { return false; }
    for (int i = 0; i < interfaces.length; i++) {
      ClassInfo anInterface = interfaces[i];
      if (matches(anInterface.getName())) {
        return true;
      } else {
        if (matchInterfaces(anInterface.getInterfaces(), classInfo)) {
          return true;
        } else {
          continue;
        }
      }
    }
    return false;
  }

  /**
   * Returns the subtype pattern type
   * 
   * @return boolean
   */
  public SubtypePatternType getSubtypePatternType() {
    return m_subtypePatternType;
  }

  /**
   * Checks if the pattern matches all types.
   * 
   * @return boolean
   */
  public boolean isEagerWildCard() {
    return m_pattern.equals(EAGER_WILDCARD);
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
   * Escapes the type pattern.
   * 
   * @param pattern the method pattern
   */
  protected void escape(final String pattern) {
    String typeName = pattern;
    if (ABBREVIATIONS.containsKey(pattern)) {
      typeName = (String) ABBREVIATIONS.get(pattern);
    }
    try {
      if (typeName.equals(REGULAR_WILDCARD) || typeName.equals(EAGER_WILDCARD)) {
        typeName = "[a-zA-Z0-9_$.\\[\\]]+";
      } else {
        // CAUTION: order matters
        typeName = Strings.replaceSubString(typeName, "[", "\\[");
        typeName = Strings.replaceSubString(typeName, "]", "\\]");
        typeName = Strings.replaceSubString(typeName, "..", "[a-zA-Z0-9_$.]+");
        typeName = Strings.replaceSubString(typeName, ".", "\\.");
        typeName = Strings.replaceSubString(typeName, "*", "[a-zA-Z0-9_$\\[\\]]*");
      }
      m_typeNamePattern = new com.tc.jrexx.regex.Pattern(typeName);
    } catch (Throwable e) {
      e.printStackTrace();
      throw new ExpressionException("type pattern is not well formed: " + pattern, e);
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
    result = (37 * result) + hashCodeOrZeroIfNull(m_typeNamePattern);
    return result;
  }

  protected static int hashCodeOrZeroIfNull(final Object o) {
    if (null == o) { return 19; }
    return o.hashCode();
  }

  public boolean equals(final Object o) {
    if (this == o) { return true; }
    if (!(o instanceof TypePattern)) { return false; }
    final TypePattern obj = (TypePattern) o;
    return areEqualsOrBothNull(obj.m_pattern, this.m_pattern)
           && areEqualsOrBothNull(obj.m_typeNamePattern, this.m_typeNamePattern);
  }

  protected static boolean areEqualsOrBothNull(final Object o1, final Object o2) {
    if (null == o1) { return (null == o2); }
    return o1.equals(o2);
  }
}