/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.tree;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.terracotta.dso.PatternHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;

/**
 * A TreeNode that represents a Java module (ICompilationUnit).
 * 
 * @see JavaProjectNode
 * @see org.eclipse.jdt.core.ICompilationUnit
 */

public class CompilationUnitNode extends JavaProjectNode {
  private ICompilationUnit m_cu;
  private String           m_signature;
  private String           m_moduleName;
  private boolean          m_showFields;
  private boolean          m_showMethods;
  private boolean          m_showTypes;
  private IType[]          m_types;
  
  public CompilationUnitNode(ICompilationUnit cu) {
    this(cu, true, true, true);
  }
  
  public CompilationUnitNode(
    ICompilationUnit cu,
    boolean          showFields,
    boolean          showMethods,
    boolean          showTypes)
  {
    super(cu);
    
    m_cu          = cu;
    m_moduleName  = cu.getResource().getName();
    m_signature   = showMethods ? PatternHelper.getExecutionPattern(cu.findPrimaryType()) :
                                  PatternHelper.getWithinPattern(cu.findPrimaryType());
    m_showFields  = showFields;
    m_showMethods = showMethods;
    m_showTypes   = showTypes;
  }
  
  public int getChildCount() {
    return children != null ? children.size() : 1;
  }
  
  public TreeNode getChildAt(int index) {
    if(children == null) {
      children = new Vector();
      children.setSize(ensureTypes().length);
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          nodeStructureChanged();
        }
      });
    }

    if(children.elementAt(index) == null) {
      addChild(index);
    }
    
    return super.getChildAt(index);
  }
  
  private void addChild(int index) {
    IType[]  types = ensureTypes();
    IType    type  = types[index];
    TypeNode node;
    
    node = new TypeNode(type, m_showFields, m_showMethods, m_showTypes);
    setChildAt(node, index);
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

  private IType[] ensureTypes() {
    if(m_types == null) {
      m_types = internalGetTypes();
    }
    return m_types;
  }
  
  private IType[] internalGetTypes() {
    try {
      return (IType[])JavaElementComparable.sort(m_cu.getTypes());
    } catch(JavaModelException jme) {
      return new IType[0];
    }
  }
}
