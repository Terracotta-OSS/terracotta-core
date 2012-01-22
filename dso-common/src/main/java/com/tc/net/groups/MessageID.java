/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.util.AbstractIdentifier;

public class MessageID extends AbstractIdentifier {

  public static final MessageID NULL_ID = new MessageID();

  public MessageID(long id) {
    super(id);
  }

  private MessageID() {
    super();
  }

  public String getIdentifierType() {
    return "MessageID";
  }

}
