/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.options;

import java.awt.Component;

import javax.swing.Icon;

public interface IOption {
  String getName();

  String getLabel();

  Icon getIcon();

  Component getDisplay();

  void apply();

}
