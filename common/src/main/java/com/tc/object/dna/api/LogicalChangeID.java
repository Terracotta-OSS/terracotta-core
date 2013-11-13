/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.api;

import com.tc.util.AbstractIdentifier;

public class LogicalChangeID extends AbstractIdentifier {
  public static LogicalChangeID NULL_ID = new LogicalChangeID();

  private LogicalChangeID() {
    //
  }

  public LogicalChangeID(long id) {
    super(id);
  }

  @Override
  public String getIdentifierType() {
    return "LogicalChangeID";
  }

}
