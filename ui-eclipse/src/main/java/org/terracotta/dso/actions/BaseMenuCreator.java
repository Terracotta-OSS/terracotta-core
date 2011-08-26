/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;
import org.eclipse.ui.texteditor.ITextEditor;
import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;

/**
 * Hackey way to dynamically create popup action submenus.
 */

public abstract class BaseMenuCreator
  implements IObjectActionDelegate,
             IEditorActionDelegate,
             IViewActionDelegate,
             IMenuCreator,
             IWorkbenchWindowPulldownDelegate2
{
  protected IJavaElement m_element;
  protected IAction      m_delegateAction;
  protected ISelection   m_selection;
  protected IEditorPart  m_editorPart;

  public BaseMenuCreator() {/**/}

  public void init(IWorkbenchWindow window) {/**/}
  public void run(IAction action) {/**/}
  public void dispose() {/**/}
  public void init(IViewPart view) {/**/}
  public void setActivePart(IAction action, IWorkbenchPart targetPart) {/**/}

  public void setActiveEditor(IAction action, IEditorPart targetEditor) {
    m_editorPart = targetEditor;
  }
  
  protected IJavaElement getElement(ISelection selection) {
    IJavaElement element;
    
    if((element = getJavaElement(selection)) != null &&
       hasTerracottaNature(element))
    {
      return element;
    }
    
    return null;
  }
  
  public void selectionChanged(IAction action, ISelection selection) {
    if(m_delegateAction == null) {
      m_delegateAction = action;
      m_delegateAction.setMenuCreator(this);
    }
    m_selection = selection;
  }

  public void setJavaElement(IJavaElement javaElement) {
    m_element = javaElement;
  }
  
  protected abstract IJavaElement getJavaElement(ISelection selection);
  
  protected void buildMenu(Menu menu) {
    menu.addMenuListener(new MenuAdapter() {
      public void menuShown(MenuEvent e) {
        Menu       m     = (Menu)e.widget;
        MenuItem[] items = m.getItems();
        
        for(int i = 0; i < items.length; i++) {
          items[i].dispose();
        }
        
        fillMenu(m);
      }
    });
  }

  protected ISelection getSelection() {
    if(m_editorPart != null) {
      if(m_editorPart instanceof ITextEditor) {
        return ((ITextEditor)m_editorPart).getSelectionProvider().getSelection();
      }
    }
    
    return m_selection;
  }
  
  public Menu getMenu(Control parent) {
    Menu menu = null;
    
    m_selection = getSelection();
    if((m_element = getElement(m_selection)) != null) {
      buildMenu(menu = new Menu(parent));
    }

    return menu;
  }

  public Menu getMenu(Menu parent) {
    Menu menu = null;
    
    m_selection = getSelection();
    if((m_element = getElement(m_selection)) != null) {
      buildMenu(menu = new Menu(parent));
    }

    return menu;
  }

  protected abstract void fillMenu(Menu menu);
  
  protected ConfigurationHelper getConfigHelper() {
    ConfigurationHelper helper  = null;
    IProject            project = getProject();
    
    if(project != null) {
      helper = TcPlugin.getDefault().getConfigurationHelper(project);
    }
    
    return helper;
  }
  
  protected IProject getProject() {
    return (m_element != null) ? m_element.getJavaProject().getProject() : null;
  }
  
  protected void addMenuAction(Menu menu, IAction action) {
    ActionContributionItem item = new ActionContributionItem(action);
    item.fill(menu, -1);
  }
  
  protected boolean hasTerracottaNature(IJavaElement element) {
    return TcPlugin.getDefault().hasTerracottaNature(element);
  }
}
