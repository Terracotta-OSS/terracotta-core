/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.TestDNACursor;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.managedobject.ManagedObjectStateSerializationTestBase;

public class ManagedObjectStateSerialization15Test extends ManagedObjectStateSerializationTestBase {

  public void testEnum() throws Exception {
    final String className = "java.lang.Enum";
    final State state = State.RUN;
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLiteralAction(state);
    final ManagedObjectState managedObjectState = applyValidation(className, cursor);
    serializationValidation(managedObjectState, cursor, ManagedObjectState.LITERAL_TYPE);
  }

  public interface EnumIntf {
    public int getStateNum();

    public void setStateNum(int stateNum);
  }

  public enum State implements EnumIntf {
    START(0), RUN(1), STOP(2);

    private int stateNum;

    State(final int stateNum) {
      this.stateNum = stateNum;
    }

    public int getStateNum() {
      return this.stateNum;
    }

    public void setStateNum(final int stateNum) {
      this.stateNum = stateNum;
    }
  }

}
