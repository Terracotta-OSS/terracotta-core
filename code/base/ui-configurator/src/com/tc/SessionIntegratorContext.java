/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import java.util.ResourceBundle;
import org.dijon.DictionaryResource;
import java.util.prefs.Preferences;

public class SessionIntegratorContext {
  public SessionIntegrator      client;
  public SessionIntegratorFrame frame;
  public ResourceBundle         bundle;
  public DictionaryResource     topRes;
  public Preferences            prefs;

  public String getMessage(String id) {
    return getString(id);
  }

  public String getString(String id) {
    return bundle.getString(id);
  }
  
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

  public Object getObject(String id) {
    return bundle.getObject(id);
  }

  public void toConsole(String msg) {
    client.toConsole(msg);
  }
}
