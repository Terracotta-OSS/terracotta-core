/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core;

public interface ConnectionAddressProvider {
  
  public static final int ROUND_ROBIN = 0; /*
                                             * current = -1 - initial condition current >= 0 - normal condition
                                             */
  public static final int LINEAR      = 1; /*
                                             * current = -1 - initial condition current >= 0 and < addresses.size() -
                                             * normal condition current >= addresses.size() - end condition
                                             */

  String getHostname();

  int getPortNumber();

  int getCount();

  boolean hasNext();

  ConnectionInfo getConnectionInfo();

  ConnectionInfo next();

  void setPolicy(int policy);

}