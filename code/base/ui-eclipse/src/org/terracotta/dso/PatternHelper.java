/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.ExpressionVisitor;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.object.bytecode.aspectwerkz.AsmMethodInfo;
import com.tc.object.bytecode.aspectwerkz.ClassInfoFactory;
import com.tc.object.bytecode.aspectwerkz.ExpressionHelper;

/**
 * Utility singleton for bridging the gap between Eclipse internal
 * Java parser constructs (MethodDeclaration) and Aspectwerks expressions.
 */

public class PatternHelper {
  private static PatternHelper m_helper = new PatternHelper();
  private ExpressionHelper     m_expressionHelper;
  private ClassInfoFactory     m_classInfoFactory;

  public static final PatternHelper getHelper() {
    return m_helper;
  }
  
  private PatternHelper() {
    m_expressionHelper = new ExpressionHelper();
    m_classInfoFactory = new ClassInfoFactory();
  }

  public boolean matchesMethod(String expression, final IMethod method) {
    MethodInfo methodInfo = method != null ? getMethodInfo(method) : null;
    
    int parentIndex = expression.indexOf('(');
    if(parentIndex > 0){
      String tmp = expression.substring(parentIndex);
      tmp = StringUtils.replaceChars(tmp, '$', '.');
      expression = expression.substring(0, parentIndex)+tmp;
    }
    
    return methodInfo != null && matchesMember(expression, methodInfo);
  }
  
  public boolean matchesMethod(
    final String            expression,
    final MethodDeclaration methodDecl)
  {
    return matchesMember(expression, getMethodInfo(methodDecl));
  }
  
  public ExpressionContext createExecutionExpressionContext(final IMethod method) {
    return createExecutionExpressionContext(getMethodInfo(method));
  }
  
  public ExpressionContext createExecutionExpressionContext(final MemberInfo methodInfo) {
    return m_expressionHelper.createExecutionExpressionContext(methodInfo);  
  }
  
  public void testValidateMethodExpression(final String expr) throws Exception {
    String execExpr = ExpressionHelper.expressionPattern2ExecutionExpression(expr);
    m_expressionHelper.createExpressionVisitor(execExpr);
  }
  
  public boolean matchesMethod(final String expr, final ExpressionContext exprCntx) {
    try {
      String            execExpr = ExpressionHelper.expressionPattern2ExecutionExpression(expr);
      ExpressionVisitor visitor  = m_expressionHelper.createExpressionVisitor(execExpr);
    
      return visitor.match(exprCntx);
    } catch(Exception e) {
      return false;
    }
  }
  
  public boolean matchesMember(String expr, final MemberInfo methodInfo) {
    int parentIndex = expr.indexOf('(');
    if(parentIndex > 0){
      String tmp = expr.substring(parentIndex);
      tmp = StringUtils.replaceChars(tmp, '$', '.');
      expr = expr.substring(0, parentIndex)+tmp;
    }
    
    return matchesMethod(expr, m_expressionHelper.createExecutionExpressionContext(methodInfo));
  }

  public static String getFullyQualifiedName(IType type) {
    return type.getFullyQualifiedName('$');
  }
  
  public ExpressionContext createWithinExpressionContext(final IType type) {
    return createWithinExpressionContext(getFullyQualifiedName(type));
  }

  public ExpressionContext createWithinExpressionContext(final IPackageFragment fragment) {
    return createWithinExpressionContext(fragment.getElementName());
  }

  public ExpressionContext createWithinExpressionContext(final String className) {
    return createWithinExpressionContext(m_classInfoFactory.getClassInfo(className));
  }
  
  public ExpressionContext createWithinExpressionContext(final ClassInfo classInfo) {
    return m_expressionHelper.createWithinExpressionContext(classInfo);
  }
  
  public boolean matchesClass(final String expr, final ExpressionContext exprCntx) {
    try {
      String            withinExpr = ExpressionHelper.expressionPattern2WithinExpression(expr);
      ExpressionVisitor visitor    = m_expressionHelper.createExpressionVisitor(withinExpr);
    
      return visitor.match(exprCntx);
    } catch(Exception e) {
      return false;
    }
  }
  
  public boolean matchesClass(final String expression, final String className) {
    return matchesClass(expression, m_classInfoFactory.getClassInfo(className));
  }
  
  public boolean matchesClass(final String expr, final ClassInfo classInfo) {
    return matchesClass(expr, m_expressionHelper.createWithinExpressionContext(classInfo));
  }
  
  public boolean matchesType(final String expr, final IType type) {
    return matchesClass(expr, createWithinExpressionContext(type));
  }
  
  public boolean matchesPackageFragment(final String expr, final IPackageFragment fragment) {
    return matchesClass(expr, createWithinExpressionContext(fragment));
  }

  public static String getSignature(IMethod method)
    throws JavaModelException
  {
    IType        dType  = method.getDeclaringType();
    String[]     pTypes = method.getParameterTypes();
    String       rType  = method.getReturnType();
    StringBuffer sb     = new StringBuffer("(");
    
    rType = Signature.getTypeErasure(rType);
    
    for(int i = 0; i < pTypes.length; i++) {
      pTypes[i] = Signature.getTypeErasure(pTypes[i]);
      JdtUtils.resolveTypeName(pTypes[i], dType, sb);
    }
    sb.append(')');
    JdtUtils.resolveTypeName(rType, dType, sb);

    String result = sb.toString().replace('.', '/');

    return result;
  }
  
  public static String getFullName(IMethod method) {
    IType declaringType = method.getDeclaringType();
    return getFullyQualifiedName(declaringType)+"."+method.getElementName();
  }
  
  public static boolean isVarargs(IMethod method) {
    try {
      return Flags.isVarargs(method.getFlags());
    } catch(JavaModelException jme) {
      return false;
    }
  }
  
  /**
   * Returns a full method signature, compatible with the forms required
   * by the DSO config format.
   */
  public static String getJavadocSignature(IMethod method)
    throws JavaModelException
  {
    StringBuffer sb            = new StringBuffer();
    IType        declaringType = method.getDeclaringType();
    boolean      isVararg      = isVarargs(method);
    String[]     params        = method.getParameterTypes();
    int          lastParam     = params.length - 1;
    int          dim;
    
    try {
      String returnType = method.getReturnType();

      dim        = Signature.getArrayCount(returnType);
      returnType = Signature.getTypeErasure(returnType);
      sb.append(JdtUtils.getResolvedTypeFileName(returnType, declaringType));
      while(dim > 0) {
        sb.append("[]");
        dim--;
      }
    } catch(JavaModelException jme) {
      sb.append("*");
    }
    
    sb.append(" ");
    sb.append(getFullyQualifiedName(declaringType));
    sb.append(".");
    sb.append(method.isConstructor() ? "__INIT__" : method.getElementName());
    sb.append("(");

    for(int i = 0; i < params.length; i++) {
      if(i != 0) {
        sb.append(", ");
      }

      params[i] = Signature.getTypeErasure(params[i]);
      sb.append(JdtUtils.getResolvedTypeFileName(params[i], declaringType));
      dim = Signature.getArrayCount(params[i]);
    
      if(i == lastParam && isVararg) {
        dim--;
      }
      while (dim > 0) {
        sb.append("[]");
        dim--;
      }
      if(i == lastParam && isVararg) {
        sb.append("...");
      }
    }
    
    sb.append(")");
    
    return sb.toString();
  }
  
  public MethodInfo getMethodInfo(IMethod method) {
    MethodInfo info = null;
    
    if(method != null) {
      try {
        String   className  = getFullyQualifiedName(method.getDeclaringType());
        String   methodName = method.isConstructor() ? "__INIT__" : method.getElementName();
        String   desc       = getSignature(method);
        String[] excepts    = method.getExceptionTypes();
        int      access     = method.getFlags();
  
        info = getMethodInfo(access, className, methodName, desc, excepts);
      } catch(JavaModelException jme) {/**/}
        catch(NullPointerException npe) {/**/}
    }
    
    return info;
  }
  
  public MethodInfo getMethodInfo(MethodDeclaration methodDecl) {
    return getMethodInfo(methodDecl2IMethod(methodDecl));
  }
  
  public static IMethod methodDecl2IMethod(MethodDeclaration methodDecl) {
    IMethodBinding binding = methodDecl.resolveBinding();
    IJavaElement   elem    = binding != null ? binding.getJavaElement() : null;
    
    if(elem instanceof IMethod) {
      return (IMethod)elem;
    }

    return null;  
  }
  
  public MethodInfo getMethodInfo(
    int      modifiers,
    String   className,
    String   methodName,
    String   description,
    String[] exceptions)
  {
    return new AsmMethodInfo(m_classInfoFactory,
                             modifiers,
                             className,
                             methodName,
                             description,
                             exceptions);
  }
  
  public static String getExecutionPattern(IMethod method) {
    try {
      if(!method.getOpenable().isOpen()) {
        method.getOpenable().open(null);
      }
      return getJavadocSignature(method);
    } catch(JavaModelException jme) {
      IType  type     = method.getDeclaringType();
      String typeName = getFullyQualifiedName(type);

      return "* "+typeName+"."+method.getElementName()+"(..)";
    }
  }
  
  public static String getExecutionPattern(IType type) {
    return "* "+getWithinPattern(type)+"(..)";
  }
  
  public static String getWithinPattern(IType type) {
    return getFullyQualifiedName(type)+".*";
  }
  
  public static String getExecutionPattern(IPackageFragment fragment) {
    return "* "+getWithinPattern(fragment)+"(..)";
  }
  
  public static String getWithinPattern(IPackageFragment fragment) {
    return fragment.getElementName()+"..*";
  }
}

