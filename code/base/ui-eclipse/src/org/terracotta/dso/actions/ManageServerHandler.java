/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.ProjectNature;
import org.terracotta.dso.TcPlugin;

import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;

/**
 * Action providing server management menuitems.
 * 
 * @see org.eclipse.jdt.core.IProject
 * @see BaseMenuCreator
 * @see ManageServerAction
 */

public class ManageServerHandler extends BaseMenuCreator implements IProjectAction {
  private IJavaProject m_javaProject;

  public ManageServerHandler() {
    super();
    TcPlugin.getDefault().registerProjectAction(this);
  }

  public void selectionChanged(IAction action, ISelection selection) {
    super.selectionChanged(action, selection);
    action.setEnabled((m_element = getElement(selection)) != null);
  }

  protected IJavaElement getJavaElement(ISelection selection) {
    update(ActionUtil.locateSelectedJavaProject(selection));
    return m_javaProject;
  }

  private void update(IJavaProject javaProject) {
    if (javaProject != null) {
      try {
        if (javaProject.getProject().hasNature(ProjectNature.NATURE_ID)) {
          m_javaProject = javaProject;
        } else {
          m_javaProject = null;
        }
      } catch (CoreException ce) {/**/
      }
    } else {
      m_javaProject = null;
    }
  }

  public void update(IProject project) {
    update(ActionUtil.findJavaProject(project));
    if (m_delegateAction != null) {
      m_delegateAction.setEnabled(m_javaProject != null);
    }
  }

  public Menu getMenu(Control parent) {
    Menu menu = null;

    buildMenu(menu = new Menu(parent));

    return menu;
  }

  public Menu getMenu(Menu parent) {
    Menu menu = null;

    buildMenu(menu = new Menu(parent));

    return menu;
  }

  protected void fillMenu(Menu menu) {
    if (m_javaProject != null) {
      TcPlugin plugin = TcPlugin.getDefault();
      IProject project = m_javaProject.getProject();
      ConfigurationHelper configHelper = plugin.getConfigurationHelper(project);
      Servers servers = configHelper.getServers();

      if (servers == null) {
        m_delegateAction.setEnabled(true);
        addMenuAction(menu, new ManageServerAction(m_javaProject));
      } else {
        Server[] serverArray = servers.getServerArray();
        m_delegateAction.setEnabled(true);
        if (serverArray != null) {
          for (int i = 0; i < serverArray.length; i++) {
            addMenuAction(menu, new ManageServerAction(m_javaProject, serverArray[i]));
          }
        } else {
          m_delegateAction.setEnabled(false);
        }
      }
    }
  }
}
