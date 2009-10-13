/*
 * @(#)BasicArrowButton.java 1.29 06/04/07 Copyright 2006 Sun Microsystems, Inc. All rights reserved. SUN
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.tc.admin.common;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;

public class BasicToggleArrowButton extends JToggleButton implements SwingConstants {
  protected int       direction;
  private final Color shadow;
  private final Color darkShadow;
  private final Color highlight;

  public BasicToggleArrowButton(int direction, Color background, Color shadow, Color darkShadow, Color highlight) {
    super();
    setRequestFocusEnabled(false);
    setDirection(direction);
    setBackground(background);
    this.shadow = shadow;
    this.darkShadow = darkShadow;
    this.highlight = highlight;
  }

  public BasicToggleArrowButton(int direction) {
    this(direction, UIManager.getColor("control"), UIManager.getColor("controlShadow"), UIManager
        .getColor("controlDkShadow"), UIManager.getColor("controlLtHighlight"));
  }

  public int getDirection() {
    return direction;
  }

  public void setDirection(int dir) {
    direction = dir;
  }

  @Override
  public void paint(Graphics g) {
    Color origColor;
    boolean isSelected, isEnabled;
    int w, h, size;

    w = getSize().width;
    h = getSize().height;
    origColor = g.getColor();
    isSelected = getModel().isSelected();
    isEnabled = isEnabled();

    g.setColor(getBackground());
    g.fillRect(1, 1, w - 2, h - 2);

    // / Draw the proper Border
    if (getBorder() != null && !(getBorder() instanceof UIResource)) {
      paintBorder(g);
    } else if (isSelected) {
      g.setColor(shadow);
      g.drawRect(0, 0, w - 1, h - 1);
    } else {
      // Using the background color set above
      g.drawLine(0, 0, 0, h - 1);
      g.drawLine(1, 0, w - 2, 0);

      g.setColor(highlight); // inner 3D border
      g.drawLine(1, 1, 1, h - 3);
      g.drawLine(2, 1, w - 3, 1);

      g.setColor(shadow); // inner 3D border
      g.drawLine(1, h - 2, w - 2, h - 2);
      g.drawLine(w - 2, 1, w - 2, h - 3);

      g.setColor(darkShadow); // black drop shadow __|
      g.drawLine(0, h - 1, w - 1, h - 1);
      g.drawLine(w - 1, h - 1, w - 1, 0);
    }

    // If there's no room to draw arrow, bail
    if (h < 5 || w < 5) {
      g.setColor(origColor);
      return;
    }

    if (isSelected) {
      g.translate(1, 1);
    }

    // Draw the arrow
    size = Math.min((h - 4) / 3, (w - 4) / 3);
    size = Math.max(size, 2);
    paintTriangle(g, (w - size) / 2, (h - size) / 2, size, direction, isEnabled);

    // Reset the Graphics back to it's original settings
    if (isSelected) {
      g.translate(-1, -1);
    }
    g.setColor(origColor);

  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(16, 16);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getMaximumSize() {
    return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  @Override
  public boolean isFocusTraversable() {
    return false;
  }

  public void paintTriangle(Graphics g, int x, int y, int size, int theDirection, boolean isEnabled) {
    Color oldColor = g.getColor();
    int mid, i, j;

    j = 0;
    size = Math.max(size, 2);
    mid = (size / 2) - 1;

    g.translate(x, y);
    if (isEnabled) g.setColor(darkShadow);
    else g.setColor(shadow);

    switch (theDirection) {
      case NORTH:
        for (i = 0; i < size; i++) {
          g.drawLine(mid - i, i, mid + i, i);
        }
        if (!isEnabled) {
          g.setColor(highlight);
          g.drawLine(mid - i + 2, i, mid + i, i);
        }
        break;
      case SOUTH:
        if (!isEnabled) {
          g.translate(1, 1);
          g.setColor(highlight);
          for (i = size - 1; i >= 0; i--) {
            g.drawLine(mid - i, j, mid + i, j);
            j++;
          }
          g.translate(-1, -1);
          g.setColor(shadow);
        }

        j = 0;
        for (i = size - 1; i >= 0; i--) {
          g.drawLine(mid - i, j, mid + i, j);
          j++;
        }
        break;
      case WEST:
        for (i = 0; i < size; i++) {
          g.drawLine(i, mid - i, i, mid + i);
        }
        if (!isEnabled) {
          g.setColor(highlight);
          g.drawLine(i, mid - i + 2, i, mid + i);
        }
        break;
      case EAST:
        if (!isEnabled) {
          g.translate(1, 1);
          g.setColor(highlight);
          for (i = size - 1; i >= 0; i--) {
            g.drawLine(j, mid - i, j, mid + i);
            j++;
          }
          g.translate(-1, -1);
          g.setColor(shadow);
        }

        j = 0;
        for (i = size - 1; i >= 0; i--) {
          g.drawLine(j, mid - i, j, mid + i);
          j++;
        }
        break;
    }
    g.translate(-x, -y);
    g.setColor(oldColor);
  }

  public static void main(String[] args) {
    JFrame frame = new JFrame("BasicToggleArrowButtonTest");
    frame.getContentPane().setLayout(new FlowLayout());
    frame.getContentPane().add(new BasicToggleArrowButton(SwingConstants.SOUTH));
    frame.pack();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    WindowHelper.center(frame);
    frame.setVisible(true);
  }
}
