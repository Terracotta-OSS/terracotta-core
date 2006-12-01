/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Panel;

import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

public class BootJarView extends ViewPart {
  private BootJarPanel m_bootJarPanel;
  
  public BootJarView() {
    super();
  }

  public void createPartControl(Composite parent) {
    final IFile     bootJarFile = BootJarPanel.getBootJarFile();
    final Composite composite   = new Composite(parent, SWT.NO_BACKGROUND|SWT.EMBEDDED);
    final Frame     frame       = SWT_AWT.new_Frame(composite);

    try {
      SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
          JRootPane rootPane = new JRootPane();
          Panel     root     = new Panel(new BorderLayout());

          frame.add(root);
          root.add(rootPane);
          m_bootJarPanel = new BootJarPanel(bootJarFile);
          rootPane.getContentPane().add(m_bootJarPanel);
          m_bootJarPanel.setVisible(true);
        }
      });
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public void setFocus() {
    /**/
  }
}
