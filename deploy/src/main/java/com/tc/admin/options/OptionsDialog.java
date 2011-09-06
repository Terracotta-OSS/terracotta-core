/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.options;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.PagedView;
import com.tc.admin.common.WindowHelper;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

public class OptionsDialog extends JDialog {
  public OptionsDialog(ApplicationContext appContext, Frame frame) {
    this(appContext, frame, null);
  }

  public OptionsDialog(final ApplicationContext appContext, final Frame frame, final String selectedName) {
    super(frame, appContext.getString("options.dialog.title"), true);
    XContainer panel = new XContainer(new BorderLayout());
    XContainer topPanel = new XContainer(new GridBagLayout());
    final PagedView centerPanel = new PagedView();
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.anchor = GridBagConstraints.WEST;
    final Color selectionColor = new Color(193, 210, 238);
    ActionListener buttonListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        AbstractButton button = (AbstractButton) e.getSource();
        boolean isSelected = button.isSelected();
        centerPanel.setPage(button.getName());
        button.setBackground(isSelected ? selectionColor : null);
        button.setOpaque(isSelected);
      }
    };
    Iterator<IOption> optionIter = appContext.options();
    boolean isFirst = selectedName == null;
    ButtonGroup buttonGroup = new ButtonGroup();
    while (optionIter.hasNext()) {
      IOption option = optionIter.next();
      JToggleButton button = new JToggleButton(option.getLabel(), option.getIcon());
      button.setName(option.getName());
      button.addActionListener(buttonListener);
      button.setBorderPainted(false);
      button.setContentAreaFilled(false);
      button.setVerticalTextPosition(SwingConstants.BOTTOM);
      button.setHorizontalTextPosition(SwingConstants.CENTER);
      button.setFocusable(false);
      buttonGroup.add(button);
      if (isFirst || selectedName.equals(option.getName())) {
        button.setSelected(true);
        button.setBackground(selectionColor);
        button.setOpaque(true);
        isFirst = false;
      }
      topPanel.add(button, gbc);
      centerPanel.addPage(option.getDisplay());
      gbc.gridx++;
    }
    // filler
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    topPanel.add(new XLabel(), gbc);
    topPanel.setBackground(Color.white);
    topPanel.setBorder(BorderFactory.createEtchedBorder());

    panel.add(topPanel, BorderLayout.NORTH);
    panel.add(centerPanel, BorderLayout.CENTER);
    XContainer bottomPanel = new XContainer(new BorderLayout());
    XContainer buttonPanel = new XContainer(new GridLayout(1, 0, 3, 3));
    XButton okButton = new XButton("OK");
    buttonPanel.add(okButton, gbc);
    okButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Iterator<IOption> iter = appContext.options();
        while (iter.hasNext()) {
          iter.next().apply();
        }
        setVisible(false);
      }
    });
    XButton cancelButton = new XButton("Cancel");
    buttonPanel.add(cancelButton, gbc);
    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setVisible(false);
      }
    });
    bottomPanel.add(buttonPanel, BorderLayout.EAST);
    bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    panel.add(bottomPanel, BorderLayout.SOUTH);
    getContentPane().add(panel);
    pack();
    WindowHelper.center(this, frame);
    setVisible(true);
  }
}
