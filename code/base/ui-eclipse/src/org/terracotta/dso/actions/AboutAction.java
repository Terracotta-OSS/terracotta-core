/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.terracotta.dso.TcPlugin;

import com.tc.util.ProductInfo;

public class AboutAction extends Action implements IActionDelegate, IWorkbenchWindowActionDelegate {
  public AboutAction() {
    super("About Terracotta");
  }

  public void run() {
    run(null);
  }

  public void run(IAction action) {
    AboutDialog aboutDialog = new AboutDialog();
    aboutDialog.open();
  }

  private class AboutDialog extends MessageDialog {
    private static final String LABEL_IMAGE_PATH = "/com/tc/admin/icons/logo.png";
    private Image               fLabelImage;

    private AboutDialog() {
      super(TcPlugin.getActiveWorkbenchShell(), "About Terracotta", null, null, MessageDialog.NONE,
            new String[] { IDialogConstants.OK_LABEL }, 0);
    }

    protected Control createCustomArea(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      Label label = new Label(parent, SWT.NONE);
      fLabelImage = ImageDescriptor.createFromURL(getClass().getResource(LABEL_IMAGE_PATH)).createImage();
      label.setImage(fLabelImage);
      label.addDisposeListener(new DisposeListener() {
        public void widgetDisposed(DisposeEvent e) {
          if (fLabelImage != null) {
            fLabelImage.dispose();
          }
        }
      });
      comp.setLayout(new GridLayout());
      GridData data = new GridData();
      data.horizontalAlignment = GridData.FILL;
      data.verticalAlignment = GridData.BEGINNING;
      data.grabExcessHorizontalSpace = true;
      comp.setLayoutData(data);
      Label versionLabel = new Label(parent, SWT.NONE);
      ProductInfo productInfo = ProductInfo.getInstance();
      versionLabel.setText(productInfo.toLongString());
      versionLabel.setLayoutData(new GridData());
      return comp;
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {
    /**/
  }

  public void dispose() {
    /**/
  }

  public void init(IWorkbenchWindow window) {
    /**/
  }
}
