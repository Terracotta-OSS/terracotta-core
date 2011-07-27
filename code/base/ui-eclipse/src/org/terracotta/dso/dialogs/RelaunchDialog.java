/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.dialogs;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.terracotta.dso.TcPlugin;
import org.terracotta.ui.util.SWTUtil;

import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * This dialog displays the set of Terracotta server instances and DSO Application launches currently running and lets
 * the user select the subset to either terminate or restart. Selecting/deselecting the Terracotta Server instance or
 * DSO Applications nodes selects/deselects all child nodes. When all child nodes are selected/deselected, the parent
 * node is made to match.
 * <p>
 * When the resultCode is set, after the dialog is closed, if the resultCode was not CONTINUE_INDEX the passed in map of
 * server launches and the list of DSO Application launches are filtered according to users selection.
 * <p>
 * This dialog is used by ResourceDeltaVisitor after it notices the config file was saved.
 * 
 * @see org.terracotta.dso.ResourceDeltaVisitor
 * @author gkeim
 */

public class RelaunchDialog extends MessageDialog implements SelectionListener {

  private final IProject      fProject;
  private Tree                fTree;
  private TreeItem            fServersItem;
  private TreeItem            fLaunchesItem;
  private final List<ILaunch> fServerLaunches;
  private final List<ILaunch> fLaunches;

  private Button              fDisableRelaunchQueryButton;

  private static String       DISABLE_RELAUNCH_QUERY_MSG = "Don't bother me with this anymore";

  private static String       TITLE                      = "Terracotta";
  private static String       MSG                        = "The configuration file changed. Relaunch all related launch targets?";

  public static final int     CONTINUE_ID                = 0;
  public static final int     TERMINATE_ID               = CONTINUE_ID + 1;

  private static String       CONTINUE_LABEL             = "Continue";
  private static String       TERMINATE_LABEL            = "Terminate";
  private static String       RESTART_LABEL              = "Restart";

  /*
   * We attempt to show all the launch items by expanding the nodes and setting hints on the tree layout to display it's
   * full content. This is the maximum number of items we'll try to accomodate in that way before the tree will scrolled
   * vertically. This is all about the initial dialog size.
   */
  private static int          MAX_VISIBLE_LAUNCH_ITEMS   = 10;

  public RelaunchDialog(Shell shell, IProject project, List<ILaunch> serverLaunches, List<ILaunch> launches) {
    super(shell, TITLE, null, MSG, MessageDialog.NONE, new String[] { CONTINUE_LABEL, TERMINATE_LABEL, RESTART_LABEL },
          0);
    setShellStyle(getShellStyle() | SWT.RESIZE);
    fProject = project;
    fServerLaunches = serverLaunches;
    fLaunches = launches;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    parent.setLayout(new GridLayout());
    return super.createDialogArea(parent);
  }

  @Override
  protected Control createCustomArea(Composite parent) {
    Composite comp = new Composite(parent, SWT.NONE);
    comp.setLayout(new GridLayout());
    comp.setLayoutData(new GridData(GridData.FILL_BOTH));

    fTree = new Tree(comp, SWT.BORDER | SWT.MULTI | SWT.CHECK);
    if (fServerLaunches.size() > 0) {
      fServersItem = new TreeItem(fTree, SWT.NONE);
      Iterator<ILaunch> iter = fServerLaunches.iterator();

      fServersItem.setText("Terracotta Server instances");
      fServersItem.setChecked(true);
      while (iter.hasNext()) {
        ILaunch launch = iter.next();
        TreeItem serverLaunchItem = new TreeItem(fServersItem, SWT.NONE);
        serverLaunchItem.setData(launch);
        serverLaunchItem.setText(computeName(launch));
        serverLaunchItem.setChecked(true);
      }
      fServersItem.setExpanded(true);
    }

    if (fLaunches.size() > 0) {
      fLaunchesItem = new TreeItem(fTree, SWT.NONE);
      fLaunchesItem.setText("DSO Applications");
      fLaunchesItem.setChecked(true);

      Iterator<ILaunch> iter = fLaunches.iterator();
      while (iter.hasNext()) {
        ILaunch launch = iter.next();
        String label = computeName(launch);
        if (label == null) {
          iter.remove();
          continue;
        }
        TreeItem launchItem = new TreeItem(fLaunchesItem, SWT.NONE);
        launchItem.setData(launch);
        launchItem.setText(label);
        launchItem.setChecked(true);
      }
      fLaunchesItem.setExpanded(true);
    }

    GridData gridData = new GridData(GridData.FILL_BOTH);
    fTree.setLayoutData(gridData);
    parent.pack();

    /*
     * For some reason the above pack on the parent isn't causing the tree to display its full content, so the tree is
     * walked and the union of all item bounds are computed and used as the size hint on the tree's grid data. We only
     * try to initially expose up to MAX_VISIBLE_LAUNCH_ITEMS in the case someone has some outlandish number of servers
     * and DSO apps running.
     */
    Rectangle r = new Rectangle(0, 0, 0, 0);
    for (int i = 0; i < fTree.getItemCount(); i++) {
      r = mergeBounds(fTree.getItem(i), r);
    }

    gridData = new GridData(GridData.FILL_BOTH);
    gridData.heightHint = Math.min(r.height, fTree.getItemHeight() * MAX_VISIBLE_LAUNCH_ITEMS) + 10;
    gridData.widthHint = Math.min(r.width, SWTUtil.textColumnsToPixels(fTree, 100));
    fTree.setLayoutData(gridData);

    fTree.addSelectionListener(this);

    fDisableRelaunchQueryButton = new Button(comp, SWT.CHECK);
    fDisableRelaunchQueryButton.setLayoutData(new GridData());
    fDisableRelaunchQueryButton.setText(DISABLE_RELAUNCH_QUERY_MSG);
    fDisableRelaunchQueryButton.setSelection(!TcPlugin.getDefault().getQueryRestartOption(fProject));
    fDisableRelaunchQueryButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        boolean disableQueryRelaunch = fDisableRelaunchQueryButton.getSelection();
        TcPlugin.getDefault().setQueryRestartOption(fProject, !disableQueryRelaunch);
      }
    });

    return parent;
  }

  @Override
  protected void setReturnCode(int code) {
    super.setReturnCode(code);

    if (code != CONTINUE_ID) {
      if (fServersItem != null) {
        int serverLaunchCount = fServersItem.getItemCount();
        for (int i = 0; i < serverLaunchCount; i++) {
          TreeItem item = fServersItem.getItem(i);
          if (!item.getChecked()) {
            fServerLaunches.remove(item.getData());
          }
        }
      }

      if (fLaunchesItem != null) {
        int launchCount = fLaunchesItem.getItemCount();
        for (int i = 0; i < launchCount; i++) {
          TreeItem item = fLaunchesItem.getItem(i);
          if (!item.getChecked()) {
            fLaunches.remove(item.getData());
          }
        }
      }
    }
  }

  private boolean allChildrenChecked(TreeItem item) {
    int count = item.getItemCount();
    for (int i = 0; i < count; i++) {
      if (!item.getItem(i).getChecked()) { return false; }
    }
    return true;
  }

  private void handleCheckChanged(TreeItem item) {
    TreeItem parentItem = item.getParentItem();
    boolean isChecked = item.getChecked();
    if (parentItem == null) {
      for (int i = 0; i < item.getItemCount(); i++) {
        TreeItem child = item.getItem(i);
        child.setChecked(isChecked);
      }
    } else {
      if (!isChecked) {
        parentItem.setChecked(false);
      } else if (allChildrenChecked(parentItem)) {
        parentItem.setChecked(true);
      }
    }
  }

  public void widgetSelected(SelectionEvent e) {
    if (e.detail == SWT.CHECK) {
      handleCheckChanged((TreeItem) e.item);
    }
  }

  public void widgetDefaultSelected(SelectionEvent e) {
    /**/
  }

  private static Rectangle mergeBounds(TreeItem item, Rectangle r) {
    r = r.union(item.getBounds());
    for (int i = 0; i < item.getItemCount(); i++) {
      r = mergeBounds(item.getItem(i), r);
    }
    return r;
  }

  private static IProcess getProcess(ILaunch launch) {
    IDebugTarget debugTarget = launch.getDebugTarget();
    if (debugTarget != null) { return debugTarget.getProcess(); }

    IProcess[] processes = launch.getProcesses();
    if (processes != null && processes.length > 0) { return processes[0]; }

    return null;
  }

  protected String computeName(ILaunch launch) {
    String label = null;
    IProcess process = getProcess(launch);
    if (process == null) { return null; }

    ILaunchConfiguration config = process.getLaunch().getLaunchConfiguration();
    label = process.getAttribute(IProcess.ATTR_PROCESS_LABEL);
    if (label == null) {
      if (config == null) {
        label = process.getLabel();
      } else {
        // check if PRIVATE config
        if (DebugUITools.isPrivate(config)) {
          label = process.getLabel();
        } else {
          String type = null;
          try {
            type = config.getType().getName();
          } catch (CoreException e) {/**/
          }
          StringBuffer buffer = new StringBuffer();
          buffer.append(config.getName());
          if (type != null) {
            buffer.append(" ["); //$NON-NLS-1$
            buffer.append(type);
            buffer.append("] "); //$NON-NLS-1$
          }
          buffer.append(process.getLabel());
          label = buffer.toString();
        }
      }
    }

    return label;
  }
}
