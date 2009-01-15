/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XTextField;
import com.tc.util.concurrent.ThreadUtil;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;

public class AddModuleDialog extends XContainer {
  private XTextField nameField;
  private XTextField versionField;

  public AddModuleDialog() {
    super(new BorderLayout());

    add(new XLabel("Module Declaration"), BorderLayout.NORTH);

    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);

    panel.add(new XLabel("Name"), gbc);
    gbc.gridx++;

    nameField = new XTextField();
    nameField.setColumns(20);
    panel.add(nameField, gbc);
    gbc.gridx--;
    gbc.gridy++;

    panel.add(new XLabel("Version"), gbc);
    gbc.gridx++;

    versionField = new XTextField();
    versionField.setColumns(20);
    panel.add(versionField, gbc);

    panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    add(panel);

    addHierarchyListener(new HierarchyListener() {
      private volatile boolean working;

      public void hierarchyChanged(HierarchyEvent he) {
        if (working) return;
        working = true;
        new Thread() {
          public void run() {
            ThreadUtil.reallySleep(400);
            nameField.requestFocusInWindow();
            working = false;
          }
        }.start();
      }
    });
  }

  public String[] prompt() {
    reset();
    int option = JOptionPane.showOptionDialog(this, this, "Enter Module Information", JOptionPane.OK_CANCEL_OPTION,
                                              JOptionPane.QUESTION_MESSAGE, null, null, null);

    if (option == JOptionPane.OK_OPTION) {
      String[] values = new String[2];
      values[0] = nameField.getText().trim();
      values[1] = versionField.getText().trim();
      if (!values[0].equals("") || !values[0].equals("")) return values;
    }
    return null;
  }

  private void reset() {
    nameField.setText("");
    versionField.setText("");
  }
}
