/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

import org.apache.xmlbeans.XmlOptions;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.navigator.LocalSelectionTransfer;
import org.terracotta.dso.TcPlugin;
import org.terracotta.ui.util.SelectionUtil;

import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

class ConfigTransferDropAdapter extends ViewerDropAdapter {
  private ConfigViewPart fPart;
  private List           fElements;
  private ISelection     fSelection;
  private String         fLastDropDescription;

  public ConfigTransferDropAdapter(ConfigViewPart viewPart, StructuredViewer viewer) {
    super(viewer);
    setSelectionFeedbackEnabled(true);
    setFeedbackEnabled(true);
    fPart = viewPart;
  }

  /**
   * {@inheritDoc}
   */
  public void dragEnter(DropTargetEvent event) {
    clear();
    super.dragEnter(event);
  }

  /**
   * {@inheritDoc}
   */
  public void dragLeave(DropTargetEvent event) {
    clear();
    super.dragLeave(event);
  }

  private void clear() {
    setSelectionFeedbackEnabled(false);
    fElements = null;
    fSelection = null;
  }

  protected void initializeSelection() {
    if (fElements != null) return;
    ISelection s = LocalSelectionTransfer.getInstance().getSelection();
    if (!(s instanceof IStructuredSelection)) {
      fSelection = StructuredSelection.EMPTY;
      fElements = Collections.EMPTY_LIST;
      return;
    }
    fSelection = s;
    fElements = ((IStructuredSelection) s).toList();
  }

  protected ISelection getSelection() {
    return fSelection;
  }

  public boolean validateDrop(Object target, int operation, TransferData transferData) {
    initializeSelection();

    IJavaElement[] inputElements = getInputElements(getSelection());
    if (inputElements != null && inputElements.length > 0) {
      switch (inputElements[0].getElementType()) {
        case IJavaElement.FIELD:
          if (target instanceof RootsWrapper || target instanceof RootWrapper
              || target instanceof TransientFieldsWrapper || target instanceof TransientFieldWrapper) { return true; }
          break;
        case IJavaElement.PACKAGE_DECLARATION:
        case IJavaElement.PACKAGE_FRAGMENT:
          if (target instanceof AutolocksWrapper || target instanceof AutolockWrapper
              || target instanceof NamedLocksWrapper || target instanceof NamedLockWrapper
              || target instanceof IncludesWrapper || target instanceof IncludeWrapper
              || target instanceof ExcludesWrapper || target instanceof ExcludeWrapper) { return true; }
          break;
        case IJavaElement.METHOD:
          if (target instanceof DistributedMethodsWrapper || target instanceof DistributedMethodWrapper
              || target instanceof AutolocksWrapper || target instanceof AutolockWrapper
              || target instanceof NamedLocksWrapper || target instanceof NamedLockWrapper) { return true; }
          break;
        case IJavaElement.CLASS_FILE:
        case IJavaElement.COMPILATION_UNIT:
        case IJavaElement.TYPE:
          if (target instanceof AdditionalBootJarClassesWrapper || target instanceof AutolocksWrapper
              || target instanceof AutolockWrapper || target instanceof NamedLocksWrapper
              || target instanceof NamedLockWrapper || target instanceof IncludesWrapper
              || target instanceof IncludeWrapper || target instanceof ExcludesWrapper
              || target instanceof ExcludeWrapper) { return true; }
          break;
      }
    }
    return false;
  }

  public boolean isEnabled(DropTargetEvent event) {
    return true;
  }

  public void drop(DropTargetEvent event) {
    event.detail = DND.DROP_COPY;
    super.drop(event);
    event.detail = DND.DROP_NONE;
  }

  public boolean performDrop(Object data) {
    IProject project = fPart.m_javaProject.getProject();
    TcPlugin plugin = TcPlugin.getDefault();

    if (plugin.getConfiguration(project) == TcPlugin.BAD_CONFIG) {
      Shell shell = Display.getDefault().getActiveShell();
      String title = "Terracotta Plugin";
      String msg = "The configuration source is not parsable and cannot be\n used until these errors are resolved.";

      MessageDialog.openWarning(shell, title, msg);
      try {
        plugin.openConfigurationEditor(project);
      } catch (Exception e) {
        // TODO:
      }
      return false;
    }

    IWorkbench workbench = PlatformUI.getWorkbench();
    IOperationHistory operationHistory = workbench.getOperationSupport().getOperationHistory();
    IUndoContext undoContext = workbench.getOperationSupport().getUndoContext();
    DropOperation operation = new DropOperation();
    operation.addContext(undoContext);
    try {
      IStatus result = operationHistory.execute(operation, null, null);
      return result == Status.OK_STATUS;
    } catch (ExecutionException ee) {
      TcPlugin.getDefault().openError("Executing DropOperation", ee);
    }
    return false;
  }

  private <E> E[] toArray(List elements, Class<E> filterType) {
    Iterator<?> iter = elements.iterator();
    List<E> l = new ArrayList<E>();
    while (iter.hasNext()) {
      Object o = iter.next();
      if (filterType.isAssignableFrom(o.getClass())) {
        l.add((E) o);
      }
    }
    return l.toArray((E[]) Array.newInstance(filterType, 0));
  }

  private boolean handleDrop() {
    List list = SelectionUtil.toList(getSelection());
    Object target = getCurrentTarget();
    IJavaElement element = (IJavaElement) list.get(0);
    switch (element.getElementType()) {
      case IJavaElement.FIELD:
        if (target instanceof RootsWrapper || target instanceof RootWrapper) {
          fPart.addRoots(toArray(list, IField.class));
          fLastDropDescription = "Add Shared Roots";
          return true;
        } else if (target instanceof TransientFieldsWrapper || target instanceof TransientFieldWrapper) {
          fPart.addTransientFields(toArray(list, IField.class));
          fLastDropDescription = "Add Transient Fields";
          return true;
        }
        break;
      case IJavaElement.PACKAGE_DECLARATION:
      case IJavaElement.PACKAGE_FRAGMENT:
        if (target instanceof AutolocksWrapper || target instanceof AutolockWrapper) {
          fPart.addAutolocks(toArray(list, IJavaElement.class));
          fLastDropDescription = "Add Autolocks";
          return true;
        } else if (target instanceof NamedLocksWrapper || target instanceof NamedLockWrapper) {
          fPart.addNamedLocks(toArray(list, IJavaElement.class));
          fLastDropDescription = "Add Named Locks";
          return true;
        } else if (target instanceof IncludesWrapper || target instanceof IncludeWrapper) {
          fPart.addIncludes(toArray(list, IJavaElement.class));
          fLastDropDescription = "Add Instrumented Types";
          return true;
        } else if (target instanceof ExcludesWrapper || target instanceof ExcludeWrapper) {
          fPart.addExcludes(toArray(list, IJavaElement.class));
          fLastDropDescription = "Add Excluded Types";
          return true;
        }
        break;
      case IJavaElement.METHOD:
        if (target instanceof DistributedMethodsWrapper || target instanceof DistributedMethodWrapper) {
          fPart.addDistributedMethods(toArray(list, IMethod.class));
          fLastDropDescription = "Add Distributed Methods";
          return true;
        } else if (target instanceof AutolocksWrapper || target instanceof AutolockWrapper) {
          fPart.addAutolocks(toArray(list, IMethod.class));
          fLastDropDescription = "Add Autolocks";
          return true;
        } else if (target instanceof NamedLocksWrapper || target instanceof NamedLockWrapper) {
          fPart.addNamedLocks(toArray(list, IMethod.class));
          fLastDropDescription = "Add Named Locks";
          return true;
        }
        break;
      case IJavaElement.CLASS_FILE:
        try {
          if (target instanceof AdditionalBootJarClassesWrapper) {
            IType[] types = new IType[list.size()];
            for (int i = 0; i < list.size(); i++) {
              types[i] = ((IClassFile) list.get(i)).getType();
            }
            fPart.addAdditionalBootJarClasses(types);
            fLastDropDescription = "Add BootJar Types";
            return true;
          } else if (target instanceof AutolocksWrapper || target instanceof AutolockWrapper) {
            fPart.addAutolocks(toArray(list, IJavaElement.class));
            fLastDropDescription = "Add Autolocks";
            return true;
          } else if (target instanceof NamedLocksWrapper || target instanceof NamedLockWrapper) {
            fPart.addNamedLocks(toArray(list, IJavaElement.class));
            fLastDropDescription = "Add Named Locks";
            return true;
          } else if (target instanceof IncludesWrapper || target instanceof IncludeWrapper) {
            fPart.addIncludes(toArray(list, IJavaElement.class));
            fLastDropDescription = "Add Instrumented Types";
            return true;
          } else if (target instanceof ExcludesWrapper || target instanceof ExcludeWrapper) {
            fPart.addExcludes(toArray(list, IJavaElement.class));
            fLastDropDescription = "Add Excluded Types";
            return true;
          }
        } catch (Exception e) {/**/
        }
        break;
      case IJavaElement.COMPILATION_UNIT:
        if (target instanceof AdditionalBootJarClassesWrapper) {
          IType[] types = new IType[list.size()];
          for (int i = 0; i < list.size(); i++) {
            types[i] = ((ICompilationUnit) list.get(i)).findPrimaryType();
          }
          fPart.addAdditionalBootJarClasses(types);
          fLastDropDescription = "Add BootJar Types";
          return true;
        } else if (target instanceof AutolocksWrapper || target instanceof AutolockWrapper) {
          fPart.addAutolocks(toArray(list, IJavaElement.class));
          fLastDropDescription = "Add Autolocks";
          return true;
        } else if (target instanceof NamedLocksWrapper || target instanceof NamedLockWrapper) {
          fPart.addNamedLocks(toArray(list, IJavaElement.class));
          fLastDropDescription = "Add Named Locks";
          return true;
        } else if (target instanceof IncludesWrapper || target instanceof IncludeWrapper) {
          fPart.addIncludes(toArray(list, IJavaElement.class));
          fLastDropDescription = "Add Instrumented Types";
          return true;
        } else if (target instanceof ExcludesWrapper || target instanceof ExcludeWrapper) {
          fPart.addExcludes(toArray(list, IJavaElement.class));
          fLastDropDescription = "Add Excluded Types";
          return true;
        }
        break;
      case IJavaElement.TYPE:
        if (target instanceof AdditionalBootJarClassesWrapper) {
          fPart.addAdditionalBootJarClasses(toArray(list, IType.class));
          fLastDropDescription = "Add BootJar Types";
          return true;
        } else if (target instanceof AutolocksWrapper || target instanceof AutolockWrapper) {
          fPart.addAutolocks(toArray(list, IType.class));
          fLastDropDescription = "Add Autolocks";
          return true;
        } else if (target instanceof NamedLocksWrapper || target instanceof NamedLockWrapper) {
          fPart.addNamedLocks(toArray(list, IType.class));
          fLastDropDescription = "Add Named Locks";
          return true;
        } else if (target instanceof IncludesWrapper || target instanceof IncludeWrapper) {
          fPart.addIncludes(toArray(list, IType.class));
          fLastDropDescription = "Add Instrumented Types";
          return true;
        } else if (target instanceof ExcludesWrapper || target instanceof ExcludeWrapper) {
          fPart.addExcludes(toArray(list, IType.class));
          fLastDropDescription = "Add Excluded Types";
          return true;
        }
        break;
    }
    return false;
  }

  private static IJavaElement[] getInputElements(ISelection selection) {
    List list = SelectionUtil.toList(selection);
    if (list == null) return null;
    return getCandidates(list);
  }

  public static IJavaElement[] getCandidates(List input) {
    Iterator iter = input.iterator();
    int type = -1;

    while (iter.hasNext()) {
      Object element = iter.next();
      if (!(element instanceof IJavaElement)) { return null; }
      IJavaElement javaElement = (IJavaElement) element;
      int elementType = javaElement.getElementType();
      if (type == -1) {
        type = elementType;
      } else if (type != elementType) { return null; }
    }

    return (IJavaElement[]) input.toArray(new IJavaElement[0]);
  }

  private class DropOperation extends AbstractOperation {
    TcConfig fPreConfig;
    TcConfig fPostConfig;

    DropOperation() {
      super("Drop operation");
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
      fPreConfig = (TcConfig) fPart.getConfig().copy();
      if (handleDrop()) {
        fPostConfig = (TcConfig) fPart.getConfig().copy();
        setLabel(fLastDropDescription);
        return Status.OK_STATUS;
      } else {
        return Status.CANCEL_STATUS;
      }
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
      try {
        setConfig(fPostConfig);
      } catch (Exception e) {
        throw new ExecutionException("Redoing drop operation", e);
      }
      return Status.OK_STATUS;
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
      try {
        setConfig(fPreConfig);
      } catch (Exception e) {
        throw new ExecutionException("Undoing drop operation", e);
      }
      return Status.OK_STATUS;
    }

    private void setConfig(TcConfig config) throws Exception {
      TcPlugin plugin = TcPlugin.getDefault();
      IJavaProject javaProject = fPart.getJavaProject();
      XmlOptions xmlOpts = plugin.getXmlOptions();
      TcConfigDocument doc = TcConfigDocument.Factory.newInstance(xmlOpts);
      doc.setTcConfig(config);
      plugin.setConfigurationFromString(javaProject.getProject(), plugin.configDocumentAsString(doc));
    }
  }

}
