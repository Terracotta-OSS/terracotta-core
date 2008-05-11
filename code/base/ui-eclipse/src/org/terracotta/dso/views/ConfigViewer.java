package org.terracotta.dso.views;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchPartSite;
import org.terracotta.dso.TcPlugin;

import com.terracottatech.config.TcConfigDocument.TcConfig;

class ConfigViewer extends TreeViewer {
  private ConfigViewPart        fPart;
  private ConfigContentProvider fContentProvider;

  ConfigViewer(Composite parent, ConfigViewPart part) {
    super(new Tree(parent, SWT.MULTI));

    fPart = part;

    getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
    setUseHashlookup(true);
    setAutoExpandLevel(ALL_LEVELS);
    fContentProvider = new ConfigContentProvider();
    setContentProvider(fContentProvider);
    setLabelProvider(new ConfigLabelProvider(this));

    clearViewer();
  }

  void setConfig(TcConfig config) {
    if (fContentProvider == null) return;
    if (config != null && config != TcPlugin.BAD_CONFIG) {
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

  void initContextMenu(IMenuListener menuListener, IWorkbenchPartSite viewSite) {
    MenuManager menuMgr = new MenuManager();
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(menuListener);
    Menu menu = menuMgr.createContextMenu(getTree());
    getTree().setMenu(menu);
    viewSite.registerContextMenu(menuMgr, this);
  }

  void refreshRoots() {
    refresh(fContentProvider.rootsWrapper);
    expandToLevel(fContentProvider.rootsWrapper, ALL_LEVELS);
  }

  void refreshRoot(int index) {
    if (fContentProvider.rootsWrapper != null) {
      RootWrapper wrapper = fContentProvider.rootsWrapper.getChildAt(index);
      if (wrapper != null) {
        refresh(wrapper);
      }
    }
  }

  void refreshDistributedMethods() {
    refresh(fContentProvider.distributedMethodsWrapper);
    expandToLevel(fContentProvider.distributedMethodsWrapper, ALL_LEVELS);
  }

  void refreshDistributedMethod(int index) {
    if (fContentProvider.distributedMethodsWrapper != null) {
      DistributedMethodWrapper wrapper = fContentProvider.distributedMethodsWrapper.getChildAt(index);
      if (wrapper != null) {
        refresh(wrapper);
      }
    }
  }

  void refreshBootClasses() {
    refresh(fContentProvider.additionalBootJarClassesWrapper);
    expandToLevel(fContentProvider.additionalBootJarClassesWrapper, ALL_LEVELS);
  }

  void refreshBootClass(int index) {
    if (fContentProvider.additionalBootJarClassesWrapper != null) {
      BootClassWrapper wrapper = fContentProvider.additionalBootJarClassesWrapper.getChildAt(index);
      if (wrapper != null) {
        refresh(wrapper);
      }
    }
  }

  void refreshTransientFields() {
    refresh(fContentProvider.transientFieldsWrapper);
    expandToLevel(fContentProvider.transientFieldsWrapper, ALL_LEVELS);
  }

  void refreshTransientField(int index) {
    if (fContentProvider.transientFieldsWrapper != null) {
      TransientFieldWrapper wrapper = fContentProvider.transientFieldsWrapper.getChildAt(index);
      if (wrapper != null) {
        refresh(wrapper);
      }
    }
  }

  void refreshNamedLock(int index) {
    if (fContentProvider.namedLocksWrapper != null) {
      NamedLockWrapper wrapper = fContentProvider.namedLocksWrapper.getChildAt(index);
      if (wrapper != null) {
        refresh(wrapper);
      }
    }
  }

  void refreshNamedLocks() {
    if (fContentProvider.namedLocksWrapper != null) {
      refresh(fContentProvider.namedLocksWrapper);
      expandToLevel(fContentProvider.namedLocksWrapper, ALL_LEVELS);
    }
  }

  void refreshAutolock(int index) {
    if (fContentProvider.autolocksWrapper != null) {
      AutolockWrapper wrapper = fContentProvider.autolocksWrapper.getChildAt(index);
      if (wrapper != null) {
        refresh(wrapper);
      }
    }
  }

  void refreshAutolocks() {
    if (fContentProvider.autolocksWrapper != null) {
      refresh(fContentProvider.locksWrapper);
      expandToLevel(fContentProvider.locksWrapper, ALL_LEVELS);
    }
  }

  void refreshIncludeRule(int index) {
    if (fContentProvider.includesWrapper != null) {
      IncludeWrapper wrapper = fContentProvider.includesWrapper.getChildAt(index);
      if (wrapper != null) {
        refresh(wrapper);
      }
    }
  }

  void refreshIncludeRules() {
    if (fContentProvider.includesWrapper != null) {
      refresh(fContentProvider.includesWrapper);
      expandToLevel(fContentProvider.includesWrapper, ALL_LEVELS);
    }
  }

  void refreshExcludeRule(int index) {
    if (fContentProvider.excludesWrapper != null) {
      ExcludeWrapper wrapper = fContentProvider.excludesWrapper.getChildAt(index);
      if (wrapper != null) {
        refresh(wrapper);
      }
    }
  }

  void refreshExcludeRules() {
    if (fContentProvider.excludesWrapper != null) {
      refresh(fContentProvider.excludesWrapper);
      expandToLevel(fContentProvider.excludesWrapper, ALL_LEVELS);
    }
  }

  void refreshInstrumentationRules() {
    refresh(fContentProvider.instrumentedClassesWrapper);
    expandToLevel(fContentProvider.instrumentedClassesWrapper, ALL_LEVELS);
  }

  void clearViewer() {
    if (fContentProvider == null) return;
    setInput(TreeRoot.EMPTY_ROOT);
  }
}
