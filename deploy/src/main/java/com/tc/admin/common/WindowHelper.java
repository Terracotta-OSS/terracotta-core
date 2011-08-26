/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

public class WindowHelper {
  public static void center(Window window) {
    Dimension size = window.getSize();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    int x = screenSize.width / 2 - size.width / 2;
    int y = screenSize.height / 2 - size.height / 2;

    window.setLocation(x, y);
  }

  public static void center(Window window, Window parent) {
    Rectangle bounds = parent.getBounds();
    Dimension size = window.getSize();
    Point origin;

    origin = new Point(bounds.x + (bounds.width - size.width) / 2, bounds.y + (bounds.height - size.height) / 2);

    if(origin.x < 0 || origin.y < 0){
      center(window);
      return;
    } else {
      window.setLocation(origin);
    }
  }

  public static void center(Window window, Component parent) {
    Rectangle bounds = parent.getBounds();
    Point screenLoc = parent.getLocationOnScreen();
    Dimension size = window.getSize();
    Point origin;

    bounds.x = screenLoc.x;
    bounds.y = screenLoc.y;

    origin = new Point(bounds.x + (bounds.width - size.width) / 2, bounds.y + (bounds.height - size.height) / 2);

    window.setLocation(origin);
  }

  public static void center(Window window, Rectangle bounds) {
    Dimension size = window.getSize();
    Point origin;

    origin = new Point(bounds.x + (bounds.width - size.width) / 2, bounds.y + (bounds.height - size.height) / 2);

    window.setLocation(origin);
  }

}
