/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import com.tc.admin.common.WindowHelper;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.terracottatech.config.Include;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;

/*
 * TODO: Merge this with the one in the Eclipse plugin.
 */

public class OnLoadDialog extends JDialog {
  private SessionIntegratorFrame parentFrame;
  private OnLoadPanel            onLoadPanel;

  public OnLoadDialog(final SessionIntegratorFrame parentFrame) {
    super(parentFrame, true);

    this.parentFrame = parentFrame;

    if(parentFrame != null) {
      setTitle(parentFrame.getTitle());
    }
    
    Container cp = getContentPane();
    cp.setLayout(new BorderLayout());

    add(onLoadPanel = new OnLoadPanel());
    
    XContainer buttonPanel = new XContainer(new FlowLayout());
    XButton closeButton = new XButton("Close");
    closeButton.addActionListener(new CloseButtonHandler());
    buttonPanel.add(closeButton);

    XButton cancelButton = new XButton("Cancel");
    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        setVisible(false);
      }
    });
    buttonPanel.add(cancelButton);

    cp.add(buttonPanel, BorderLayout.SOUTH);

    pack();
  }

  private class CloseButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      onLoadPanel.updateInclude();
      setVisible(false);
      if(parentFrame != null) {
        parentFrame.modelChanged();
      }
    }
  }
  
  public void setInclude(Include include) {
    onLoadPanel.setInclude(include);
    pack();
  }

  public Include getInclude() {
    return onLoadPanel.getInclude();
  }

  public void edit(Include theInclude) {
    setInclude(theInclude);
    WindowHelper.center(this, getOwner());
    setVisible(true);
  }
  
  public static void main(String[] args) {
    OnLoadDialog d = new OnLoadDialog(null);
    d.setTitle("OnLoad Test");
    d.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    Include include = Include.Factory.newInstance();
    include.setClassExpression("com.test.MyClass");
    d.edit(include);
    System.out.println(include.xmlText());
  }
}
