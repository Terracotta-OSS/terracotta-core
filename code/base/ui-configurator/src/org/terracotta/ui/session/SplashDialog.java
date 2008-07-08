/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.ui.session;

import org.dijon.Dialog;
import org.dijon.DialogResource;

import java.awt.Frame;

public class SplashDialog extends Dialog {
  public SplashDialog(Frame parent, boolean modal) {
    super(parent, modal);
  }
  
  public void load(DialogResource dialogRes) {
    super.load(dialogRes);
  }
}
