/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.tree;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.terracotta.dso.PatternHelper;

/**
 * A TreeNode that represents a Java field, or instance variable.
 * 
 * @see JavaProjectNode
 * @see org.eclipse.jdt.core.IField
 */

public class FieldNode extends JavaProjectNode {
  private IField m_field;
  private String m_moniker;
  private String m_fullName;
  
  public FieldNode(IField field) {
    super(field);
    
    IType  parentType = field.getDeclaringType();
    String name       = field.getElementName();
    String type       = getTypeSignature(field);
    
    m_field    = field;
    m_moniker  = type != null ? name +" : "+type : name;
    m_fullName = PatternHelper.getFullyQualifiedName(parentType)+"."+name;
  }
  
  private static String getTypeSignature(IField field) {
    try {
      String result = field.getTypeSignature();
      return Signature.getSimpleName(Signature.toString(result));      
    } catch(JavaModelException jme) {
      return null;
    }
  }
  
  public IField getField() {
    return m_field;
  }
  
  public String toString() {
    return m_moniker;
  }

  public String getSignature() {
    return getFullyQualifiedName();
  }
  
  public String[] getFields() {
    return new String[] {getFullyQualifiedName()};
  }
  
  public String getFullyQualifiedName() {
    return m_fullName;
  }
}
