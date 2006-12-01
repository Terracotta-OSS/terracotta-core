/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.Timer;

import org.dijon.Frame;

public class XFrame extends Frame {
  private Timer storeTimer;

  public XFrame() {
    super();

    storeTimer = new Timer(1000, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        storeBounds();
      }
    });
    storeTimer.setRepeats(false);

    addComponentListener(new ComponentListener());
  }

  public void storeBounds() {/**/}

  class ComponentListener extends ComponentAdapter {
    public void componentResized(ComponentEvent e) {
      if(storeTimer.isRunning()) {
        storeTimer.stop();
      }
      storeTimer.start();
    }
    public void componentMoved(ComponentEvent e) {
      if(storeTimer.isRunning()) {
        storeTimer.stop();
      }
      storeTimer.start();
    }
  }
}
