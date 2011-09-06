/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.expression;


import com.tc.aspectwerkz.cflow.CflowAspectExpressionVisitor;
import com.tc.aspectwerkz.exception.DefinitionException;
import com.tc.aspectwerkz.exception.WrappedRuntimeException;
import com.tc.aspectwerkz.expression.ast.ExpressionParser;
import com.tc.aspectwerkz.expression.ast.Node;
import com.tc.aspectwerkz.expression.regexp.Pattern;
import com.tc.aspectwerkz.joinpoint.JoinPoint;
import com.tc.aspectwerkz.joinpoint.StaticJoinPoint;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.aspectwerkz.reflect.ClassInfoHelper;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.util.SequencedHashMap;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * Abstraction that holds info about the expression and the different visitors.
 * <br/>
 * We are using a lazy initialization for m_hasCflowPointcut field to allow to fully resolve each expression (that is f.e. on IBM
 * compiler, fields are in the reverse order, thus pointcut reference in aspect defined with annotations
 * may not be resolved until the whole class has been parsed.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class ExpressionInfo {

  public final static String JOINPOINT_CLASS_NAME = JoinPoint.class.getName();
  public final static String STATIC_JOINPOINT_CLASS_NAME = StaticJoinPoint.class.getName();
  public final static String JOINPOINT_ABBREVIATION = "JoinPoint";
  public final static String STATIC_JOINPOINT_ABBREVIATION = "StaticJoinPoint";
  public final static String RTTI_ABBREVIATION = "Rtti";

  /**
   * The sole instance of the parser.
   */
  private static final ExpressionParser s_parser = new ExpressionParser(System.in);

  private final ExpressionVisitor m_expression;

  private final AdvisedClassFilterExpressionVisitor m_advisedClassFilterExpression;

  private final CflowAspectExpressionVisitor m_cflowAspectExpression;

  /**
   * Ordered map of the pointcut arguments type, indexed by their name.
   */
  private Map m_argsTypeByName = new SequencedHashMap();

  /**
   * List<String> of possible arguments names/references that appear in the expression.
   * Note that afterReturning/Throwing binding will not appear here (not composable).
   * This list is lasily populated once using the ExpressionValidateVisitor.
   * Note that "types" are part of the populated list:
   * <br/>pointcutRef(x) ==> "x"
   * <br/>execution(...) && args(x, int) ==> "x", "int"
   * <br/>this(..), target(..)
   */
  private List m_possibleArguments = null;

  /**
   * Name of the special argument for an afterReturning/Throwing when this one is bounded.
   */
  private String m_specialArgumentName = null;

  /**
   * Creates a new expression info instance from its string representation
   *
   * @param expression the expression
   * @param namespace  the namespace
   */
  public ExpressionInfo(final String expression, final String namespace) {
    try {
      Node root;
      synchronized (s_parser) {
        root = s_parser.parse(expression);
      }
      m_expression = new ExpressionVisitor(this, expression, namespace, root);
      m_advisedClassFilterExpression =
              new AdvisedClassFilterExpressionVisitor(this, expression, namespace, root);
      m_cflowAspectExpression = new CflowAspectExpressionVisitor(this, root, namespace);
    } catch (Throwable e) {
      throw new DefinitionException("expression is not well-formed [" + expression + "]: " + e.getMessage(), e);
    }
  }

  /**
   * Creates a new expression info from an already parsed node
   * This is usefull when extracting cflow sub expressions.
   * <p/>
   * Some attached visitor will be wrong since the string representation
   * of the expression is not available.
   *
   * @param subExpression the sub expression node
   * @param namespace     the namespace
   */
  public ExpressionInfo(final Node subExpression, final String namespace) {
    try {
      m_expression = new ExpressionVisitor(this, "N/A", namespace, subExpression);
      m_advisedClassFilterExpression =
              new AdvisedClassFilterExpressionVisitor(this, "N/A", namespace, subExpression);
      m_cflowAspectExpression = new CflowAspectExpressionVisitor(this, subExpression, namespace);
    } catch (Throwable e) {
      throw new DefinitionException("sub expression is not well-formed from [" + subExpression + "]: " + e.getMessage(), e);
    }
  }

  /**
   * Returns the regular expression.
   *
   * @return the regular expression
   */
  public ExpressionVisitor getExpression() {
    return m_expression;
  }

  /**
   * Returns the namespace
   *
   * @return
   */
  public String getNamespace() {
    return m_expression.m_namespace;
  }

  /**
   * Returns the cflow aspect expression.
   *
   * @return the cflow aspect expression
   */
  public CflowAspectExpressionVisitor getCflowAspectExpression() {
    return m_cflowAspectExpression;
  }

  /**
   * Returns the advised class filter expression.
   *
   * @return the advised class filter expression
   */
  public AdvisedClassFilterExpressionVisitor getAdvisedClassFilterExpression() {
    return m_advisedClassFilterExpression;
  }

  /**
   * Returns the expression as string.
   *
   * @return the expression as string
   */
  public String toString() {
    return m_expression.toString();
  }

  /**
   * Add an argument extracted from the call signature of the expression info.
   * Check is made to ensure that the argument is part of an args(..) or pointcutReference(..) subexpression.
   * Note that specialArgument for afterReturning/Throwing is handled in a different way.
   *
   * @param name
   * @param className
   * @param loader
   */
  public void addArgument(final String name, final String className, final ClassLoader loader) {
    //AW-241
    // Note: we do not check the signature and we ignore JoinPoint parameters types
    String expression = toString();
    // fast check if we have a parenthesis
    if (expression.indexOf('(') > 0) {
      // fast check if the given argument (that appears in the advice signature) is part of the pointcut expression
      if (!isJoinPointOrRtti(className, loader)) {
        if (toString().indexOf(name) < 0) {
          throw new DefinitionException(
                  "pointcut expression is missing a parameter that has been encountered in the advice: '"
                          + toString() + "' - '" + name + "' of type '" + className +
                          "' missing in '" +
                          getExpression().m_namespace +
                          "'"
          );
        } else {
          // lazily populate the possible argument list
          if (m_possibleArguments == null) {
            m_possibleArguments = new ArrayList();
            new ExpressionValidateVisitor(toString(), getNamespace(), getExpression().m_root)
                    .populate(m_possibleArguments);
          }
          if (!m_possibleArguments.contains(name)) {
            throw new DefinitionException(
                    "pointcut expression is missing a parameter that has been encountered in the advice: '"
                            + toString() + "' - '" + name + "' of type '" +
                            className +
                            "' missing in '" +
                            getExpression().m_namespace +
                            "'"
            );
          }
        }
      }
    }
    m_argsTypeByName.put(name, className);
  }

  /**
   * Set the bounded name of the special argument for afterReturning/Throwing binding
   *
   * @param specialArgumentName
   */
  public void setSpecialArgumentName(String specialArgumentName) {
    m_specialArgumentName = specialArgumentName;
  }

  /**
   * Get the bounded name of the special argument for afterReturning/Throwing binding
   *
   * @return
   */
  public String getSpecialArgumentName() {
    return m_specialArgumentName;
  }

  /**
   * Returns the argumen type.
   *
   * @param parameterName
   * @return
   */
  public String getArgumentType(final String parameterName) {
    return (String) m_argsTypeByName.get(parameterName);
  }

  /**
   * Returns the argument index.
   *
   * @param parameterName
   * @return
   */
  public int getArgumentIndex(final String parameterName) {
    if (m_argsTypeByName.containsKey(parameterName)) {
      return ((SequencedHashMap) m_argsTypeByName).indexOf(parameterName);
    } else {
      return -1;
    }
  }

  /**
   * Returns the argument at the given index.
   *
   * @param index
   * @return paramName
   */
  public String getArgumentNameAtIndex(final int index) {
    if (index >= m_argsTypeByName.size()) {
      throw new ArrayIndexOutOfBoundsException(
              "cannot getDefault argument at index " +
                      index + " in " + m_expression.toString()
      );
    }
    return (String) m_argsTypeByName.keySet().toArray()[index];
  }

  /**
   * Returns all argument names.
   *
   * @return
   */
  public Set getArgumentNames() {
    return m_argsTypeByName.keySet();
  }

  /**
   * Check if the given className is one of the know argument: JoinPoint, StaticJoinPoint, Rtti
   * <p/>
   * className can be not qualified (for XML def simplification)
   *
   * @param className
   * @param loader
   * @return true if so
   */
  private boolean isJoinPointOrRtti(String className, final ClassLoader loader) {
    if (JOINPOINT_CLASS_NAME.equals(className)
            || STATIC_JOINPOINT_CLASS_NAME.equals(className)
            || JOINPOINT_ABBREVIATION.equals(className)
            || STATIC_JOINPOINT_ABBREVIATION.equals(className)
            || RTTI_ABBREVIATION.equals(className)) {
      return true;
    }
    if (className.equals("int") ||
            className.equals("long") ||
            className.equals("short") ||
            className.equals("float") ||
            className.equals("double") ||
            className.equals("boolean") ||
            className.equals("byte") ||
            className.equals("char") ||
            className.endsWith("]") ||
            className.startsWith("java.")) {
      return false;
    }
    try {
      String fullClassName = (String) Pattern.ABBREVIATIONS.get(className);
      if (fullClassName != null) {
        className = fullClassName;
      }
      if (className.startsWith("java.")) {
        return false;
      }
      ClassInfo classInfo = AsmClassInfo.getClassInfo(className, loader);
      if (ClassInfoHelper.implementsInterface(classInfo, JOINPOINT_CLASS_NAME) ||
              ClassInfoHelper.implementsInterface(classInfo, STATIC_JOINPOINT_CLASS_NAME)) {
        return true;
      }
    } catch (Throwable e) {
      throw new WrappedRuntimeException(e);
    }
    return false;
  }

  public void inheritPossibleArgumentFrom(ExpressionInfo expressionInfo) {
    m_specialArgumentName = expressionInfo.m_specialArgumentName;
    m_possibleArguments = expressionInfo.m_possibleArguments;
    m_argsTypeByName = expressionInfo.m_argsTypeByName;
  }

  public static ExpressionParser getParser() {
    return s_parser;
  }
  
  public String getExpressionString() {
    return m_expression.toString();
  }
}

