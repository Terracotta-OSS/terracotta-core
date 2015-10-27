/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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
