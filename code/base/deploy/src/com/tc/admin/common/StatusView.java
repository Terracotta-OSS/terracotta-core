/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

public class StatusView extends XContainer {
  protected XLabel               label;
  protected XLabel               indicator;

  private static final Dimension SIZE                 = new Dimension(12, 12);
  private static final Color     DEFAULT_INDICATOR_FG = Color.black;

  public StatusView() {
    super(new GridBagLayout());

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(1, 3, 1, 3);
    gbc.anchor = GridBagConstraints.WEST;

    indicator = new XLabel();
    indicator.setOpaque(true);
    indicator.setHorizontalAlignment(SwingConstants.CENTER);
    indicator.setVerticalAlignment(SwingConstants.CENTER);
    indicator.setFont(new Font("Serif", Font.BOLD, 10));
    indicator.setForeground(DEFAULT_INDICATOR_FG);
    indicator.setBorder(LineBorder.createBlackLineBorder());
    indicator.setMinimumSize(SIZE);
    indicator.setPreferredSize(SIZE);
    add(indicator, gbc);
    gbc.gridx++;

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    add(label = new XLabel(), gbc);
  }

  public XLabel getLabel() {
    return label;
  }

  public XLabel getIndicator() {
    return indicator;
  }

  @Override
  public void setForeground(Color fg) {
    if (label != null) {
      label.setForeground(fg);
    }
    super.setForeground(fg);
  }

  public void setText(String text) {
    label.setText(text);
    paintImmediately(0, 0, getWidth(), getHeight());
  }

  public void setIndicator(Color bg) {
    setIndicator(bg, "");
  }

  public void setIndicator(Color bg, String text) {
    setIndicator(bg, DEFAULT_INDICATOR_FG, text);
  }

  public void setIndicator(Color bg, Color fg, String text) {
    indicator.setBackground(bg);
    indicator.setForeground(fg);
    indicator.setText(text);
    revalidate();
    paintImmediately(0, 0, getWidth(), getHeight());
  }

  public static void main(String[] args) {
    JFrame frame = new JFrame("Server Status Indicators");
    Container cp = frame.getContentPane();
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(0, 5, 0, 1);
    StatusView sv;

    cp.add(sv = new StatusView(), gbc);
    sv.setText("Starting or disk-based standby");
    sv.setIndicator(Color.yellow, "S");
    sv.setBackground(Color.white);
    sv.setOpaque(true);
    gbc.gridy++;

    cp.add(sv = new StatusView(), gbc);
    sv.setText("Initializing (network-based only)");
    sv.setIndicator(Color.orange, "I");
    sv.setBackground(Color.white);
    sv.setOpaque(true);
    gbc.gridy++;

    cp.add(sv = new StatusView(), gbc);
    sv.setText("Passive-standby (network-based only)");
    sv.setIndicator(Color.cyan, "P");
    sv.setBackground(Color.white);
    sv.setOpaque(true);
    gbc.gridy++;

    cp.add(sv = new StatusView(), gbc);
    sv.setText("Active-coordinator server");
    sv.setIndicator(Color.green, "A");
    sv.setBackground(Color.white);
    sv.setOpaque(true);
    gbc.gridy++;

    cp.add(sv = new StatusView(), gbc);
    sv.setText("Unreachable");
    sv.setIndicator(Color.red, Color.white, "E");
    sv.setBackground(Color.white);
    sv.setOpaque(true);
    gbc.gridy++;

    cp.setBackground(Color.white);
    frame.pack();
    WindowHelper.center(frame);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
  }
}
