/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import java.util.ResourceBundle;
import org.dijon.DictionaryResource;
import java.util.prefs.Preferences;
import javax.swing.UIDefaults;

public class AdminClientContext {
  public AdminClient           client;
  public AdminClientController controller;
  public UIDefaults            uiDefaults;
  public ResourceBundle        bundle;
  public DictionaryResource    topRes;
  public Preferences           prefs;

  /**
   * Load a message string from Resources.java.
   */
  public String getMessage(String id) {
    return getString(id);
  }

  /**
   * Load a string string from Resources.java.
   */
  public String getString(String id) {
    return bundle.getString(id);
  }
  
  /**
   * Load an array of messages from Resources.java.
   */
  public String[] getMessages(String[] ids) {
    String[] result = null;

    if(ids != null && ids.length > 0) {
      int count = ids.length;

      result = new String[count];

      for(int i = 0; i < count; i++) {
        result[i] = getMessage(ids[i]);
      }
    }

    return result;
  }


  /**
   * Load an object from Resources.java.
   */
  public Object getObject(String id) {
    return bundle.getObject(id);
  }

  /**
   * Log a message to the AdminClientController
   */
  public void log(String msg) {
    controller.log(msg);
  }

  /*
   * Log an exception to the AdminClientController
   */
  public void log(Exception e) {
    controller.log(e);
  }

  /**
   * Set a message on the AdminClientController
   */
  public void setStatus(String msg) {
    controller.setStatus(msg);
  }
}
