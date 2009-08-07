/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.ExpressionVisitor;
import com.tc.aspectwerkz.expression.PointcutType;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.object.bytecode.aspectwerkz.ExpressionHelper;
import com.terracottatech.config.Root;

import java.util.HashMap;

/**
 * Utility singleton for bridging the gap between Eclipse internal Java parser constructs (MethodDeclaration) and
 * Aspectwerkx expressions.
 */

public class PatternHelper {
  private static PatternHelper   m_helper = new PatternHelper();
  private final ExpressionHelper m_expressionHelper;
  private final ClassInfoFactory m_classInfoFactory;
  private final HashMap          m_expressionContextCache;
  private final HashMap          m_executionExpressionContextCache;

  public static final PatternHelper getHelper() {
    return m_helper;
  }

  private PatternHelper() {
    m_expressionHelper = new ExpressionHelper();
    m_classInfoFactory = new ClassInfoFactory();
    m_expressionContextCache = new HashMap();
    m_executionExpressionContextCache = new HashMap();
  }

  public boolean matchesField(Root root, IField field) {
    if (root.isSetFieldName()) {
      return root.getFieldName().equals(ConfigurationHelper.getFullName(field));
    } else {
      FieldInfo fi = getFieldInfo(field);
      try {
        String fieldExpr = root.getFieldExpression();
        ExpressionVisitor visitor = m_expressionHelper.createExpressionVisitor("get(" + fieldExpr + ")");
        return visitor.match(new ExpressionContext(PointcutType.GET, fi, fi));
      } catch (Throwable t) {
        return false;
      }
    }
  }

  public boolean matchesMethod(String expression, final IMethod method) {
    MethodInfo methodInfo = method != null ? getMethodInfo(method) : null;

    int parentIndex = expression.indexOf('(');
    if (parentIndex > 0) {
      String tmp = expression.substring(parentIndex);
      tmp = StringUtils.replaceChars(tmp, '$', '.');
      expression = expression.substring(0, parentIndex) + tmp;
    }

    return methodInfo != null && matchesMember(expression, methodInfo);
  }

  public boolean matchesMethod(final String expression, final MethodDeclaration methodDecl) {
    return matchesMember(expression, getMethodInfo(methodDecl));
  }

  public ExpressionContext createExecutionExpressionContext(final IMethod method) {
    return createExecutionExpressionContext(getMethodInfo(method));
  }

  public ExpressionContext createExecutionExpressionContext(final MemberInfo methodInfo) {
    ExpressionContext exprCntx = (ExpressionContext) m_executionExpressionContextCache.get(methodInfo);
    if (exprCntx == null) {
      exprCntx = m_expressionHelper.createExecutionExpressionContext(methodInfo);
      m_executionExpressionContextCache.put(methodInfo, exprCntx);
    }
    return exprCntx;
  }

  public void testValidateMethodExpression(final String expr) throws Exception {
    String execExpr = ExpressionHelper.expressionPattern2ExecutionExpression(expr);
    m_expressionHelper.createExpressionVisitor(execExpr);
  }

  public boolean matchesMethod(final String expr, final ExpressionContext exprCntx) {
    try {
      String execExpr = ExpressionHelper.expressionPattern2ExecutionExpression(expr);
      ExpressionVisitor visitor = m_expressionHelper.createExpressionVisitor(execExpr);

      return visitor.match(exprCntx);
    } catch (Exception e) {
      return false;
    }
  }

  public boolean matchesMember(String expr, final MemberInfo methodInfo) {
    int parentIndex = expr.indexOf('(');
    if (parentIndex > 0) {
      String tmp = expr.substring(parentIndex);
      tmp = StringUtils.replaceChars(tmp, '$', '.');
      expr = expr.substring(0, parentIndex) + tmp;
    }

    return matchesMethod(expr, createExecutionExpressionContext(methodInfo));
  }

  public static String getFullyQualifiedName(IType type) {
    return type.getFullyQualifiedName('$');
  }

  public ExpressionContext createWithinExpressionContext(final IType type) {
    return createWithinExpressionContext(m_classInfoFactory.getClassInfo(type));
  }

  // the following two methods are non-sensical

  public ExpressionContext createWithinExpressionContext(final IPackageDeclaration packageDecl) {
    return createWithinExpressionContext(packageDecl.getElementName());
  }

  public ExpressionContext createWithinExpressionContext(final IPackageFragment fragment) {
    return createWithinExpressionContext(fragment.getElementName());
  }

  public ExpressionContext createWithinExpressionContext(final String className) {
    return createWithinExpressionContext(m_classInfoFactory.getClassInfo(className));
  }

  public ExpressionContext createWithinExpressionContext(final ClassInfo classInfo) {
    ExpressionContext exprCntx = (ExpressionContext) m_expressionContextCache.get(classInfo);
    if (exprCntx == null) {
      exprCntx = m_expressionHelper.createWithinExpressionContext(classInfo);
      m_expressionContextCache.put(classInfo, exprCntx);
    }
    return exprCntx;
  }

  public boolean matchesClass(final String expr, final ExpressionContext exprCntx) {
    try {
      String withinExpr = ExpressionHelper.expressionPattern2WithinExpression(expr);
      ExpressionVisitor visitor = m_expressionHelper.createExpressionVisitor(withinExpr);

      return visitor.match(exprCntx);
    } catch (Exception e) {
      return false;
    }
  }

  public JavaModelClassInfo getClassInfo(IType type) {
    return (JavaModelClassInfo) m_classInfoFactory.getClassInfo(type);
  }

  public boolean matchesClass(final String expression, final String className) {
    if (expression != null && expression.equals(className)) return true;
    return matchesClass(expression, m_classInfoFactory.getClassInfo(className));
  }

  public boolean matchesClass(final String expr, final ClassInfo classInfo) {
    return matchesClass(expr, createWithinExpressionContext(classInfo));
  }

  public boolean matchesType(final String expr, final IType type) {
    return matchesClass(expr, createWithinExpressionContext(type));
  }

  public boolean matchesPackageFragment(final String expr, final IPackageFragment fragment) {
    return expr.equals(fragment.getElementName() + "..*") || expr.equals(fragment.getElementName() + ".*");
  }

  public boolean matchesPackageDeclaration(final String expr, final IPackageDeclaration packageDecl) {
    return expr.equals(packageDecl.getElementName() + "..*") || expr.equals(packageDecl.getElementName() + ".*");
  }

  public static String getSignature(IMethod method) throws JavaModelException {
    IType dType = method.getDeclaringType();
    String[] pTypes = method.getParameterTypes().clone();
    String rType = method.getReturnType();
    StringBuffer sb = new StringBuffer("(");

    rType = Signature.getTypeErasure(rType);

    for (int i = 0; i < pTypes.length; i++) {
      String erasedSig = Signature.getTypeErasure(pTypes[i]);
      if (erasedSig.charAt(0) == 'T') {
        erasedSig = "Ljava.lang.Object;";
      } else if (erasedSig.charAt(0) == '[' && erasedSig.charAt(1) == 'T') {
        erasedSig = "[Ljava.lang.Object;";
      }
      JdtUtils.resolveTypeName(erasedSig, dType, sb);
    }
    sb.append(')');
    if (rType.charAt(0) == 'T') {
      rType = "Ljava.lang.Object;";
    } else if (rType.charAt(0) == '[' && rType.charAt(1) == 'T') {
      rType = "[Ljava.lang.Object;";
    }
    JdtUtils.resolveTypeName(rType, dType, sb);

    String result = sb.toString().replace('.', '/');

    return result;
  }

  public static String getFullName(IMethod method) {
    IType declaringType = method.getDeclaringType();
    return getFullyQualifiedName(declaringType) + "." + method.getElementName();
  }

  public static boolean isVarargs(IMethod method) {
    try {
      return Flags.isVarargs(method.getFlags());
    } catch (JavaModelException jme) {
      return false;
    }
  }

  /**
   * Returns a full method signature, compatible with the forms required by the DSO config format.
   */
  public static String getJavadocSignature(IMethod method) throws JavaModelException {
    StringBuffer sb = new StringBuffer();
    IType declaringType = method.getDeclaringType();
    boolean isVararg = isVarargs(method);
    String[] params = method.getParameterTypes().clone();
    int lastParam = params.length - 1;
    int dim;

    try {
      String returnType = method.getReturnType();

      dim = Signature.getArrayCount(returnType);
      returnType = Signature.getTypeErasure(returnType);
      sb.append(JdtUtils.getResolvedTypeFileName(returnType, declaringType));
      while (dim > 0) {
        sb.append("[]");
        dim--;
      }
    } catch (JavaModelException jme) {
      sb.append("*");
    }

    sb.append(" ");
    sb.append(getFullyQualifiedName(declaringType));
    sb.append(".");
    sb.append(method.isConstructor() ? "__INIT__" : method.getElementName());
    sb.append("(");

    for (int i = 0; i < params.length; i++) {
      if (i != 0) {
        sb.append(", ");
      }

      String erasedSig = Signature.getTypeErasure(params[i]);
      sb.append(JdtUtils.getResolvedTypeFileName(erasedSig, declaringType));
      dim = Signature.getArrayCount(erasedSig);

      if (i == lastParam && isVararg) {
        dim--;
      }
      while (dim > 0) {
        sb.append("[]");
        dim--;
      }
      if (i == lastParam && isVararg) {
        sb.append("...");
      }
    }

    sb.append(")");

    return sb.toString();
  }

  public MethodInfo getMethodInfo(IMethod method) {
    MethodInfo info = null;

    if (method != null) {
      try {
        info = m_classInfoFactory.getMethodInfo(method);
      } catch (JavaModelException jme) {/**/
      } catch (NullPointerException npe) {/**/
      }
    }

    return info;
  }

  public FieldInfo getFieldInfo(IField field) {
    return (field != null) ? m_classInfoFactory.getFieldInfo(field) : null;
  }

  public MethodInfo getMethodInfo(MethodDeclaration methodDecl) {
    return getMethodInfo(methodDecl2IMethod(methodDecl));
  }

  public static IMethod methodDecl2IMethod(MethodDeclaration methodDecl) {
    IMethodBinding binding = methodDecl.resolveBinding();
    IJavaElement elem = binding != null ? binding.getJavaElement() : null;

    if (elem instanceof IMethod) { return (IMethod) elem; }

    return null;
  }

  public static String getExecutionPattern(IJavaElement element) {
    if (element instanceof IMethod) {
      return getExecutionPattern((IMethod) element);
    } else if (element instanceof IType) {
      return getExecutionPattern((IType) element);
    } else if (element instanceof IPackageFragment) {
      return getExecutionPattern((IPackageFragment) element);
    } else if (element instanceof ICompilationUnit) {
      return getExecutionPattern(((ICompilationUnit) element).findPrimaryType());
    } else if (element instanceof IPackageDeclaration) { return getExecutionPattern((IPackageDeclaration) element); }
    return null;
  }

  public static String getExecutionPattern(IMethod method) {
    try {
      if (!method.getOpenable().isOpen()) {
        method.getOpenable().open(null);
      }
      return getJavadocSignature(method);
    } catch (JavaModelException jme) {
      IType type = method.getDeclaringType();
      String typeName = getFullyQualifiedName(type);

      return "* " + typeName + "." + method.getElementName() + "(..)";
    }
  }

  public static String getExecutionPattern(IType type) {
    return "* " + getWithinPattern(type) + "(..)";
  }

  public static String getWithinPattern(IType type) {
    return getFullyQualifiedName(type) + ".*";
  }

  public static String getExecutionPattern(IPackageFragment fragment) {
    return "* " + getWithinPattern(fragment) + "(..)";
  }

  public static String getExecutionPattern(IPackageDeclaration packageDecl) {
    return "* " + getWithinPattern(packageDecl) + "(..)";
  }

  public static String getWithinPattern(IPackageFragment fragment) {
    return fragment.getElementName() + "..*";
  }

  public static String getWithinPattern(IPackageDeclaration packageDecl) {
    return packageDecl.getElementName() + "..*";
  }
}
