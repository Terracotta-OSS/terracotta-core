package org.terracotta.dso.views;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchPartSite;

import com.terracottatech.config.TcConfigDocument.TcConfig;

class ConfigViewer extends TreeViewer {
  private ConfigViewPart fPart;
  private ConfigContentProvider fContentProvider;

  ConfigViewer(Composite parent, ConfigViewPart part) {
    super(new Tree(parent, SWT.MULTI));

    fPart = part;

    getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
    setUseHashlookup(true);
    setAutoExpandLevel(ALL_LEVELS);
    fContentProvider = new ConfigContentProvider(fPart);
    setContentProvider(fContentProvider);
    setLabelProvider(new ConfigLabelProvider(this));

    clearViewer();
  }

  void setConfig(TcConfig config) {
    if(config != null) {
      setInput(getTreeRoot(config));
    } else {
      clearViewer();
    }
  }

  ConfigViewPart getPart() {
    return fPart;
  }

  void setFocus() {
    getControl().setFocus();
  }

  boolean isInFocus() {
    return getControl().isFocusControl();
  }

  void addKeyListener(KeyListener keyListener) {
    getControl().addKeyListener(keyListener);
  }

  private TreeRoot getTreeRoot(TcConfig root) {
    return new TreeRoot(root);
  }

  void initContextMenu(IMenuListener menuListener, IWorkbenchPartSite viewSite, ISelectionProvider selectionProvider) {
    MenuManager menuMgr= new MenuManager();
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(menuListener);
    Menu menu= menuMgr.createContextMenu(getTree());
    getTree().setMenu(menu);
    viewSite.registerContextMenu(menuMgr, selectionProvider);
  }

  void clearViewer() {
    setInput(TreeRoot.EMPTY_ROOT);
  }
}
