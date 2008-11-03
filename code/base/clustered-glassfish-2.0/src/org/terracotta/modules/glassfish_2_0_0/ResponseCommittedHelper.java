/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.glassfish_2_0_0;

import com.tc.tomcat50.session.SessionResponse50;

import javax.servlet.ServletResponse;

public class ResponseCommittedHelper {

  public static final String CLASS = ResponseCommittedHelper.class.getName().replace('.', '/');

  public static boolean isCommitted(ServletResponse response) {
    if (response instanceof SessionResponse50) {
      return ((SessionResponse50)response).isCommittedForce();
    }
    return response.isCommitted();
  }

}
