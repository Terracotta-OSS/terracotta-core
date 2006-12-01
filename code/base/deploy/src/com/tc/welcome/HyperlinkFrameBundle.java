/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.welcome;

import java.util.ListResourceBundle;

public class HyperlinkFrameBundle extends ListResourceBundle {
  public Object[][] getContents() {
    return contents;
  }

  static final Object[][] contents = {
    {"file.menu.title", "File"},
    {"help.menu.title", "Help"},
    {"visit.forums.title", "Visit Terracotta Forums"},
    {"forums.url", "http://www.terracottatech.com/forums/"},
    {"contact.support.title", "Contact Terracotta Technical Support"},
    {"support.url", "http://www.terracottatech.com/support_services.shtml"},
    {"contact.field.eng.title", "Contact Terracotta Field Engineering"},
    {"field.eng.url", "http://www.terracottatech.com/contact/field/"},
    {"contact.sales.title", "Contact Terracotta Sales"},
    {"sales.url", "http://www.terracottatech.com/contact/"},
    {"page.load.error", "Unable to load page."},
    {"about.title.prefix", "About "},
    {"quit.action.name", "Exit"}
  };
}
