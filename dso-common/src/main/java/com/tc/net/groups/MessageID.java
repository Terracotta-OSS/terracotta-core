/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
