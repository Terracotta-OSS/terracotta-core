/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.IAdminClientContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.model.IBasicObject;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IClusterModelElement;
import com.tc.admin.model.RootCreationListener;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class RootsNode extends ComponentNode implements RootCreationListener, PropertyChangeListener {
  protected final IAdminClientContext adminClientContext;
  protected final IClusterModel       clusterModel;

  protected IBasicObject[]            roots;
  protected ObjectBrowser             objectBrowserPanel;
  protected JPopupMenu                popupMenu;
  protected RefreshAction             refreshAction;

  private static final String         REFRESH_ACTION = "RefreshAction";

  private static final IBasicObject[] EMPTY_ROOTS    = new IBasicObject[0];

  public RootsNode(IAdminClientContext adminClientContext, IClusterModel clusterModel) {
    super();
    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;
    roots = EMPTY_ROOTS;

    setLabel(adminClientContext.getMessage("dso.roots"));
    clusterModel.addPropertyChangeListener(this);
    clusterModel.addRootCreationListener(this);
    if (clusterModel.isReady()) {
      init();
    }
  }

  String getBaseLabel() {
    return adminClientContext.getMessage("dso.roots");
  }

  private void updateLabel() {
    int rootCount = getRootCount();
    String suffix = rootCount == 1 ? adminClientContext.getMessage("dso.roots.suffix.singular") : adminClientContext
        .getMessage("dso.roots.suffix.plural");
    setLabel(getBaseLabel() + " (" + rootCount + " " + suffix + ")");
    nodeChanged();
  }

  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if (IClusterModelElement.PROP_READY.equals(prop)) {
      if (clusterModel.isReady()) {
        SwingUtilities.invokeLater(new InitRunnable());
      }
    }
  }

  private class InitRunnable implements Runnable {
    public void run() {
      init();
    }
  }

  private void init() {
    roots = EMPTY_ROOTS;
    if (objectBrowserPanel != null) {
      objectBrowserPanel.clearModel();
    }
    adminClientContext.execute(new InitWorker());
  }

  private class InitWorker extends BasicWorker<IBasicObject[]> {
    private InitWorker() {
      super(new Callable<IBasicObject[]>() {
        public IBasicObject[] call() throws Exception {
          return clusterModel.getRoots();
        }
      });
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (!(rootCause instanceof IOException)) {
          adminClientContext.log(e);
        }
      } else {
        roots = getResult();
        initMenu();
        if (objectBrowserPanel != null) {
          objectBrowserPanel.setObjects(roots);
        }
        updateLabel();
      }
    }
  }

  protected ObjectBrowser createObjectBrowserPanel() {
    return new ObjectBrowser(adminClientContext, clusterModel, roots);
  }

  @Override
  public Component getComponent() {
    if (objectBrowserPanel == null) {
      adminClientContext.block();
      objectBrowserPanel = createObjectBrowserPanel();
      objectBrowserPanel.setNode(this);
      adminClientContext.unblock();
    }
    return objectBrowserPanel;
  }

  public IBasicObject getRoot(int index) {
    return roots != null ? roots[index] : null;
  }

  public int getRootCount() {
    return roots != null ? roots.length : 0;
  }

  private void initMenu() {
    refreshAction = new RefreshAction();

    popupMenu = new JPopupMenu();
    popupMenu.add(refreshAction);

    addActionBinding(REFRESH_ACTION, refreshAction);
  }

  @Override
  public JPopupMenu getPopupMenu() {
    return popupMenu;
  }

  @Override
  public Icon getIcon() {
    return RootsHelper.getHelper().getRootsIcon();
  }

  private class RefreshWorker extends BasicWorker<Void> {
    private RefreshWorker(final boolean isExpanded) {
      super(new Callable<Void>() {
        public Void call() throws Exception {
          tearDownChildren();
          roots = clusterModel.getRoots();
          for (IBasicObject root : roots) {
            adminClientContext.setStatus(adminClientContext.format("refreshing.field.pattern", root.getName()));
            root.refresh();
          }
          return null;
        }
      });
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (!(rootCause instanceof IOException)) {
          adminClientContext.log(e);
        }
      } else {
        if (objectBrowserPanel != null) {
          objectBrowserPanel.setObjects(roots);
        }
        updateLabel();
      }
      adminClientContext.unblock();
      adminClientContext.clearStatus();
    }
  }

  public void refresh() {
    adminClientContext.setStatus(adminClientContext.getMessage("dso.roots.refreshing"));
    adminClientContext.block();
    adminClientContext.execute(new RefreshWorker(adminClientContext.getAdminClientController().isExpanded(this)));
  }

  private class RefreshAction extends XAbstractAction {
    private RefreshAction() {
      super();
      setName(adminClientContext.getMessage("refresh.name"));
      setSmallIcon(RootsHelper.getHelper().getRefreshIcon());
      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
    }

    public void actionPerformed(ActionEvent ae) {
      refresh();
    }
  }

  @Override
  public void nodeClicked(MouseEvent me) {
    if (refreshAction != null) {
      refreshAction.actionPerformed(null);
    }
  }

  public void rootCreated(IBasicObject root) {
    SwingUtilities.invokeLater(new RootCreatedRunnable(root));
  }

  private class RootCreatedRunnable implements Runnable {
    private final IBasicObject root;

    private RootCreatedRunnable(IBasicObject root) {
      this.root = root;
    }

    public void run() {
      adminClientContext.setStatus(adminClientContext.getMessage("dso.root.retrieving"));
      ArrayList<IBasicObject> list = new ArrayList<IBasicObject>(Arrays.asList(roots));
      list.add(root);
      roots = list.toArray(new IBasicObject[list.size()]);
      if (objectBrowserPanel != null) {
        objectBrowserPanel.add(root);
      }
      adminClientContext.setStatus(adminClientContext.getMessage("dso.root.new") + root);
      updateLabel();
    }
  }

  @Override
  public void tearDown() {
    clusterModel.removePropertyChangeListener(this);
    clusterModel.removeRootCreationListener(this);

    if (objectBrowserPanel != null) {
      objectBrowserPanel.tearDown();
    }

    super.tearDown();
  }
}
