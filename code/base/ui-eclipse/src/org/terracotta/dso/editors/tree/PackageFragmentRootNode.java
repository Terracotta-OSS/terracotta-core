/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.tree;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;

/**
 * A TreeNode that represents the root of a Java project
 * 
 * @see JavaProjectNode
 * @see JavaProjectModel
 */

public class PackageFragmentRootNode extends JavaProjectNode {
  private IPackageFragmentRoot m_packageFragmentRoot;
  private IPackageFragment[]   m_packageFragments;
  private String               m_name;
  private boolean              m_showFields;
  private boolean              m_showMethods;
  private boolean              m_showTypes;
  
  public PackageFragmentRootNode(IPackageFragmentRoot packageFragmentRoot) {
    this(packageFragmentRoot, true, true, true);
  }
  
  public PackageFragmentRootNode(
    IPackageFragmentRoot packageFragmentRoot,
    boolean              showFields,
    boolean              showMethods,
    boolean              showTypes)
  {
    super(packageFragmentRoot);

    m_packageFragmentRoot = packageFragmentRoot;
    m_showFields          = showFields;
    m_showMethods         = showMethods;
    m_showTypes           = showTypes;
    
    IPath path = m_packageFragmentRoot.getPath();
    m_name = path.lastSegment();
    
    String ext = path.getFileExtension();
    if(ext != null && (ext.equals("jar") || ext.equals("zip"))) {
      m_name = path.lastSegment() + " - " + path.removeLastSegments(1).toOSString();
    }
  }
  
  public int getChildCount() {
    return children != null ? children.size() : 1;
  }

  public TreeNode getChildAt(int index) {
    if(children == null) {
      children = new Vector();
      children.setSize(ensurePackageFragments().length);
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
  
  public void addChild(int index) {
    IPackageFragment[]  fragments = ensurePackageFragments();
    IPackageFragment    fragment  = fragments[index];
    PackageFragmentNode node;
      
    node = new PackageFragmentNode(fragment, m_showFields, m_showMethods, m_showTypes);
    setChildAt(node, index);
  }
  
  private IPackageFragment[] ensurePackageFragments() {
    if(m_packageFragments == null) {
      m_packageFragments = internalGetPackageFragments();
    }
    
    return m_packageFragments;
  }
  
  private IPackageFragment[] internalGetPackageFragments() {
    try {
      IJavaElement[]     elements = m_packageFragmentRoot.getChildren();
      ArrayList          list     = new ArrayList();
      IJavaElement       element;
      IPackageFragment[] fragments;
      IPackageFragment   fragment;
      
      for(int i = 0; i < elements.length; i++) {
        element = elements[i];
        
        if(element.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
          fragment = (IPackageFragment)element;
          
          if(fragment.containsJavaResources()) {
            list.add(element);
          }
        }
      }
      
      fragments = (IPackageFragment[])list.toArray(new IPackageFragment[0]); 
      return (IPackageFragment[])JavaElementComparable.sort(fragments);
    } catch(JavaModelException jme) {
      return new IPackageFragment[0];
    }
  }

  public String getSignature() {
    return "";
  }
  
  public String[] getFields() {
    ArrayList list       = new ArrayList();
    int       childCount = getChildCount();
    
    for(int i = 0; i < childCount; i++) {
      list.addAll(Arrays.asList(((JavaProjectNode)getChildAt(i)).getFields()));
    }
    
    return (String[])list.toArray(new String[0]);
  }
  
  public String toString() {
    return m_name;
  }
}
