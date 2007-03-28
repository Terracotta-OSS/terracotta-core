/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.tree;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.terracotta.dso.PatternHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;

/**
 * A TreeNode that represents a Java package root.
 * 
 * @see JavaProjectNode
 * @see org.eclipse.jdt.core.IPackageFragment
 */

public class PackageFragmentNode extends JavaProjectNode {
  private IPackageFragment   m_fragment;
  private String             m_signature;
  private String             m_fragmentName;
  private boolean            m_showFields;
  private boolean            m_showMethods;
  private boolean            m_showTypes;
  private ICompilationUnit[] m_cus;
  private IClassFile[]       m_classFiles;
  
  public PackageFragmentNode(IPackageFragment fragment) {
    this(fragment, true, true, true);
  }
  
  public PackageFragmentNode(
    IPackageFragment fragment,
    boolean          showFields,
    boolean          showMethods,
    boolean          showTypes)
  {
    super(fragment);
    
    m_fragment     = fragment;
    m_fragmentName = m_fragment.getElementName();
    m_signature    = showMethods ? PatternHelper.getExecutionPattern(fragment) :
                                   PatternHelper.getWithinPattern(fragment);
    m_showFields   = showFields;
    m_showMethods  = showMethods;
    m_showTypes    = showTypes;
  }
  
  public int getChildCount() {
    return children != null ? children.size() : 1;
  }
  
  private int determineChildCount() {
    return isSourceFragment() ?
        ensureCompilationUnits().length :
        ensureClassFiles().length;
  }
  
  public TreeNode getChildAt(int index) {
    if(children == null) {
      children = new Vector();
      children.setSize(determineChildCount());
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
    ProjectNode node;
    
    if(isSourceFragment()) {
      ICompilationUnit[]  cus = ensureCompilationUnits();
      ICompilationUnit    cu  = cus[index];
        
      node = new CompilationUnitNode(cu, m_showFields, m_showMethods, m_showTypes);
      setChildAt(node, index);
    }
    else {
      IClassFile[]  classFiles = ensureClassFiles();
      IClassFile    classFile  = classFiles[index];
        
      node = new ClassFileNode(classFile, m_showFields, m_showMethods, m_showTypes);
      setChildAt(node, index);
    }
  }
  
  private boolean isSourceFragment() {
    try {
      return m_fragment.getKind() == IPackageFragmentRoot.K_SOURCE;
    } catch(JavaModelException jme) {
      return false;
    }
  }
  
  public String getSignature() {
    return m_signature;
  }
  
  public String[] getFields() {
    ArrayList<String> list = new ArrayList<String>();
    int childCount = getChildCount();
    
    for(int i = 0; i < childCount; i++) {
      list.addAll(Arrays.asList(((JavaProjectNode)getChildAt(i)).getFields()));
    }
    
    return list.toArray(new String[0]);
  }
  
  public String toString() {
    return m_fragmentName;
  }
  
  private ICompilationUnit[] ensureCompilationUnits() {
    if(m_cus == null) {
      m_cus = internalGetCompilationUnits();
    }
    
    return m_cus;
  }
  
  private ICompilationUnit[] internalGetCompilationUnits() {
    try {
      ICompilationUnit[] cus = m_fragment.getCompilationUnits();
      return (ICompilationUnit[])JavaElementComparable.sort(cus);
    } catch(JavaModelException jme) {
      jme.printStackTrace();
      return new ICompilationUnit[0];
    }
  }
  
  private IClassFile[] ensureClassFiles() {
    if(m_classFiles == null) {
      m_classFiles = internalGetClassFiles();
    }
    
    return m_classFiles;
  }
  
  private IClassFile[] internalGetClassFiles() {
    try {
      IClassFile[]          classFiles = m_fragment.getClassFiles();
      ArrayList<IClassFile> list       = new ArrayList<IClassFile>();
      IType                 type;
      int                   flags;
      
      for(int i = 0; i < classFiles.length; i++) {
        type  = classFiles[i].getType();
        flags = type.getFlags();
        
        if(!type.isInterface()       &&
           !type.isAnonymous()       &&
           !type.isMember()          &&
           !type.isEnum()            &&
           !type.isAnnotation()      &&
           !Flags.isPrivate(flags)   &&
           !Flags.isProtected(flags) &&
           !Flags.isStatic(flags))
        {
          list.add(classFiles[i]);
        }
      }

      classFiles = list.toArray(new IClassFile[0]);
      return (IClassFile[])JavaElementComparable.sort(classFiles);
    } catch(JavaModelException jme) {
      jme.printStackTrace();
      return new IClassFile[0];
    }
  }
}
