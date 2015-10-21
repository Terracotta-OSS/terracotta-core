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

import com.tc.util.AbstractIdentifier;

/**
 * Identifier for a client session
 * 
 * @author steve
 */
public class ChannelID extends AbstractIdentifier {
  /**
   * Indicates no ID
   */
  public static final ChannelID NULL_ID      = new ChannelID();
  
  /**
   * Construct with specific id value
   * @param id ID value
   */
  public ChannelID(long id) {
    super(id);
  }

  private ChannelID() {
    super();
  }

  @Override
  public String getIdentifierType() {
    return "ChannelID";
  }
}
