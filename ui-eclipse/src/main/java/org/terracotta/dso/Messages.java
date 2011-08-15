/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.osgi.util.NLS;

/**
 * This is the Eclipse way of doing I18N.  We may go there after everything
 * is working properly.
 */

final public class Messages extends NLS {
  private static final String BUNDLE_NAME = Messages.class.getName();

  private Messages() {/**/}

  public static String Editor_warning_save_delete;
  public static String Editor_error_save_title;
  public static String Editor_error_save_message;
  
  static {
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }
}
