/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.installer.panel;

import com.tc.installer.util.WarFileFilter;
import com.zerog.ia.api.pub.CustomCodePanel;
import com.zerog.ia.api.pub.CustomCodePanelProxy;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

public class ChooseWarFiles extends CustomCodePanel {

  private static final String  MESSAGE_TEXT_NO  = "I do not have any web applications that I would like to use with Terracotta. Use the only the default applications.";
  private static final String  MESSAGE_TEXT_YES = "Copy web applications (.war only) from the following directory:";
  private static final String  TITLE            = "Select war Applications to Include in Sandbox";
  private JList                list;
  private JButton              button;
  private JTextField           field;
  private CustomCodePanelProxy panelProxy;
  private String               startDir;

  public boolean setupUI(CustomCodePanelProxy customCodePanelProxy) {
    panelProxy = customCodePanelProxy;
    startDir = (String) panelProxy.getVariable("lax.nl.env.CATALINA_HOME");
    if (startDir == null) startDir = panelProxy.getVariable("SYSTEM_DRIVE_ROOT").toString();

    JPanel p = new JPanel();
    p.setOpaque(false);
    p.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.insets = new Insets(1, 1, 16, 1);
    c.anchor = GridBagConstraints.NORTHWEST;

    c.gridx = 0;
    c.gridy = 0;
    c.weightx = 1;
    c.weighty = 0;
    p.add(choicePanel(), c);

    c.gridx = 0;
    c.gridy = 1;
    c.weightx = 1;
    c.weighty = 0;
    p.add(browserPanel(), c);

    list = new JList();
    list.setBackground(Color.WHITE);
    JScrollPane scroll = new JScrollPane(list);

    c.gridy = 2;
    c.weighty = 1;
    c.insets = new Insets(1, 1, 1, 1);
    p.add(scroll, c);
    add(p);

    return true;
  }

  private JPanel browserPanel() {
    JPanel p = new JPanel();
    p.setOpaque(false);
    p.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;
    c.insets = new Insets(0, 0, 0, 0);
    c.anchor = GridBagConstraints.CENTER;
    c.gridx = 0;
    c.gridy = 0;
    c.weightx = 1;
    c.weighty = 0;

    field = new JTextField();
    field.setFocusable(false);
    
    button = new JButton("Choose...");
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (!field.getText().trim().equals("")) {
          try {
            new File(field.getText());
            startDir = field.getText();
          } catch (Exception ee) {
            // not implemented
          }
        }
        JFileChooser fileChooser = new JFileChooser(startDir);
        fileChooser.setDialogTitle("Browse for .war Application Directory");
        fileChooser.setDragEnabled(false);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        int returnValue = fileChooser.showDialog(new JFrame(), "OK");
        if (returnValue == JFileChooser.APPROVE_OPTION) {
          File warDir = fileChooser.getSelectedFile();
          field.setText(warDir.getAbsolutePath());

          // populate list
          FileFilter filter = new WarFileFilter();
          File[] warList = new File(warDir.getAbsolutePath()).listFiles(filter);
          ArrayList wars = new ArrayList();
          for (int i = 0; i < warList.length; i++) {
            wars.add(warList[i].getName());
          }

          // select items
          list.setListData(wars.toArray());
          int size = list.getModel().getSize();
          int[] indices = new int[size];
          for (int i = 0; i < size; i++) {
            indices[i] = i;
          }
          list.setSelectedIndices(indices);
        }
      }
    });
    button.setFont(getFont());

    p.add(field, c);
    c.gridx = 1;
    c.weightx = 0;
    c.insets = new Insets(0, 6, 0, 0);
    p.add(button, c);

    return p;
  }

  private JPanel choicePanel() {
    JRadioButton yesWars = new JRadioButton();
    yesWars.setBackground(Color.WHITE);
    yesWars.setActionCommand("yesWars");
    yesWars.setSelected(true);
    yesWars.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        list.setEnabled(true);
        button.setEnabled(true);
        field.setEditable(true);
      }
    });
    JRadioButton noWars = new JRadioButton();
    noWars.setBackground(Color.WHITE);
    noWars.setActionCommand("noWars");
    noWars.setSelected(false);
    noWars.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        list.setEnabled(false);
        list.getSelectionModel().clearSelection();
        button.setEnabled(false);
        field.setEditable(false);
      }
    });

    ButtonGroup group = new ButtonGroup();
    group.add(yesWars);
    group.add(noWars);

    JTextPane msgYes = new JTextPane();
    msgYes.setOpaque(false);
    msgYes.setEditable(false);
    msgYes.setFont(getFont());
    msgYes.setText(MESSAGE_TEXT_YES);

    JTextPane msgNo = new JTextPane();
    msgNo.setOpaque(false);
    msgNo.setEditable(false);
    msgNo.setFont(getFont());
    msgNo.setText(MESSAGE_TEXT_NO + "\n");

    JPanel msgPanel = new JPanel();
    msgPanel.setOpaque(false);
    msgPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK), BorderFactory
        .createEmptyBorder(3, 3, 3, 3)));
    msgPanel.setLayout(new GridBagLayout());

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(4, 0, 0, 0);
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1;
    gbc.weighty = 1;
    msgPanel.add(noWars, gbc);
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.gridx = 1;
    gbc.weightx = 100;
    msgPanel.add(msgNo, gbc);
    gbc.insets = new Insets(4, 0, 0, 0);
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.weightx = 1;
    msgPanel.add(yesWars, gbc);
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.gridx = 1;
    gbc.weightx = 100;
    msgPanel.add(msgYes, gbc);
    return msgPanel;
  }

  public boolean okToContinue() {
    Object[] wars = list.getSelectedValues();
    for (int i = 0; i < wars.length; i++) {
      panelProxy.setVariable("USR_CP_WAR_" + i, panelProxy.substitute(wars[i].toString()));
    }
    return true;
  }

  public String getTitle() {
    return TITLE;
  }
}
