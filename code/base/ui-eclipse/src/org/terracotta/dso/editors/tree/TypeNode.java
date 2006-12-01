/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.tree;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.terracotta.dso.PatternHelper;

import java.util.Vector;

import javax.swing.tree.TreeNode;

/**
 * A TreeNode that represents a Java type (yeah, a class).
 * 
 * @see JavaProjectNode
 */

public class TypeNode extends JavaProjectNode {
  private IType     m_type;
  private String    m_name;
  private String    m_signature;
  private boolean   m_showFields;
  private IField[]  m_fields;
  private boolean   m_showMethods;
  private IMethod[] m_methods;
  private boolean   m_showTypes;
  private IType[]   m_types;
  
  public TypeNode(IType type) {
    this(type, true, true, true);
  }
  
  public TypeNode(
    IType   type,
    boolean showFields,
    boolean showMethods,
    boolean showTypes)
  {
    super(type);
    
    m_type        = type;
    m_name        = type.getElementName();
    m_signature   = showMethods ? PatternHelper.getExecutionPattern(type) :
                                  PatternHelper.getFullyQualifiedName(type);
    m_showFields  = showFields;
    m_showMethods = showMethods;
    m_showTypes   = showTypes;
  }

  public int getChildCount() {
    if(children == null) {
      children = new Vector();
      children.setSize(determineChildCount());
    }
    
    return children.size();
  }
  
  private int determineChildCount() {
    int result = 0;
    
    if(m_showFields) {
      result += ensureFields().length;
    }
    if(m_showMethods) {
      result += ensureMethods().length;
    }
    if(m_showTypes) {
      result += ensureTypes().length;
    }
    
    return result;
}
  
  public TreeNode getChildAt(int index) {
    if(children.elementAt(index) == null) {
      addChildren();
    }
    
    return super.getChildAt(index);
  }
  
  private void addChildren() {
    int count = 0;
    
    if(m_showFields) {
      IField[] fields = ensureFields();
      
      for(int i = 0; i < fields.length; i++, count++) {
        setChildAt(new FieldNode(fields[i]), i);
      }
    }
    
    if(m_showMethods) {
      IMethod[] methods = ensureMethods();
      
      for(int i = 0, j = count; i < methods.length; i++, count++, j++) {
        setChildAt(new MethodNode(methods[i]), j);
      }
    }

    if(m_showTypes) {
      IType[]  types = ensureTypes();
      TypeNode node;
      
      for(int i = 0, j = count; i < types.length; i++, j++) {
        node = new TypeNode(types[i], m_showFields, m_showMethods, m_showTypes);
        setChildAt(node, j);
      }
    }
  }
  
  public String getSignature() {
    return m_signature;
  }
  
  public String[] getFields() {
    String   typeName = PatternHelper.getFullyQualifiedName(m_type);
    IField[] fields   = internalGetFields();
    String[] result   = new String[fields.length];
    
    for(int i = 0; i < fields.length; i++) {
      result[i] = typeName+"."+fields[i].getElementName();
    }
    
    return result;
  }
  
  public String toString() {
    return m_name;
  }
  
  private IField[] ensureFields() {
    if(m_fields == null) {
      m_fields = internalGetFields();
    }
    return m_fields;
  }
  
  private IField[] internalGetFields() {
    try {
      return (IField[])JavaElementComparable.sort(m_type.getFields());
    } catch(JavaModelException jme) {
      return new IField[0];
    }
  }

  private IMethod[] ensureMethods() {
    if(m_methods == null) {
      m_methods = internalGetMethods();
    }
    return m_methods;
  }
  
  private IMethod[] internalGetMethods() {
    try {
      return (IMethod[])JavaElementComparable.sort(m_type.getMethods());
    } catch(JavaModelException jme) {
      return new IMethod[0];
    }
  }

  private IType[] ensureTypes() {
    if(m_types == null) {
      m_types = internalGetTypes();
    }
    return m_types;
  }
  
  private IType[] internalGetTypes() {
    try {
      return (IType[])JavaElementComparable.sort(m_type.getTypes());
    } catch(JavaModelException jme) {
      return new IType[0];
    }
  }
}
