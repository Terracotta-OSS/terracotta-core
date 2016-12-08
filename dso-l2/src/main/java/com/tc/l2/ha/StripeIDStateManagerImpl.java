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
package com.tc.l2.ha;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.StripeID;
import com.tc.net.groups.StripeIDEventListener;
import com.tc.net.groups.StripeIDStateManager;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class StripeIDStateManagerImpl implements StripeIDStateManager, PrettyPrintable {
  private static final TCLogger                             logger               = TCLogging
                                                                                     .getLogger(StripeIDStateManagerImpl.class);

  private final CopyOnWriteArrayList<StripeIDEventListener> listeners            = new CopyOnWriteArrayList<>();
  private StripeID                                    stripeID = StripeID.NULL_ID;
  private final AtomicInteger                               unKnownIDCount       = new AtomicInteger(0);
  private final ClusterStatePersistor                       clusterStatePersistor;

  public StripeIDStateManagerImpl(ClusterStatePersistor clusterStatePersistor) {
    this.clusterStatePersistor = clusterStatePersistor;
    this.unKnownIDCount.set(loadStripeIDFromDB());
  }

  private int loadStripeIDFromDB() {
    StripeID id = clusterStatePersistor.getStripeID();
    return id.isNull() ? 1 : 0;
  }
  
  public StripeID getStripeID() {
    return stripeID;
  }

  @Override
  public boolean isStripeIDMatched(StripeID stripeID) {
    return !stripeID.isNull() && stripeID.equals(getStripeID());
  }

  private void putToStore(StripeID stripeID) {
    logger.info("putToStore " + stripeID);
    clusterStatePersistor.setStripeID(stripeID);
  }

  @Override
  public boolean verifyOrSaveStripeID(StripeID stripeID, boolean overwrite) {
    if (stripeID.isNull()) {
      logger.warn("Ignore null StripeID");
      return false;
    }

    StripeID oldID = this.stripeID;

    if (overwrite || oldID.isNull()) {
      putToStore(stripeID);
      if (oldID.isNull()) unKnownIDCount.decrementAndGet();
      logger.debug("Collected " + stripeID + " count: " + unKnownIDCount.get());

      notifyLocalStripeIDReady(stripeID);
    } else {
      if (!oldID.equals(stripeID)) {
        logger.error("Mismatch StripeID " + oldID + " with " + stripeID);
        return false;
      }
    }
    return true;
  }

  private void notifyLocalStripeIDReady(StripeID stripeID) {
    logger.info("Notify local StripeID ready");
    for (StripeIDEventListener listener : listeners) {
      listener.notifyStripeIDCreated(stripeID);
    }
  }

  @Override
  public void registerForStripeIDEvents(StripeIDEventListener listener) {
    listeners.add(listener);

    StripeID stripeID = getStripeID();
    if (!stripeID.isNull()) {
      listener.notifyStripeIDCreated(stripeID);
    }

    // one case is when fully loaded from DB.
    if (unKnownIDCount.get() == 0) {
      listener.notifyStripeIDMapReady();
    }
  }
  
  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.print(this.getClass().getName()).flush();
    out.print("stripeID: ").print(getStripeID()).flush();
    return out;
  }

}
