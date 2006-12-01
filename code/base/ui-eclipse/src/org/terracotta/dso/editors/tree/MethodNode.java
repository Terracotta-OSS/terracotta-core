/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.tree;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.Signature;

import org.terracotta.dso.PatternHelper;

/**
 * A TreeNode that represents a Java method.
 * 
 * @see JavaProjectNode
 */

public class MethodNode extends JavaProjectNode {
  private IMethod m_method;      
  private String  m_moniker;
  private String  m_signature;
  
  public MethodNode(IMethod method) {
    super(method);

    m_method    = method;
    m_moniker   = buildMoniker(method);
    m_signature = buildSignature(method);
  }
  
  private static String buildSignature(IMethod method) {
    return PatternHelper.getExecutionPattern(method);
  }
  
  private static String buildMoniker(IMethod method) {
    StringBuffer sb           = new StringBuffer();
    String       paramTypes[] = getParameterTypes(method);
    
    sb.append(method.getElementName());
    sb.append("(");
    
    for(int i = 0; i < paramTypes.length; i++) {
      sb.append(paramTypes[i]);

      if(i < paramTypes.length-1) {
        sb.append(", ");
      }
    }
    sb.append(")");
    
    return sb.toString();
  }
  
  private static String[] getParameterTypes(IMethod method) {
    String[] typeSigs = method.getParameterTypes();
    String[] types    = new String[typeSigs.length];
    
    for(int i= 0; i < typeSigs.length; i++) {
      types[i]= Signature.getSimpleName(Signature.toString(typeSigs[i]));
    }
    
    return types;
  }
  
  public IMethod getMethod() {
    return m_method;
  }

  public String getSignature() {
    return m_signature;
  }
  
  public String[] getFields() {
    return new String[0];
  }
  
  public String toString() {
    return m_moniker;
  }
}
