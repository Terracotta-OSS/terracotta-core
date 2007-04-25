/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.apache.xmlbeans.XmlOptions;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.help.IHelpResource;
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
import org.terracotta.dso.JavaSetupParticipant;
import org.terracotta.dso.Messages;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.decorator.AdaptedModuleDecorator;
import org.terracotta.dso.decorator.AdaptedPackageFragmentDecorator;
import org.terracotta.dso.decorator.AdaptedTypeDecorator;
import org.terracotta.dso.decorator.AutolockedDecorator;
import org.terracotta.dso.decorator.DistributedMethodDecorator;
import org.terracotta.dso.decorator.ExcludedModuleDecorator;
import org.terracotta.dso.decorator.ExcludedTypeDecorator;
import org.terracotta.dso.decorator.NameLockedDecorator;
import org.terracotta.dso.decorator.RootDecorator;
import org.terracotta.dso.decorator.TransientDecorator;
import org.terracotta.dso.editors.xml.XMLEditor;
import org.terracotta.dso.editors.xmlbeans.XmlConfigContext;

import com.terracottatech.config.Application;
import com.terracottatech.config.ConfigurationModel;
import com.terracottatech.config.System;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.IOException;
import java.text.MessageFormat;

public class ConfigurationEditor extends MultiPageEditorPart implements IResourceChangeListener, IGotoMarker {

  private static final int         XML_EDITOR_PAGE_INDEX      = 0;
  private static final int         DSO_APPLICATION_PAGE_INDEX = 1;
  private static final int         SERVERS_PAGE_INDEX         = 2;
  private static final int         CLIENT_PAGE_INDEX          = 3;

  private IProject                 m_project;
  private Application              m_application;
  private ConfigurationEditorPanel m_dsoAppPanel;
  private ConfigurationEditorPanel m_serversPanel;
  private ConfigurationEditorPanel m_clientsPanel;
  private XMLEditor                m_xmlEditor;
  private int                      m_xmlEditorPageIndex;
  private ResourceDeltaVisitor     m_resourceDeltaVisitor;
  private boolean                  m_haveActiveConfig;
  private DocumentListener         m_docListener;
  private ElementStateListener     m_elementStateListener;
  private TextInputListener        m_textInputListener;
  private Runnable                 m_parseTimer;
  private boolean                  m_isXmlEditorVisible       = true;
  private int                      m_currentPage              = -1;
  private Display                  m_display;

  static {
    try {
      java.lang.System.setProperty("sun.awt.noerasebackground", "true");
      java.lang.System.setProperty("swing.volatileImageBufferEnabled", "false");
    } catch (Throwable t) {/**/
    }
  }

  public ConfigurationEditor() {
    super();

    m_docListener = new DocumentListener();
    m_elementStateListener = new ElementStateListener();
    m_textInputListener = new TextInputListener();
    m_parseTimer = new ParseTimer();
    m_display = Display.getDefault();
  }

  private void setTimer(boolean start) {
    if (start) m_display.timerExec(2000, m_parseTimer);
    else m_display.timerExec(-1, m_parseTimer);
  }

  private class ParseTimer implements Runnable {
    public void run() {
      if (m_isXmlEditorVisible) syncXmlModel();
    }
  }

  void createDsoApplicationPage(int pageIndex) {
    addPage(pageIndex, m_dsoAppPanel = new DsoApplicationPanel(getContainer(), SWT.NONE));
    setPageText(pageIndex, "DSO config");
  }

  public DsoApplicationPanel getDsoApplicationPanel() {
    return (DsoApplicationPanel) m_dsoAppPanel;
  }

  public void showDsoApplicationPanel() {
    setActivePage(0);
  }

  void createServersPage(int pageIndex) {
    ScrolledComposite scroll = new ScrolledComposite(getContainer(), SWT.V_SCROLL);
    scroll.setContent(m_serversPanel = new ServersPanel(scroll, SWT.NONE));
    scroll.setExpandHorizontal(true);
    scroll.setExpandVertical(true);
    scroll.setMinHeight(370);
    addPage(pageIndex, scroll);
    setPageText(pageIndex, "Servers config");
  }

  void createClientPage(int pageIndex) {
    ScrolledComposite scroll = new ScrolledComposite(getContainer(), SWT.V_SCROLL);
    scroll.setContent(m_clientsPanel = new ClientsPanel(scroll, SWT.NONE));
    scroll.setExpandHorizontal(true);
    scroll.setExpandVertical(true);
    scroll.setMinHeight(460);
    addPage(pageIndex, scroll);
    setPageText(pageIndex, "Clients config");
  }

  void createXMLEditorPage(int pageIndex) {
    try {
      IEditorInput input = getEditorInput();

      addPage(m_xmlEditorPageIndex = pageIndex, m_xmlEditor = new XMLEditor(), input);
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

    return file != null && file.equals(configFile);
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
    }
    initPanels();
  }

  public void dispose() {
    m_dsoAppPanel.detach();
    m_serversPanel.detach();
    m_clientsPanel.detach();
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
    super.dispose();
  }

  public void doSave(IProgressMonitor monitor) {
    m_xmlEditor.doSave(monitor);
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
      final FileEditorInput input = new FileEditorInput(file);

      setInput(input);

      m_project = file.getProject();

      if (haveActiveConfig()) {
        if (getPageCount() == 1) {
          createDsoApplicationPage(1);
          createServersPage(2);
          createClientPage(3);
        }
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
        initPanels();
      } else {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        for (int i = getPageCount() - 1; i > 0; i--) {
          removePage(i);
        }
      }

      syncExec(new Runnable() {
        public void run() {
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

  protected void pageChange(int newPageIndex) {
    super.pageChange(newPageIndex);
    if (m_currentPage == XML_EDITOR_PAGE_INDEX) {
      m_isXmlEditorVisible = false;
      updateXmlConfig();
      switch (newPageIndex) {
        case DSO_APPLICATION_PAGE_INDEX:
          m_dsoAppPanel.refreshContent();
          break;
        case SERVERS_PAGE_INDEX:
          m_serversPanel.refreshContent();
          break;
        case CLIENT_PAGE_INDEX:
          m_clientsPanel.refreshContent();
          break;

        default:
          break;
      }
    } else {
      m_isXmlEditorVisible = true;
      clearDirty();
    }
    m_currentPage = newPageIndex;
  }

  private ResourceDeltaVisitor getResourceDeltaVisitor() {
    if (m_resourceDeltaVisitor == null) {
      m_resourceDeltaVisitor = new ResourceDeltaVisitor();
    }
    return m_resourceDeltaVisitor;
  }

  class ResourceDeltaVisitor implements IResourceDeltaVisitor {
    public boolean visit(IResourceDelta delta) {
      if (PlatformUI.getWorkbench().isClosing()) { return false; }

      if (delta.getKind() == IResourceDelta.CHANGED) {
        TcPlugin plugin = TcPlugin.getDefault();
        IResource res = delta.getResource();

        if (res instanceof IFile) {
          IProject project = res.getProject();
          int flags = delta.getFlags();

          if (project != null && (flags & IResourceDelta.CONTENT) != 0) {
            if ((flags & IResourceDelta.MARKERS) != 0) { return false; }
            IFile configFile = plugin.getConfigurationFile(project);

            if (configFile != null && configFile.equals(res)) {
              // plugin.reloadConfiguration(m_project);
              // initPanels();
              // clearDirty();

              return false;
            }
          }
        }
      }

      return true;
    }
  }

  /**
   * Ensures that the config has both a System and Application element. The System element sets the config-mode to
   * DEVELOPMENT.
   */
  private void ensureRequiredConfigElements() {
    if (m_project != null && m_project.isOpen()) {
      TcPlugin plugin = TcPlugin.getDefault();
      TcConfig config = plugin.getConfiguration(m_project);

      if (config != null) {
        System system = config.getSystem();

        if (system == null) {
          system = config.addNewSystem();
          system.setConfigurationModel(ConfigurationModel.DEVELOPMENT);
        }

        m_application = config.getApplication();

        if (m_application == null) {
          m_application = config.addNewApplication();
          m_application.addNewDso().addNewInstrumentedClasses();
        }
      }
    }
  }

  public void initPanels() {
    if (m_project != null && m_project.isOpen()) {
      ensureRequiredConfigElements();
      m_dsoAppPanel.init(m_project);
      m_serversPanel.init(m_project);
      m_clientsPanel.init(m_project);
      enablePanels();
    } else {
      disablePanels();
    }
  }

  private void updateXmlConfig() {
    internalSetDirty(Boolean.FALSE);
    syncXmlModel();
    XmlConfigContext.getInstance(m_project).refreshXmlConfig();
  }

  public void updateInstrumentedClassesPanel() {
    updateXmlConfig();
    m_dsoAppPanel.refreshContent();
    TcPlugin.getDefault().updateDecorators(
        new String[] {
          AdaptedModuleDecorator.DECORATOR_ID,
          AdaptedTypeDecorator.DECORATOR_ID,
          AdaptedPackageFragmentDecorator.DECORATOR_ID,
          ExcludedTypeDecorator.DECORATOR_ID,
          ExcludedModuleDecorator.DECORATOR_ID });
  }

  public void updateTransientsPanel() {
    updateXmlConfig();
    m_dsoAppPanel.refreshContent();
    TcPlugin.getDefault().updateDecorator(TransientDecorator.DECORATOR_ID);
  }

  public void updateRootsPanel() {
    updateXmlConfig();
    m_dsoAppPanel.refreshContent();
    TcPlugin.getDefault().updateDecorator(RootDecorator.DECORATOR_ID);
  }

  public void updateDistributedMethodsPanel() {
    updateXmlConfig();
    m_dsoAppPanel.refreshContent();
    TcPlugin.getDefault().updateDecorator(DistributedMethodDecorator.DECORATOR_ID);
  }

  public void updateLocksPanel() {
    updateXmlConfig();
    m_dsoAppPanel.refreshContent();
    TcPlugin.getDefault().updateDecorators(
        new String[] { NameLockedDecorator.DECORATOR_ID, AutolockedDecorator.DECORATOR_ID });
  }

  public void updateBootClassesPanel() {
    updateXmlConfig();
    m_dsoAppPanel.refreshContent();
  }

  private void disablePanels() {
    m_dsoAppPanel.setActive(false);
    m_serversPanel.setActive(false);
    m_clientsPanel.setActive(false);
  }

  private void enablePanels() {
    m_dsoAppPanel.setActive(true);
    m_serversPanel.setActive(true);
    m_clientsPanel.setActive(true);
  }

  public void closeEditor() {
    getSite().getPage().closeEditor(this, true);
  }

  public void resourceChanged(final IResourceChangeEvent event) {
    switch (event.getType()) {
      case IResourceChangeEvent.PRE_DELETE:
      case IResourceChangeEvent.PRE_CLOSE: {
        if (m_project.equals(event.getResource())) {
          ConfigurationEditor.this.closeEditor();
        }
        break;
      }
      case IResourceChangeEvent.POST_CHANGE: {
        try {
          event.getDelta().accept(getResourceDeltaVisitor());
        } catch (CoreException ce) {
          ce.printStackTrace();
        }
        break;
      }
    }
  }

  public void syncXmlDocument() {
    TcPlugin plugin = TcPlugin.getDefault();
    IDocument doc = m_xmlEditor.getDocument();
    XmlOptions opts = plugin.getXmlOptions();
    TcConfig config = plugin.getConfiguration(m_project);
    if (config != null) {
      TcConfigDocument configDoc = TcConfigDocument.Factory.newInstance();
      configDoc.setTcConfig(config);
      doc.removeDocumentListener(m_docListener);
      doc.set(configDoc.xmlText(opts));
      doc.addDocumentListener(m_docListener);
    }
    plugin.fireConfigurationChange(m_project);
  }

  public synchronized void syncXmlModel() {
    TcPlugin plugin = TcPlugin.getDefault();
    IDocument doc = m_xmlEditor.getDocument();
    String xmlText = doc.get();
    try {
      plugin.setConfigurationFromString(m_project, xmlText);
    } catch (IOException ioe) {
      disablePanels();
    }
  }

  /**
   * This gets invokes by our sub-components when they've modified the model.
   */
  public synchronized void setDirty() {
    syncXmlDocument();
    doSave(null);
  }

  public synchronized void _setDirty() {
    // XXX: This really needs to be removed. The doc should only be saved when the user explicitly saves and the XML
    // text editor should only be refreshed when it comes into view. On top of that there is no reason why a GUI would
    // need a setDirty() method in the first place. The entire config is written to disk, then reloaded from disk in
    // it's entirety after every xml element update...
    syncXmlDocument();
    JavaSetupParticipant.inspectAll();
    TcPlugin plugin = TcPlugin.getDefault();
    plugin.updateDecorators();
    plugin.fireConfigurationChange(m_project);
    plugin.saveConfigurationQuietly(m_project);
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

  /**
   * Help support
   */

  private HelpContextProvider m_helpContextProvider;
  private HelpContext         m_helpContext;

  private HelpContextProvider getHelpContextProvider() {
    if (m_helpContextProvider == null) {
      m_helpContextProvider = new HelpContextProvider();
    }
    return m_helpContextProvider;
  }

  private HelpContext getHelpContext() {
    if (m_helpContext == null) {
      m_helpContext = new HelpContext();
    }
    return m_helpContext;
  }

  public Object getAdapter(Class key) {
    if (key.equals(IContextProvider.class)) { return getHelpContextProvider(); }
    return super.getAdapter(key);
  }

  class HelpContext implements IContext, IHelpResource {
    private IHelpResource[] m_helpResources = new IHelpResource[] { this };

    public IHelpResource[] getRelatedTopics() {
      return m_helpResources;
    }

    public String getText() {
      return "Terracotta Configuration Editor";
    }

    public String getHref() {
      return "/org.terracotta.dso/html/tasks/config/Overview.html";
    }

    public String getLabel() {
      return getText();
    }
  }

  class HelpContextProvider implements IContextProvider {
    public int getContextChangeMask() {
      return NONE;
    }

    public IContext getContext(Object target) {
      return getHelpContext();
    }

    public String getSearchExpression(Object target) {
      return "Terracotta";
    }
  }
}
