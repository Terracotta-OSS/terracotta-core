/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.terracotta.dso.BootJarHelper;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.actions.ActionUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class BootJarView extends ViewPart
  implements IResourceChangeListener,
             IResourceDeltaVisitor
{
  public static final String ID_BOOTJAR_VIEW_PART = "org.terracotta.dso.ui.views.bootJarView";

  private TableViewer viewer;
  private IFile bootJarFile;
  
  public BootJarView() {
    super();
    bootJarFile = getBootJarFile();
  }

  public void createPartControl(Composite parent) {
    viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    viewer.setContentProvider(new ViewContentProvider());
    viewer.setLabelProvider(new ViewLabelProvider());
    viewer.setInput(getViewSite());
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
  }

  class ViewContentProvider implements IStructuredContentProvider {
    public void inputChanged(Viewer v, Object oldInput, Object newInput) {/**/}

    public void dispose() {/**/}

    public Object[] getElements(Object parent) {
      ArrayList list = new ArrayList();

      if(bootJarFile == null ||
          !TcPlugin.getDefault().hasTerracottaNature(bootJarFile.getProject())) {
        return new Object[0];
      }

      try {
        String fileName = bootJarFile.getLocation().toOSString();
        ZipFile zipFile = new ZipFile(fileName);
        Enumeration entries = zipFile.entries();
        while(entries.hasMoreElements()) {
          ZipEntry e = (ZipEntry)entries.nextElement();
          String name = e.getName();
          
          if(name.endsWith(".class") && !name.startsWith("com/tc")) {
            name = name.substring(0, name.lastIndexOf('.')).replace('/', '.').replace('$', '.');
            list.add(name);
          }
        }
      } catch(IOException ioe) {
        ioe.printStackTrace();
      }
      
      Collections.sort(list);
      
      return list.toArray();
    }
  }

  class ViewLabelProvider extends LabelProvider implements
      ITableLabelProvider {
    public String getColumnText(Object obj, int index) {
      return getText(obj);
    }

    public Image getColumnImage(Object obj, int index) {
      return getImage(obj);
    }

    public Image getImage(Object obj) {
      return PlatformUI.getWorkbench().getSharedImages().getImage(
          ISharedImages.IMG_OBJ_ELEMENT);
    }
  }

  public void setFocus() {
    viewer.getControl().setFocus();
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
  
  public boolean visit(IResourceDelta delta) {
    if(viewer == null ||
        viewer.getTable().isDisposed() ||
        PlatformUI.getWorkbench().isClosing()) {
      return false;
    }
    
    if((delta.getFlags() & IResourceDelta.DESCRIPTION) != 0) {
      bootJarFile = getBootJarFile();
      viewer.setContentProvider(new ViewContentProvider());
      viewer.setInput(getViewSite());
      return false;
    } else if(delta.getKind() == IResourceDelta.CHANGED) {
      IResource res = delta.getResource();
        
      if(res instanceof IFile) {
        IProject project = res.getProject();

        if(project != null && (delta.getFlags() & IResourceDelta.CONTENT) != 0) {
          bootJarFile = getBootJarFile();
          if(res.equals(bootJarFile)) {
            viewer.setContentProvider(new ViewContentProvider());
            viewer.setInput(getViewSite());
            return false;
          }
        }
      }
    }
    
    return true;
  }
  
  public void resourceChanged(final IResourceChangeEvent event){
    if(event.getType() == IResourceChangeEvent.POST_CHANGE) {
      getSite().getShell().getDisplay().asyncExec(new Runnable() {
        public void run(){
          try {
            event.getDelta().accept(BootJarView.this);
          } catch(CoreException ce) {
            ce.printStackTrace();
          }
        }
      });
    }
  }
}
