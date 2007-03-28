/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.tree;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import com.tc.admin.common.XRootNode;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A TreeNode that represents the root of a Java project
 * 
 * @see JavaProjectNode
 * @see JavaProjectModel
 */

public class JavaProjectRoot extends XRootNode {
  private IJavaProject m_project;
  private boolean      m_showFields;
  private boolean      m_showMethods;
  private boolean      m_showTypes;
  
  public JavaProjectRoot(IJavaProject project) {
    this(project, true, true, true);
  }
  
  public JavaProjectRoot(
    IJavaProject project,
    boolean      showFields,
    boolean      showMethods,
    boolean      showTypes)
  {
    super(project);

    m_project     = project;
    m_showFields  = showFields;
    m_showMethods = showMethods;
    m_showTypes   = showTypes;
    
    init();
  }
 
  public void init() {
    IPackageFragmentRoot[] fragmentRoots = getPackageFragmentRoots(m_project);
    IPackageFragmentRoot   fragmentRoot;
      
    for(int i = 0; i < fragmentRoots.length; i++) {
      fragmentRoot = fragmentRoots[i];
      add(new PackageFragmentRootNode(fragmentRoot, m_showFields, m_showMethods, m_showTypes));
    }
  }
  
  private static IPackageFragmentRoot[] getPackageFragmentRoots(IJavaProject project) {
    try {
      return project.getPackageFragmentRoots();
    } catch(JavaModelException jme) {
      return null;
    }
  }
  
  public String getSignature() {
    return "";
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
    return m_project.getElementName();
  }
}
