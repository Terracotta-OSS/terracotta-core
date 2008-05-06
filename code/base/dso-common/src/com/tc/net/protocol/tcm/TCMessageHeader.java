/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.net.protocol.TCNetworkHeader;

public interface TCMessageHeader extends TCNetworkHeader {

  final byte VERSION_1        = (byte) 1;

  public static final int   HEADER_LENGTH    = 2 * 4;

  final int  MIN_LENGTH       = HEADER_LENGTH;
  final int  MAX_LENGTH       = HEADER_LENGTH;
  
  public short getVersion();

  public int getHeaderLength();

  public int getMessageType();

  public int getMessageTypeVersion();

  public void setMessageType(int type);

  public void setMessageTypeVersion(int messageVersion);

}