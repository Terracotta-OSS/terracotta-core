/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;
import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.IConfigurationListener;
import org.terracotta.dso.Messages;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.xml.XMLEditor;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;

import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.text.MessageFormat;

public class ConfigurationEditor extends MultiPageEditorPart
  implements IResourceChangeListener,
             IGotoMarker,
             XmlObjectStructureListener
  {
  private static final int         XML_EDITOR_PAGE_INDEX      = 0;
  private static final int         DSO_APPLICATION_PAGE_INDEX = 1;
  private static final int         SERVERS_PAGE_INDEX         = 2;
  private static final int         CLIENT_PAGE_INDEX          = 3;

  private IProject                 m_project;
  private DsoApplicationPanel      m_dsoAppPanel;
  private ServersPanel             m_serversPanel;
  private ClientsPanel             m_clientsPanel;
  private XMLEditor                m_xmlEditor;
  private int                      m_xmlEditorPageIndex;
  private boolean                  m_haveActiveConfig;
  private DocumentListener         m_docListener;
  private ElementStateListener     m_elementStateListener;
  private TextInputListener        m_textInputListener;
  private Runnable                 m_parseTimer;
  private boolean                  m_syncXmlText;
  private Display                  m_display;
  private IConfigurationListener   m_configAdapter;
  
  public ConfigurationEditor() {
    super();

    m_docListener = new DocumentListener();
    m_elementStateListener = new ElementStateListener();
    m_textInputListener = new TextInputListener();
    m_parseTimer = new ParseTimer();
    m_display = Display.getDefault();
    m_configAdapter = new ConfigAdapter();
  }

  protected void pageChange(final int newPageIndex) {
    if (newPageIndex != 0) {
      if (m_project != null && m_project.isOpen()) {
        TcPlugin plugin = TcPlugin.getDefault();
        TcConfig config = plugin.getConfiguration(m_project);

        if (config == TcPlugin.BAD_CONFIG) {
          Display.getDefault().syncExec(new Runnable() {
            public void run() {
              getControl(newPageIndex).setVisible(false);

              Shell shell = Display.getDefault().getActiveShell();
              String title = "Terracotta Config Editor";
              String msg = "The source page has errors. The other pages cannot be\nused until these errors are resolved.";

              MessageDialog.openWarning(shell, title, msg);
            }
          });
          setActivePage(0);
          return;
        }
      }
    }
    super.pageChange(newPageIndex);
  }
  
  class ConfigAdapter implements IConfigurationListener {
    private void update(boolean initPanels) {
      if(!m_syncXmlText) {
        syncXmlDocument();
      }
      if(initPanels) {
        initPanels();
      }
      internalSetDirty(Boolean.TRUE);
    }
    
    private void handleUpdate(final boolean updatePanels) {
      if(Display.getCurrent() != null) {
        update(updatePanels);
        return;
      }
      asyncExec(new Runnable() {
        public void run() {
          update(updatePanels);
        }
      });
    }
    
    public void configurationChanged(IProject project) {
      if(TcPlugin.getDefault().hasTerracottaNature(project)) {
        if(m_project != null && m_project.equals(project)) {
          handleUpdate(true);
        }
      }
    }
    
    private void handleChange(IProject project, boolean updatePanels) {
      if(TcPlugin.getDefault().hasTerracottaNature(project)) {
        if(m_project != null && m_project.equals(project)) {
          handleUpdate(updatePanels);
        }
      }
    }
    
    public void serverChanged(IProject project, int index) {handleChange(project, false);}
    public void serversChanged(IProject project) {handleChange(project, true);}
      
    public void rootChanged(IProject project, int index) {handleChange(project, false);}
    public void rootsChanged(IProject project) {handleChange(project, true);}

    public void distributedMethodsChanged(IProject project) {handleChange(project, true);}
    public void distributedMethodChanged(IProject project, int index) {handleChange(project, false);}

    public void bootClassesChanged(IProject project) {handleChange(project, true);}
    public void bootClassChanged(IProject project, int index) {handleChange(project, false);}

    public void transientFieldsChanged(IProject project) {handleChange(project, true);}
    public void transientFieldChanged(IProject project, int index) {handleChange(project, false);}

    public void autolockChanged(IProject project, int index) {handleChange(project, false);}
    public void autolocksChanged(IProject project) {handleChange(project, true);}
    public void namedLockChanged(IProject project, int index) {handleChange(project, false);}
    public void namedLocksChanged(IProject project) {handleChange(project, true);}
      
    public void includeRuleChanged(IProject project, int index) {handleChange(project, false);}
    public void includeRulesChanged(IProject project) {handleChange(project, true);}
    public void excludeRuleChanged(IProject project, int index) {handleChange(project, false);}
    public void excludeRulesChanged(IProject project) {handleChange(project, true);}
    public void instrumentationRulesChanged(IProject project) {handleChange(project, true);}

    public void clientChanged(IProject project) {handleChange(project, false);}
    public void moduleReposChanged(IProject project) {handleChange(project, true);}
    public void moduleRepoChanged(IProject project, int index) {handleChange(project, false);}
    public void moduleChanged(IProject project, int index) {handleChange(project, false);}
    public void modulesChanged(IProject project) {handleChange(project, true);}
  }
  
  private void setTimer(boolean start) {
    if (start) m_display.timerExec(1000, m_parseTimer);
    else m_display.timerExec(-1, m_parseTimer);
  }

  private class ParseTimer implements Runnable {
    public void run() {
      m_syncXmlText = true;
      syncXmlModel();
      m_syncXmlText = false;
    }
  }

  void createDsoApplicationPage(int pageIndex) {
    addPage(pageIndex, m_dsoAppPanel = new DsoApplicationPanel(getContainer(), SWT.NONE));
    setPageText(pageIndex, "DSO config");
    m_dsoAppPanel.addXmlObjectStructureListener(this);
  }

  public DsoApplicationPanel getDsoApplicationPanel() {
    return m_dsoAppPanel;
  }

  public void showDsoApplicationPanel() {
    setActivePage(0);
  }

  void createServersPage(int pageIndex) {
    ScrolledComposite scroll = new ScrolledComposite(getContainer(), SWT.V_SCROLL | SWT.H_SCROLL);
    scroll.setContent(m_serversPanel = new ServersPanel(scroll, SWT.NONE));
    scroll.setExpandHorizontal(true);
    scroll.setExpandVertical(true);
    scroll.setMinSize(m_serversPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    addPage(pageIndex, scroll);
    setPageText(pageIndex, "Servers config");
    m_serversPanel.addXmlObjectStructureListener(this);
  }

  void createClientPage(int pageIndex) {
    ScrolledComposite scroll = new ScrolledComposite(getContainer(), SWT.V_SCROLL | SWT.H_SCROLL);
    scroll.setContent(m_clientsPanel = new ClientsPanel(scroll, SWT.NONE));
    scroll.setExpandHorizontal(true);
    scroll.setExpandVertical(true);
    scroll.setMinSize(m_clientsPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    addPage(pageIndex, scroll);
    setPageText(pageIndex, "Clients config");
    m_clientsPanel.addXmlObjectStructureListener(this);
  }

  void createXMLEditorPage(int pageIndex) {
    try {
      IEditorInput input = getEditorInput();

      m_xmlEditor = new XMLEditor() {
        public void doSave(IProgressMonitor progressMonitor) {
          ConfigurationEditor.this.doSave(progressMonitor);
        }
      };
      addPage(m_xmlEditorPageIndex = pageIndex, m_xmlEditor, input);
      setPageText(m_xmlEditorPageIndex, m_xmlEditor.getTitle());

      m_xmlEditor.addTextInputListener(m_textInputListener);
      m_xmlEditor.getDocument().addDocumentListener(m_docListener);
      m_xmlEditor.getDocumentProvider().addElementStateListener(m_elementStateListener);
    } catch (PartInitException e) {
      ErrorDialog.openError(getSite().getShell(), "Error creating nested text editor", null, e.getStatus());
    }
  }

  public boolean isActiveConfig() {
    TcPlugin plugin = TcPlugin.getDefault();
    IFile configFile = plugin.getConfigurationFile(m_project);
    IFileEditorInput fileEditorInput = (FileEditorInput) getEditorInput();
    IFile file = fileEditorInput.getFile();

    return file != null && file.equals(configFile) && plugin.hasTerracottaNature(m_project);
  }

  boolean haveActiveConfig() {
    return m_haveActiveConfig;
  }

  protected void createPages() {
    createXMLEditorPage(XML_EDITOR_PAGE_INDEX);
    if (haveActiveConfig()) {
      createDsoApplicationPage(DSO_APPLICATION_PAGE_INDEX);
      createServersPage(SERVERS_PAGE_INDEX);
      createClientPage(CLIENT_PAGE_INDEX);
      ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
      initPanels();
      TcPlugin.getDefault().addConfigurationListener(m_configAdapter);
    }
  }

  public void structureChanged(XmlObjectStructureChangeEvent e) {
    // This means that either the servers, clients, or dsoApplication elements were
    // removed.
  }

  public void dispose() {
    if (haveActiveConfig()) {
      m_serversPanel.removeXmlObjectStructureListener(this);
      m_clientsPanel.removeXmlObjectStructureListener(this);
      m_dsoAppPanel.removeXmlObjectStructureListener(this);
      ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
      TcPlugin.getDefault().removeConfigurationListener(m_configAdapter);
    }
    super.dispose();
  }

  public void doSave(IProgressMonitor monitor) {
    if (haveActiveConfig()) setTimer(false);
    TcPlugin.getDefault().ignoreNextConfigChange();
    m_xmlEditor.doSaveWork(monitor);
    m_syncXmlText = true;
    syncXmlModel();
    m_syncXmlText = false;
    clearDirty();
  }

  public void doSaveAs() {
    performSaveAs(null);
  }

  protected void performSaveAs(IProgressMonitor progressMonitor) {
    Shell shell = getSite().getShell();
    IEditorInput input = getEditorInput();
    SaveAsDialog dialog = new SaveAsDialog(shell);
    IFile original = null;

    if (input instanceof IFileEditorInput) {
      if ((original = ((IFileEditorInput) input).getFile()) != null) {
        dialog.setOriginalFile(original);
      }
    }

    dialog.create();

    IDocumentProvider provider = m_xmlEditor.getDocumentProvider();
    if (provider == null) {
      // editor has programmatically been closed while the dialog was open
      return;
    }

    if (provider.isDeleted(input) && original != null) {
      String message = MessageFormat.format(Messages.Editor_warning_save_delete, new Object[] { original.getName() });

      dialog.setErrorMessage(null);
      dialog.setMessage(message, IMessageProvider.WARNING);
    }

    if (dialog.open() == Window.CANCEL) {
      if (progressMonitor != null) {
        progressMonitor.setCanceled(true);
      }
      return;
    }

    IPath filePath = dialog.getResult();
    if (filePath == null) {
      if (progressMonitor != null) {
        progressMonitor.setCanceled(true);
      }
      return;
    }

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IFile file = workspace.getRoot().getFile(filePath);
    final IEditorInput newInput = new FileEditorInput(file);
    boolean success = false;

    try {
      provider.aboutToChange(newInput);
      provider.saveDocument(progressMonitor, newInput, provider.getDocument(input), true);
      success = true;
      clearDirty();
    } catch (CoreException ce) {
      IStatus status = ce.getStatus();

      if (status == null || status.getSeverity() != IStatus.CANCEL) {
        String title = Messages.Editor_error_save_title;
        String msg = MessageFormat.format(Messages.Editor_error_save_message, new Object[] { ce.getMessage() });

        if (status != null) {
          switch (status.getSeverity()) {
            case IStatus.INFO:
              MessageDialog.openInformation(shell, title, msg);
              break;
            case IStatus.WARNING:
              MessageDialog.openWarning(shell, title, msg);
              break;
            default:
              MessageDialog.openError(shell, title, msg);
          }
        } else {
          MessageDialog.openError(shell, title, msg);
        }
      }
    } finally {
      provider.changed(newInput);
      if (success) {
        setInput(newInput);
      }
    }

    if (progressMonitor != null) {
      progressMonitor.setCanceled(!success);
    }
  }

  public void gotoMarker(IMarker marker) {
    setActivePage(0);
    IDE.gotoMarker(getEditor(0), marker);
  }

  public void newInputFile(final IFile file) {
    if (file != null && file.exists()) {
      syncExec(new Runnable() {
        public void run() {
          final FileEditorInput input = new FileEditorInput(file);

          setInput(input);

          m_project = file.getProject();

          if (haveActiveConfig()) {
            if (getPageCount() == 1) {
              createDsoApplicationPage(1);
              createServersPage(2);
              createClientPage(3);
            }
            ResourcesPlugin.getWorkspace().addResourceChangeListener(ConfigurationEditor.this);
            initPanels();
          } else {
            ResourcesPlugin.getWorkspace().removeResourceChangeListener(ConfigurationEditor.this);
            for (int i = getPageCount() - 1; i > 0; i--) {
              removePage(i);
            }
          }

          m_xmlEditor.setInput(input);
          String name = file.getName();
          setPartName(name);
          setPageText(m_xmlEditorPageIndex, name);
        }
      });
    }
  }

  private void syncExec(Runnable runner) {
    getSite().getShell().getDisplay().syncExec(runner);
  }

  private void asyncExec(Runnable runner) {
    getSite().getShell().getDisplay().asyncExec(runner);
  }

  public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
    if (!(editorInput instanceof IFileEditorInput)) { throw new PartInitException(
        "Invalid Input: Must be IFileEditorInput"); }

    IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;
    IFile file = fileEditorInput.getFile();
    IProject project = file.getProject();

    if (!project.exists()) {
      String msg = "Project '" + project.getName() + "' does not exist";

      ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
      throw new PartInitException(msg);
    }

    if (!project.isOpen()) {
      String msg = "Project '" + project.getName() + "' is not open";

      ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
      throw new PartInitException(msg);
    }

    m_project = project;
    super.init(site, editorInput);

    setPartName(file.getName());
  }

  protected void setInput(IEditorInput input) {
    super.setInput(input);
    m_haveActiveConfig = isActiveConfig();
  }

  public boolean isSaveAsAllowed() {
    return true;
  }

  public void initPanels() {
    if (m_project != null && m_project.isOpen()) {
      m_dsoAppPanel.setup(m_project);
      m_serversPanel.setup(m_project);
      m_clientsPanel.setup(m_project);
      enablePanels();
    } else {
      disablePanels();
    }
  }

  private void disablePanels() {
    m_dsoAppPanel.setEnabled(false);
    m_serversPanel.setEnabled(false);
    m_clientsPanel.setEnabled(false);
  }

  private void enablePanels() {
    m_dsoAppPanel.setEnabled(true);
    m_serversPanel.setEnabled(true);
    m_clientsPanel.setEnabled(true);
  }

  public void closeEditor() {
    getSite().getPage().closeEditor(this, true);
  }

  public void resourceChanged(final IResourceChangeEvent event) {
    switch (event.getType()) {
      case IResourceChangeEvent.PRE_DELETE:
      case IResourceChangeEvent.PRE_CLOSE: {
        if (m_project.equals(event.getResource())) {
          if(Display.getCurrent() != null) {
            ConfigurationEditor.this.closeEditor();
            return;
          }
          syncExec(new Runnable() {
            public void run() {
              ConfigurationEditor.this.closeEditor();
            }
          });
        }
        break;
      }
    }
  }

  public void syncXmlDocument() {
    if(PlatformUI.getWorkbench().isClosing()) return;
    asyncExec(new Runnable() {
      public void run() {
        m_xmlEditor.getTextWidget().setRedraw(false);
        try {
          int topLine = m_xmlEditor.getTopIndex();
          TcPlugin plugin = TcPlugin.getDefault();
          IDocument doc = m_xmlEditor.getDocument();
          TcConfig config = plugin.getConfiguration(m_project);
          if (config != null && config != TcPlugin.BAD_CONFIG) {
            TcConfigDocument configDoc = TcConfigDocument.Factory.newInstance();
            configDoc.setTcConfig(config);
            doc.removeDocumentListener(m_docListener);
            doc.set(plugin.configDocumentAsString(configDoc));
            doc.addDocumentListener(m_docListener);
          }
          m_xmlEditor.setTopIndex(topLine);
        } finally {
          m_xmlEditor.getTextWidget().setRedraw(true);
        }
      }
    });
  }

  public synchronized void syncXmlModel() {
    TcPlugin plugin = TcPlugin.getDefault();
    IDocument doc = m_xmlEditor.getDocument();
    String xmlText = doc.get();
    try {
      plugin.setConfigurationFromString(m_project, xmlText);
    } catch (Exception e) {
      disablePanels();
    }
  }

  private void clearDirty() {
    internalSetDirty(Boolean.FALSE);
  }

  private void internalSetDirty(Boolean isDirty) {
    TcPlugin.getDefault().setConfigurationFileDirty(m_project, isDirty);
    firePropertyChange(PROP_DIRTY);
  }

  public boolean isDirty() {
    if (m_project != null && haveActiveConfig()) {
      return m_project.isOpen() && TcPlugin.getDefault().isConfigurationFileDirty(m_project);
    } else {
      return super.isDirty();
    }
  }

  public void modelChanged() {
    syncXmlDocument();
    internalSetDirty(Boolean.TRUE);
  }

  public IDocument getDocument() {
    return m_xmlEditor.getDocument();
  }

  class TextInputListener implements ITextInputListener {
    public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {/**/}

    public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
      if (oldInput != null) {
        oldInput.removeDocumentListener(m_docListener);
      }
      if (newInput != null) {
        newInput.addDocumentListener(m_docListener);
      }
    }
  }

  class ElementStateListener implements IElementStateListener {
    public void elementContentAboutToBeReplaced(Object element) {
      m_xmlEditor.getDocument().removeDocumentListener(m_docListener);
    }

    public void elementContentReplaced(Object element) {
      m_xmlEditor.getDocument().addDocumentListener(m_docListener);
    }

    public void elementDeleted(Object element) {/**/}
    public void elementMoved(Object originalElement, Object movedElement) {/**/}
    public void elementDirtyStateChanged(Object element, boolean isDirty) {/**/}
  }

  class DocumentListener implements IDocumentListener {
    public void documentAboutToBeChanged(DocumentEvent event) {/**/}

    public void documentChanged(DocumentEvent event) {
      if (haveActiveConfig()) setTimer(false);
      internalSetDirty(Boolean.TRUE);
      if (haveActiveConfig()) setTimer(true);
    }
  }

  public void applyProblemToText(String text, String msg, String markerType) {
    TcPlugin plugin = TcPlugin.getDefault();
    IDocument doc = m_xmlEditor.getDocument();
    ConfigurationHelper configHelper = plugin.getConfigurationHelper(m_project);

    configHelper.applyProblemToText(doc, text, msg, markerType);
  }
}
