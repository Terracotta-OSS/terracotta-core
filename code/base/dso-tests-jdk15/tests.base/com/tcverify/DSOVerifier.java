/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcverify;

import com.tc.util.Assert;
import com.tc.verify.VerificationException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Verifies that DSO is working correctly.
 */
public class DSOVerifier {

  private static final boolean       DEBUG       = false;

  private static final long          TIMEOUT     = 1 * 60 * 1000;                 // 1 minute
  private static final long          POLL_PERIOD = 1 * 1000;                      // 1 second

  private final int                  myID;
  private final int                  otherID;
  private final Map<Integer, String> verifierMap = new HashMap<Integer, String>();
  private final Integer              myIDInteger;
  private final Integer              otherIDInteger;

  public DSOVerifier(int myID, int otherID) {
    Assert.eval(myID != otherID);
    this.myID = myID;
    this.otherID = otherID;
    this.myIDInteger = new Integer(this.myID);
    this.otherIDInteger = new Integer(this.otherID);
  }

  private static void debug(String message) {
    if (DEBUG) {
      System.err.println(new Date() + ": " + message);
    }
  }

  public void verify() throws VerificationException {
    setValue(this.myID);

    long startTime = System.currentTimeMillis();
    boolean correct = false;

    while ((System.currentTimeMillis() - startTime) < TIMEOUT) {
      debug(myID + ": Fetching value from map.");
      String value = getValue(this.otherID);
      debug(myID + ": Got: " + value);
      if (value != null) {
        if (value.equals("PRESENT-" + otherID)) {
          correct = true;
          break;
        } else {
          throw new VerificationException("Got unexpected value '" + value + "' from other VM.");
        }
      }

      try {
        Thread.sleep(POLL_PERIOD);
      } catch (InterruptedException ie) {
        // whatever
      }
    }

    debug("All done. Correct? " + correct);
    if (!correct) throw new VerificationException("Waited " + (System.currentTimeMillis() - startTime)
                                                  + " milliseconds, but didn't get other VM's signal. Is DSO broken?");
  }

  private String getValue(int id) {
    debug("Returning value for " + id);
    return verifierMap.get(otherIDInteger);
  }

  private void setValue(int id) {
    debug("Setting value for " + id + " to " + id);
    verifierMap.put(myIDInteger, "PRESENT-" + this.myID);
  }

  public static void main(String[] args) {
    if (args.length != 2) {
      System.err.println("Usage:");
      System.err.println("  java " + DSOVerifier.class.getName() + " my-id other-id");
      System.exit(2);
    }

    DSOVerifier verifier = new DSOVerifier(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
    try {
      verifier.verify();
      System.out.println("L2-DSO-OK: Terracotta L2 DSO server is running and DSOing properly.");
      System.exit(0);
    } catch (Throwable t) {
      System.out.println("L2-DSO-FAIL: " + t.getMessage());
      t.printStackTrace();
      System.exit(1);
    }
  }
}