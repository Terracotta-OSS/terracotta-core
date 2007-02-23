/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.terracotta.dso.BootJarHelper;
import org.terracotta.dso.actions.ActionUtil;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XRootNode;
import com.tc.admin.common.XTree;
import com.tc.admin.common.XTreeModel;
import org.terracotta.dso.editors.tree.PackageFragmentRootNode;

import java.awt.BorderLayout;

import javax.swing.JScrollPane;

public class BootJarPanel extends XContainer
  implements IJavaLaunchConfigurationConstants
{
  public BootJarPanel(IFile bootJarFile) {
    super();
    setLayout(new BorderLayout());
    
    XRootNode            root  = new XRootNode();
    XTreeModel           model = new XTreeModel(root);
    XTree                tree  = new XTree();
    IPackageFragmentRoot pfr   = (IPackageFragmentRoot)JavaCore.create(bootJarFile); 
    
    if(pfr != null) {
      root.add(new PackageFragmentRootNode(pfr));
    }
    tree.setModel(model);

    add(new JScrollPane(tree));
  }
  
  public static IFile getBootJarFile() {
    IWorkbenchWindow window      = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    String           bootJarName = safeGetInstallBootJarName();
    
    if(window != null) {
      ISelection selection = window.getSelectionService().getSelection();
      
      if(selection != null) {
        IJavaProject javaProject  = ActionUtil.locateSelectedJavaProject(selection);

        if(javaProject != null) {
          IProject project      = javaProject.getProject();
          IFile    localBootJar = project.getFile(bootJarName);

          if(localBootJar != null) {
            return localBootJar;
          }
        }
      }
    }
    
    IPath      bootJarPath = BootJarHelper.getHelper().getBootJarPath(bootJarName);
    IWorkspace workspace   = ResourcesPlugin.getWorkspace(); 
    
    return workspace.getRoot().getFileForLocation(bootJarPath);
  }
  
  private static String safeGetInstallBootJarName() {
    try {
      return BootJarHelper.getHelper().getBootJarName();
    } catch(CoreException ce) {
      ce.printStackTrace();
      return null;
    }
  }
}
