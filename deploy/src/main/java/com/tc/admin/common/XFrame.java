/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.Timer;

public class XFrame extends JFrame {
  private boolean     blocked;
  private Cursor      savedCursor;
  private final Timer storeTimer;
  protected Action    closeAction;

  public XFrame() {
    super();

    addWindowListener(new WindowListener());
    addComponentListener(new ComponentListener());

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    storeTimer = new Timer(1000, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        storeBounds();
      }
    });
    storeTimer.setRepeats(false);

    blocked = false;
  }

  public XFrame(String title) {
    this();
    setTitle(title);
  }

  @Override
  public void addNotify() {
    super.addNotify();

    Rectangle rect = getPreferredBounds();
    if (rect != null) {
      setBounds(rect);
    }

    Integer state = getPreferredState();
    if (state != null) {
      setExtendedState(state.intValue());
    }
  }

  @Override
  public void setExtendedState(int state) {
    Toolkit toolkit = Toolkit.getDefaultToolkit();

    if ((state & MAXIMIZED_VERT) != 0) {
      if (!toolkit.isFrameStateSupported(MAXIMIZED_VERT)) {
        System.out.println("MAXIMIZED_VERT not supported!");
      }
    }
    if ((state & MAXIMIZED_HORIZ) != 0) {
      if (!toolkit.isFrameStateSupported(MAXIMIZED_HORIZ)) {
        System.out.println("MAXIMIZED_HORIZ not supported!");
      }
    }
    if ((state & ICONIFIED) != 0) {
      if (!toolkit.isFrameStateSupported(ICONIFIED)) {
        System.out.println("ICONIFIED not supported!");
      }
    }

    super.setExtendedState(state);
  }

  protected Preferences getPreferences() {
    return null;
  }

  protected void storePreferences() {
    /**/
  }

  public void storeBounds() {
    if (getExtendedState() != NORMAL) { return; }
    Preferences prefs = getPreferences();
    if (prefs != null) {
      String bounds = getBoundsString();
      String existing = prefs.get("Bounds", "");
      if (!bounds.equals(existing)) {
        prefs.put("Bounds", bounds);
        storePreferences();
      }
    }
  }

  public void storeState() {
    /**/
  }

  @Override
  public void setUndecorated(boolean undecorated) {
    if (isDisplayable()) {
      dispose();
      super.setUndecorated(undecorated);
      pack();
      setVisible(true);
    } else {
      super.setUndecorated(undecorated);
    }
  }

  private static Dimension minFrameSize = null;

  private static Dimension _getMinFrameSize() {
    XFrame d = new XFrame();
    Container cp = d.getContentPane();

    cp.setPreferredSize(new Dimension(0, 0));
    cp.setMinimumSize(new Dimension(0, 0));
    d.pack();

    return d.getMinimumSize();
  }

  public static Dimension decorationSize() {
    if (minFrameSize == null) {
      minFrameSize = _getMinFrameSize();
    }
    return minFrameSize;
  }

  protected Rectangle getPreferredBounds() {
    Preferences prefs = getPreferences();
    String s = null;
    if (prefs != null) {
      s = prefs.get("Bounds", null);
    }
    return s != null ? parseBoundsString(s) : getDefaultBounds();
  }

  public Rectangle getDefaultBounds() {
    Toolkit tk = Toolkit.getDefaultToolkit();
    Dimension size = tk.getScreenSize();
    GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice device = env.getDefaultScreenDevice();
    GraphicsConfiguration config = device.getDefaultConfiguration();
    Insets insets = tk.getScreenInsets(config);

    size.width -= (insets.left + insets.right);
    size.height -= (insets.top + insets.bottom);

    int width = (int) (size.width * 0.65f);
    int height = (int) (size.height * 0.75f);

    // center
    int x = size.width / 2 - width / 2;
    int y = size.height / 2 - height / 2;

    return new Rectangle(x, y, width, height);
  }

  public static String getSizeString(java.awt.Window window) {
    Dimension size = window.getSize();
    return size.width + "," + size.height;
  }

  public static String getBoundsString(java.awt.Window window) {
    Rectangle b = window.getBounds();
    return b.x + "," + b.y + "," + b.width + "," + b.height;
  }

  public String getBoundsString() {
    return getBoundsString(this);
  }

  public static int parseInt(String s) {
    try {
      return Integer.parseInt(s);
    } catch (Exception e) {
      return 0;
    }
  }

  public static Dimension parseSizeString(String s) {
    String[] split = s.split(",");
    int width = parseInt(split[0]);
    int height = parseInt(split[1]);

    return new Dimension(width, height);
  }

  public static Rectangle parseBoundsString(String s) {
    String[] split = s.split(",");
    int x = parseInt(split[0]);
    int y = parseInt(split[1]);
    int width = parseInt(split[2]);
    int height = parseInt(split[3]);

    return new Rectangle(x, y, width, height);
  }

  protected Integer getPreferredState() {
    return null;
  }

  @Override
  protected void processKeyEvent(KeyEvent e) {
    if (!blocked) {
      super.processKeyEvent(e);
    }
  }

  protected boolean shouldClose() {
    return true;
  }

  @Override
  protected void processWindowEvent(final WindowEvent e) {
    if (!blocked) {
      if (e.getID() == WindowEvent.WINDOW_CLOSING) {
        if (shouldClose() == false) { return; }
      }
      super.processWindowEvent(e);
    }
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    if (!blocked) {
      super.processMouseEvent(e);
    }
  }

  @Override
  protected void processMouseMotionEvent(MouseEvent e) {
    if (!blocked) {
      super.processMouseMotionEvent(e);
    }
  }

  public void block() {
    if (!blocked) {
      savedCursor = getCursor();
      setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      blocked = true;
    }
  }

  public void unblock() {
    if (blocked) {
      setCursor(savedCursor);
      savedCursor = null;
      blocked = false;
    }
  }

  public void center() {
    Dimension size = getSize();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    int x = screenSize.width / 2 - size.width / 2;
    int y = screenSize.height / 2 - size.height / 2;

    setLocation(x, y);
  }

  public void center(java.awt.Window parent) {
    Rectangle bounds = parent.getBounds();
    Dimension size = getSize();
    Point origin;

    origin = new Point(bounds.x + (bounds.width - size.width) / 2, bounds.y + (bounds.height - size.height) / 2);

    setLocation(origin);
  }

  public void center(java.awt.Component parent) {
    Rectangle bounds = parent.getBounds();
    Point screenLoc = parent.getLocationOnScreen();
    Dimension size = getSize();
    Point origin;

    bounds.x = screenLoc.x;
    bounds.y = screenLoc.y;

    origin = new Point(bounds.x + (bounds.width - size.width) / 2, bounds.y + (bounds.height - size.height) / 2);

    setLocation(origin);
  }

  class WindowListener extends WindowAdapter {
    @Override
    public void windowIconified(WindowEvent e) {
      storeState();
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
      storeState();
    }

    @Override
    public void windowStateChanged(WindowEvent e) {
      storeState();
    }
  }

  class ComponentListener extends ComponentAdapter {
    @Override
    public void componentResized(ComponentEvent e) {
      if (storeTimer.isRunning()) {
        storeTimer.stop();
      }
      storeTimer.start();
    }

    @Override
    public void componentMoved(ComponentEvent e) {
      if (storeTimer.isRunning()) {
        storeTimer.stop();
      }
      storeTimer.start();
    }
  }
}
