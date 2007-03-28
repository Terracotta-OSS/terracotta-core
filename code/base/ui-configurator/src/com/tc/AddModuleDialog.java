/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc;

import org.dijon.ContainerResource;
import org.dijon.TextField;

import com.tc.admin.common.XContainer;

import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

import javax.swing.JOptionPane;

public class AddModuleDialog extends XContainer {

  private static SessionIntegratorContext CONTEXT = SessionIntegrator.getContext();
  private TextField                       m_nameField;
  private TextField                       m_versionField;

  public AddModuleDialog() {
    super();
    load(CONTEXT.topRes.findComponent("ModulesDialog"));
  }

  public void load(ContainerResource containerRes) {
    super.load(containerRes);
    m_nameField = (TextField) findComponent("ModuleNameField");
    m_versionField = (TextField) findComponent("ModuleVersionField");

    addHierarchyListener(new HierarchyListener() {
      private volatile boolean working;

      public void hierarchyChanged(HierarchyEvent he) {
        if (working) return;
        working = true;
        new Thread() {
          public void run() {
            try {
              Thread.sleep(400);
              m_nameField.requestFocusInWindow();
              working = false;
            } catch (Exception e) {
              // ignore
            }
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
      values[0] = m_nameField.getText().trim();
      values[1] = m_versionField.getText().trim();
      if (!values[0].equals("") || !values[0].equals("")) return values;
    }
    return null;
  }

  private void reset() {
    m_nameField.setText("");
    m_versionField.setText("");
  }
}
