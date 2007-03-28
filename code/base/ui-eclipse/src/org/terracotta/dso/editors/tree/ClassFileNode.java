/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.tree;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.terracotta.dso.PatternHelper;

import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.tree.TreeNode;

/**
 * A TreeNode that represents a Java module (ICompilationUnit).
 * 
 * @see JavaProjectNode
 * @see org.eclipse.jdt.core.ICompilationUnit
 */

public class ClassFileNode extends JavaProjectNode {
  private IClassFile m_classFile;
  private String     m_signature;
  private String     m_moduleName;
  private boolean    m_showFields;
  private boolean    m_showMethods;
  private boolean    m_showTypes;
  private TypeNode   m_typeNode;
  
  public ClassFileNode(IClassFile classFile) {
    this(classFile, true, true, true);
  }
  
  public ClassFileNode(
    IClassFile classFile,
    boolean    showFields,
    boolean    showMethods,
    boolean    showTypes)
  {
    super(classFile);
    
    m_classFile   = classFile;
    m_moduleName  = internalGetType().getElementName()+".class";
    m_signature   = showMethods ? PatternHelper.getExecutionPattern(internalGetType()) :
                                  PatternHelper.getWithinPattern(internalGetType());
    m_showFields  = showFields;
    m_showMethods = showMethods;
    m_showTypes   = showTypes;
  }
  
  public int getChildCount() {
    return 1;
  }
  
  public TreeNode getChildAt(int index) {
    if(m_typeNode == null) {
      m_typeNode = buildTypeNode();
    }
    
    return m_typeNode;
  }
  
  private TypeNode buildTypeNode() {
    IType type = internalGetType();
    return new TypeNode(type, m_showFields, m_showMethods, m_showTypes);
  }
  
  public String getSignature() {
    return m_signature;
  }

  public String[] getFields() {
    ArrayList<String> list = new ArrayList<String>();
    int childCount = getChildCount();
    
    for(int i = 0; i < childCount; i++) {
      list.addAll(Arrays.asList(((TypeNode)getChildAt(i)).getFields()));
    }
    
    return list.toArray(new String[0]);
  }
  
  public String toString() {
    return m_moduleName;
  }
  
  private IType internalGetType() {
    try {
      return m_classFile.getType();
    } catch(JavaModelException jme) {
      jme.printStackTrace();
      return null;
    }
  }
}
