package org.terracotta.dso.editors.tree;

import org.eclipse.jdt.core.IJavaProject;

import com.tc.admin.common.XRootNode;
import com.tc.admin.common.XTreeModel;

/**
 * A TreeModel that represent Java projects
 * 
 * Used by the various typed-navigators used by editor choosers:
 *   @see org.terracotta.dso.editors.chooser.MethodNavigator
 *   @see org.terracotta.dso.editors.TypeNavigator
 *   @see org.terracotta.dso.editors.chooser.FieldNavigator
 *   
 * @see org.eclipse.jdt.core.IJavaProject
 * @see javax.swing.tree.DefaultTreeModel
 * @see JavaProjectRoot
 */

public class JavaProjectModel extends XTreeModel {
  public JavaProjectModel(IJavaProject project) {
    this(new JavaProjectRoot(project));
  }
  
  public JavaProjectModel(XRootNode root) {
    super(root);
  }
  
  public JavaProjectModel(
    IJavaProject project,
    boolean      showFields,
    boolean      showMethods,
    boolean      showTypes)
  {
    super(new JavaProjectRoot(project, showFields, showMethods, showTypes));
  }
}


