/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import java.util.ListResourceBundle;

public class CommonBundle extends ListResourceBundle {
  public Object[][] getContents() {
    return new Object[][] {
        { "forums.url", "http://www.terracotta.org/kit/reflector?kitID={0}&pageID=Forums" },
        { "support.url", "http://www.terracotta.org/kit/reflector?kitID={0}&pageID=SupportServices" },
        { "visit.forums.title", "Visit Terracotta Forums" },
        { "contact.support.title", "Contact Terracotta Technical Support" }
    };
  }
}
