/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
