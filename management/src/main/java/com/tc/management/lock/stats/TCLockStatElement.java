/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.lock.stats;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class TCLockStatElement implements TCSerializable, Serializable {
  private Collection lockStatElements;

  public TCLockStatElement() {
    return;
  }

  public TCLockStatElement(Collection lockStatElements) {
    this.lockStatElements = lockStatElements;
  }
  
  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    int length = serialInput.readInt();
    lockStatElements = new ArrayList();
    for (int i=0; i<length; i++) {
      LockStatElement lse = new LockStatElement();
      lse.deserializeFrom(serialInput);
      lockStatElements.add(lse);
    }

    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeInt(lockStatElements.size());
    for (Iterator i=lockStatElements.iterator(); i.hasNext(); ) {
      LockStatElement tcSerObj = (LockStatElement)i.next();
      tcSerObj.serializeTo(serialOutput);
    }
  }
  
  public Collection getLockStatElements() {
    return this.lockStatElements;
  }
}
