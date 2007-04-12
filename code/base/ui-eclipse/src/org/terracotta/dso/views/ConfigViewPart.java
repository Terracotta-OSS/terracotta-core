/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dnd.DelegatingDropAdapter;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;
import org.eclipse.jdt.internal.ui.viewsupport.SelectionProviderMediator;
import org.eclipse.jdt.internal.ui.viewsupport.StatusBarUpdater;
import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.navigator.LocalSelectionTransfer;
import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.IConfigurationListener;
import org.terracotta.dso.JdtUtils;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.actions.ActionUtil;
import org.terracotta.dso.editors.ConfigurationEditor;

import com.terracottatech.config.QualifiedClassName;
import com.terracottatech.config.QualifiedFieldName;
import com.terracottatech.config.TcConfigDocument.TcConfig;

public class ConfigViewPart extends ViewPart
  implements ISelectionChangedListener,
             IPartListener,
             IMenuListener,
             IConfigurationListener,
             IDoubleClickListener,
             IResourceChangeListener,
             IResourceDeltaVisitor
{
  public static final String ID_CONFIG_VIEW_PART = "org.terracotta.dso.ui.views.configView";
  private static TcPlugin fPlugin = TcPlugin.getDefault();
  ConfigViewer fConfigViewer;
  TcConfig fConfig;
  ConfigRefreshAction fRefreshAction;
  DeleteAction fDeleteAction;
  IncludeActionGroup fIncludeActionGroup;
  LockActionGroup fLockActionGroup;
  private ConfigSelectionProvider fSelectionProviderMediator;
  IJavaProject m_javaProject;

  private static String REFRESH = ActionFactory.REFRESH.getId();
  private static String DELETE = ActionFactory.DELETE.getId();
  
  public ConfigViewPart() {
    super();
    fPlugin.addConfigurationListener(this);
  }

  public void createPartControl(Composite parent) {
    createViewer(parent);
    initDragAndDrop(parent);

    fSelectionProviderMediator = new ConfigSelectionProvider(new StructuredViewer[] {fConfigViewer});
    IStatusLineManager slManager = getViewSite().getActionBars().getStatusLineManager();
    fSelectionProviderMediator.addSelectionChangedListener(new StatusBarUpdater(slManager));
    getSite().setSelectionProvider(fSelectionProviderMediator);

    fConfigViewer.initContextMenu(this, getSite(), fSelectionProviderMediator);
    fConfigViewer.addDoubleClickListener(this);

    makeActions();
    fillViewMenu();
    getSite().getPage().addPartListener(this);
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
  }

  public void menuAboutToShow(IMenuManager menu) {
    fillConfigViewerContextMenu(menu);
  }

  private void createViewer(Composite parent) {
    fConfigViewer = new ConfigViewer(parent, this);

    fConfigViewer.addKeyListener(createKeyListener());
    fConfigViewer.addSelectionChangedListener(this);
  }
  
  private void initDragAndDrop(Composite parent) {
    addDropAdapters(fConfigViewer);

    DropTarget dropTarget = new DropTarget(parent, DND.DROP_LINK | DND.DROP_DEFAULT);
    dropTarget.setTransfer(new Transfer[] {LocalSelectionTransfer.getInstance()});
    dropTarget.addDropListener(new ConfigTransferDropAdapter(this, fConfigViewer));
  }
    
  private void addDropAdapters(StructuredViewer viewer) {
    Transfer[] transfers = new Transfer[] {LocalSelectionTransfer.getInstance()};
    int ops = DND.DROP_LINK | DND.DROP_DEFAULT;
    
    TransferDropTargetListener[] dropListeners = new TransferDropTargetListener[] {
      new ConfigTransferDropAdapter(this, viewer)
    };
    viewer.addDropSupport(ops, transfers, new DelegatingDropAdapter(dropListeners));
  }

  protected void fillConfigViewerContextMenu(IMenuManager menu) {
    JavaPlugin.createStandardGroups(menu);

    menu.appendToGroup(IContextMenuConstants.GROUP_SHOW, fRefreshAction);
    if(fDeleteAction.canActionBeAdded()) {
      menu.appendToGroup(IContextMenuConstants.GROUP_SHOW, fDeleteAction);
    }
    
    fIncludeActionGroup.setContext(new ActionContext(getSelection()));
    fIncludeActionGroup.fillContextMenu(menu);
    fIncludeActionGroup.setContext(null);

    fLockActionGroup.setContext(new ActionContext(getSelection()));
    fLockActionGroup.fillContextMenu(menu);
    fLockActionGroup.setContext(null);
  }
  
  public void setConfig(final TcConfig config) {
    getSite().getShell().getDisplay().asyncExec(new Runnable () {
      public void run() {
        if(config == null) {
          m_javaProject = null;
        }
        fConfigViewer.setConfig(fConfig = config);
        fRefreshAction.setEnabled(config != null);
        fDeleteAction.setEnabled(config != null && fDeleteAction.canActionBeAdded());
      }
    });
  }
  
  private void updateView() {
    fConfigViewer.refresh();
    fConfigViewer.expandAll();
  }

  private KeyListener createKeyListener() {
    KeyListener keyListener = new KeyAdapter() {
      public void keyReleased(KeyEvent event) {
        handleKeyEvent(event);
      }
    };

    return keyListener;
  }

  protected void handleKeyEvent(KeyEvent event) {
    if(event.stateMask == 0) {
      if(event.keyCode == SWT.F5) {
        if((fRefreshAction != null) && fRefreshAction.isEnabled()) {
          fRefreshAction.run();
          return;
        }
      } else if(event.keyCode == Action.findKeyCode("DELETE")) {
        if((fDeleteAction != null) && fDeleteAction.isEnabled()) {
          fDeleteAction.run();
          return;
        }
      }

    }
  }

  private void makeActions() {
    fRefreshAction = new ConfigRefreshAction(this);
    fDeleteAction = new DeleteAction(this);
    fIncludeActionGroup = new IncludeActionGroup(this);
    fLockActionGroup = new LockActionGroup(this);
  }
  
  private void fillViewMenu() {
    IActionBars actionBars = getViewSite().getActionBars();
    IMenuManager viewMenu = actionBars.getMenuManager();
    IToolBarManager toolBar = actionBars.getToolBarManager();
    
    actionBars.setGlobalActionHandler(REFRESH, fRefreshAction);
    actionBars.setGlobalActionHandler(DELETE, fDeleteAction);

    toolBar.add(fRefreshAction);
    toolBar.add(fDeleteAction);

    viewMenu.add(fRefreshAction);
    viewMenu.add(fDeleteAction);
    
    fIncludeActionGroup.fillActionBars(actionBars);
    fLockActionGroup.fillActionBars(actionBars);
  }
  
  public void refresh() {
    updateView();
  }
  
  void removeSelectedItem() {
    ISelection sel = getSelection();
    
    if(!sel.isEmpty()) {
      if(sel instanceof StructuredSelection) {
        IProject project = m_javaProject.getProject();
        ConfigurationHelper configHelper = fPlugin.getConfigurationHelper(project);
        ConfigurationEditor editor = configHelper.getConfigurationEditor();
        StructuredSelection ss = (StructuredSelection)sel;
        Object[] objects = ss.toArray();
        
        for(int i = objects.length-1; i >= 0; i--) {
          Object obj = objects[i];
          
          if(obj instanceof RootWrapper) {
            RootWrapper wrapper = (RootWrapper)obj;
            if(false) {
              wrapper.remove();
              if(editor != null) {
                editor.updateRootsPanel();
              }
            } else {
              configHelper.ensureNotRoot(wrapper.getFieldName());
            }
          } else if(obj instanceof LockWrapper) {
            LockWrapper wrapper = (LockWrapper)obj;
            wrapper.remove();
            if(editor != null) {
              editor.updateLocksPanel();
            }
          } else if(obj instanceof BootClassWrapper) {
            BootClassWrapper wrapper = (BootClassWrapper)obj;
            wrapper.remove();
            if(editor != null) {
              editor.updateBootClassesPanel();
            }
          } else if(obj instanceof TransientFieldWrapper) {
            TransientFieldWrapper wrapper = (TransientFieldWrapper)obj;
            wrapper.remove();
            if(editor != null) {
              editor.updateTransientsPanel();
            }
          } else if(obj instanceof DistributedMethodWrapper) {
            DistributedMethodWrapper wrapper = (DistributedMethodWrapper)obj;
            wrapper.remove();
            if(editor != null) {
              editor.updateDistributedMethodsPanel();
            }
          } else if(obj instanceof IncludeWrapper) {
            IncludeWrapper wrapper = (IncludeWrapper)obj;
            wrapper.remove();
            if(editor != null) {
              editor.updateInstrumentedClassesPanel();
            }
          } else if(obj instanceof ExcludeWrapper) {
            ExcludeWrapper wrapper = (ExcludeWrapper)obj;
            wrapper.remove();
            if(editor != null) {
              editor.updateInstrumentedClassesPanel();
            }
          }
        }
        configHelper.persistConfiguration();
      }
    }
  }
  
  void addRoot(IField field) {
    if(m_javaProject != null) {
      IProject project = m_javaProject.getProject();
      ConfigurationHelper configHelper = fPlugin.getConfigurationHelper(project);
      configHelper.ensureRoot(field);
    }
  }
  
  void addInclude(IJavaElement element) {
    if(m_javaProject != null) {
      IProject project = m_javaProject.getProject();
      ConfigurationHelper configHelper = fPlugin.getConfigurationHelper(project);
      configHelper.ensureAdaptable(element);
    }
  }

  void setIncludeExpression(String classExpression) {
    ISelection selection = getSelection();
    Object element = SelectionUtil.getSingleElement(selection);
    
    if(element instanceof IncludeWrapper) {
      IncludeWrapper wrapper = (IncludeWrapper)element;
      wrapper.setClassExpression(classExpression);

      IProject project = m_javaProject.getProject();
      ConfigurationHelper configHelper = fPlugin.getConfigurationHelper(project);
      ConfigurationEditor editor = configHelper.getConfigurationEditor();

      fPlugin.removeConfigurationListener(this);
      if(editor != null) {
        editor.updateInstrumentedClassesPanel();
      }
      configHelper.persistConfiguration();
      fPlugin.addConfigurationListener(this);
      
      fConfigViewer.update(element, null);
    }
  }
  
  void setHonorTransient(boolean honor) {
    ISelection selection = getSelection();
    Object element = SelectionUtil.getSingleElement(selection);
    
    if(element instanceof IncludeWrapper) {
      IncludeWrapper wrapper = (IncludeWrapper)element;
      wrapper.setHonorTransient(honor);

      IProject project = m_javaProject.getProject();
      ConfigurationHelper configHelper = fPlugin.getConfigurationHelper(project);
      ConfigurationEditor editor = configHelper.getConfigurationEditor();

      fPlugin.removeConfigurationListener(this);
      if(editor != null) {
        editor.updateInstrumentedClassesPanel();
      }
      configHelper.persistConfiguration();
      fPlugin.addConfigurationListener(this);
    }
  }

  void setOnLoad(OnLoadAction action, String handler) {
    ISelection selection = getSelection();
    Object element = SelectionUtil.getSingleElement(selection);
    
    if(element instanceof IncludeWrapper) {
      IncludeWrapper wrapper = (IncludeWrapper)element;
      
      if(action.isNoop()) {
        wrapper.unsetOnLoad();
      } else if(action.isExecute()) {
        wrapper.setOnLoadExecute(handler);
      } else if(action.isMethod()) {
        wrapper.setOnLoadMethod(handler);
      }
      
      IProject project = m_javaProject.getProject();
      ConfigurationHelper configHelper = fPlugin.getConfigurationHelper(project);
      ConfigurationEditor editor = configHelper.getConfigurationEditor();

      fPlugin.removeConfigurationListener(this);
      if(editor != null) {
        editor.updateInstrumentedClassesPanel();
      }
      configHelper.persistConfiguration();
      fPlugin.addConfigurationListener(this);
    }
  }

  void addExclude(IJavaElement element) {
    if(m_javaProject != null) {
      IProject project = m_javaProject.getProject();
      ConfigurationHelper configHelper = fPlugin.getConfigurationHelper(project);
      configHelper.ensureExcluded(element);
    }
  }

  void setLockExpression(String classExpression) {
    ISelection selection = getSelection();
    Object element = SelectionUtil.getSingleElement(selection);
    
    if(element instanceof LockWrapper) {
      LockWrapper wrapper = (LockWrapper)element;
      wrapper.setMethodExpression(classExpression);

      IProject project = m_javaProject.getProject();
      ConfigurationHelper configHelper = fPlugin.getConfigurationHelper(project);
      ConfigurationEditor editor = configHelper.getConfigurationEditor();

      fPlugin.removeConfigurationListener(this);
      if(editor != null) {
        editor.updateLocksPanel();
      }
      configHelper.persistConfiguration();
      fPlugin.addConfigurationListener(this);
      
      fConfigViewer.update(element, null);
    }
  }
  
  void setLockLevel(LockLevelAction action) {
    ISelection selection = getSelection();
    Object element = SelectionUtil.getSingleElement(selection);
    
    if(element instanceof LockWrapper) {
      LockWrapper wrapper = (LockWrapper)element;
      
      wrapper.setLevel(action.getLevel());
      
      IProject project = m_javaProject.getProject();
      ConfigurationHelper configHelper = fPlugin.getConfigurationHelper(project);
      ConfigurationEditor editor = configHelper.getConfigurationEditor();

      fPlugin.removeConfigurationListener(this);
      if(editor != null) {
        editor.updateLocksPanel();
      }
      configHelper.persistConfiguration();
      fPlugin.addConfigurationListener(this);
    }
  }

  void addTransientField(IField field) {
    if(m_javaProject != null) {
      IProject project = m_javaProject.getProject();
      ConfigurationHelper configHelper = fPlugin.getConfigurationHelper(project);
      configHelper.ensureTransient(field);
    }
  }

  void addDistributedMethod(IMethod method) {
    if(m_javaProject != null) {
      IProject project = m_javaProject.getProject();
      ConfigurationHelper configHelper = fPlugin.getConfigurationHelper(project);
      configHelper.ensureDistributedMethod(method);
    }
  }

  void addAdditionalBootJarClass(IType type) {
    if(m_javaProject != null) {
      IProject project = m_javaProject.getProject();
      ConfigurationHelper configHelper = fPlugin.getConfigurationHelper(project);
      configHelper.ensureBootJarClass(type);
    }
  }

  void addAutolock(IJavaElement element) {
    if(m_javaProject != null) {
      IProject project = m_javaProject.getProject();
      ConfigurationHelper configHelper = fPlugin.getConfigurationHelper(project);
      configHelper.ensureAutolocked(element);
    }
  }

  void addNamedLock(IJavaElement element) {
    if(m_javaProject != null) {
      IProject project = m_javaProject.getProject();
      ConfigurationHelper configHelper = fPlugin.getConfigurationHelper(project);
      configHelper.ensureNameLocked(element);
    }
  }

  public void setFocus() {
    fConfigViewer.setFocus();
  }

  IType getType(String typeName) {
    if(m_javaProject != null) {
      try {
        return JdtUtils.findType(m_javaProject, typeName);
      } catch(JavaModelException jme) {/**/}
    }
    return null;
  }
  
  IField getField(String fieldName) {
    if(fieldName != null && m_javaProject != null) {
      IProject project = m_javaProject.getProject();
      ConfigurationHelper configHelper = fPlugin.getConfigurationHelper(project);
      return configHelper.getField(fieldName);
    }

    return null;
  }
  
  public void selectionChanged(SelectionChangedEvent event) {
    ISelection sel = event.getSelection();
    
    fDeleteAction.setEnabled(fDeleteAction.canActionBeAdded());
    
    if(!sel.isEmpty()) {
      if(sel instanceof StructuredSelection) {
        StructuredSelection ss = (StructuredSelection)sel;
        
        if(ss.size() == 1) {
          Object obj = ss.getFirstElement();
          IMember member = null;
          
          if(obj instanceof RootWrapper) {
            member = getField(((RootWrapper)obj).getFieldName());
          } else if(obj instanceof QualifiedFieldName) {
            member = getField(((QualifiedFieldName)obj).getStringValue());
          } else if(obj instanceof QualifiedClassName) {
            member = getType(((QualifiedClassName)obj).getStringValue());
          } else if(obj instanceof BootClassWrapper) {
            member = getType(((BootClassWrapper)obj).getClassName());
          } else if(obj instanceof TransientFieldWrapper) {
            member = getField(((TransientFieldWrapper)obj).getFieldName());
          }
          
          if(member != null) {
            ConfigUI.jumpToMember(member);
          }
        }
      }
    }
  }
  
  public void dispose() {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
    if(fIncludeActionGroup != null) {
      fIncludeActionGroup.dispose();
    }
    if(fLockActionGroup != null) {
      fLockActionGroup.dispose();
    }
    fPlugin.removeConfigurationListener(this);
    getViewSite().getPage().removePartListener(this);
    super.dispose();
  }

  public TcConfig getConfig() {
    if(m_javaProject != null) {
      return fPlugin.getConfiguration(m_javaProject.getProject());
    }
    return null;
  }

  protected ISelection getSelection() {
    StructuredViewer viewerInFocus= fSelectionProviderMediator.getViewerInFocus();
    if(viewerInFocus != null) {
      return viewerInFocus.getSelection();
    }
    return StructuredSelection.EMPTY;
  }
  
  private class ConfigSelectionProvider extends SelectionProviderMediator {
    public ConfigSelectionProvider(StructuredViewer[] viewers) {
      super(viewers, null);
    }

    public ISelection getSelection() {
      ISelection selection = super.getSelection();
      if(false && !selection.isEmpty()) {
        return ConfigUI.convertSelection(selection);
      }
      return selection;
    }
  }
  
  private void initFromJavaProject(IJavaProject javaProject) {
    if(javaProject == null || !javaProject.equals(m_javaProject)) {
      TcConfig config = null;
      
      if((m_javaProject = javaProject) != null) {
        IProject project = javaProject.getProject();
        
        if(TcPlugin.getDefault().hasTerracottaNature(project)) {
          config = fPlugin.getConfiguration(project);
        }
      }
      setConfig(config);
    }
  }

  public void partActivated(IWorkbenchPart part) {
    if(part != this) {
      IWorkbenchWindow window = part.getSite().getWorkbenchWindow();
      
      if(window != null) {
        ISelection selection = window.getSelectionService().getSelection();
        
        if(selection != null) {
          initFromJavaProject(ActionUtil.locateSelectedJavaProject(selection));
        } else {
          //setConfig(null);
        }
      }
    }
  }

  public void partBroughtToTop(IWorkbenchPart part) {/**/}
  public void partClosed(IWorkbenchPart part) {/**/}
  public void partDeactivated(IWorkbenchPart part) {/**/}

  public void partOpened(IWorkbenchPart part) {
    if(part == this) {
      IWorkbenchWindow window = part.getSite().getWorkbenchWindow();
      
      if(window != null) {
        ISelection selection = window.getSelectionService().getSelection();
        
        if(selection != null) {
          initFromJavaProject(ActionUtil.locateSelectedJavaProject(selection));
        } else {
          //setConfig(null);
        }
      }
    }
  }
  
  public void configurationChanged(IProject project) {
    if(TcPlugin.getDefault().hasTerracottaNature(project)) {
      if(m_javaProject != null && m_javaProject.getProject().equals(project)) {
        m_javaProject = JavaCore.create(project);
        setConfig(fPlugin.getConfiguration(project));
      }
    } else {
      setConfig(null);
    }
  }

  public void doubleClick(DoubleClickEvent event) {/**/}
  
  public boolean visit(IResourceDelta delta) {
    if(fConfigViewer == null ||
       fConfigViewer.getTree().isDisposed() ||
       PlatformUI.getWorkbench().isClosing()) {
      return false;
    }
    
    final IProject project;
    if((project = isAffected(delta)) != null) {
      Display.getDefault().asyncExec(new Runnable() {
        public void run() {
          m_javaProject = JavaCore.create(project);
          setConfig(fPlugin.getConfiguration(project));
        }
      });
      return false;
    }
    
    return true;
  }
  
  private IProject isAffected(IResourceDelta delta) {
    IResource res = delta.getResource();
    IProject project = null;
    
    if(res instanceof IProject) {
      if(delta.getKind() == IResourceDelta.ADDED ||
         (delta.getFlags() & IResourceDelta.DESCRIPTION) != 0)
      {
        project = (IProject)delta.getResource();
        return TcPlugin.getDefault().hasTerracottaNature(project) ? project : null;
      }
    }
    
    IResourceDelta[] children = delta.getAffectedChildren();
    for(int i = 0; i < children.length; i++) {
      if((project = isAffected(children[i])) != null) {
        return project;
      }
    }

    return null;
  }
  
  public void resourceChanged(final IResourceChangeEvent event){
    switch(event.getType()) {
      case IResourceChangeEvent.POST_CHANGE:
        try {
          event.getDelta().accept(this);
        } catch(CoreException ce) {
          ce.printStackTrace();
        }
        break;
      case IResourceChangeEvent.PRE_DELETE:
      case IResourceChangeEvent.PRE_CLOSE: {
        setConfig(null);
        break;
      }
    }
  }
}
