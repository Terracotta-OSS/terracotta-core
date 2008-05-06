/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

public interface XmlObjectHolder {
  final String    RESET        = "Reset";
  final KeyStroke RESET_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_G,
                                                        InputEvent.CTRL_DOWN_MASK);
  
  boolean isRequired();
  boolean isSet();
  void    set();
  void    unset();
}
