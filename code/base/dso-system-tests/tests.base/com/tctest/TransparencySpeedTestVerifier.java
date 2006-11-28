/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

import com.tc.util.Assert;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author steve To change the template for this generated type comment go to Window&gt;Preferences&gt;Java&gt;Code
 *         Generation&gt;Code and Comments
 */
public class TransparencySpeedTestVerifier {
  private Map resultRoot = new HashMap();


  public boolean verify() throws Exception {
    synchronized (resultRoot) {
      int desiredSize = TransparencySpeedTestApp.MUTATOR_COUNT * TransparencySpeedTestApp.ADD_COUNT;
      while(resultRoot.size() < desiredSize) {
        System.out.println("Verifier SIZE:" + resultRoot.size());
        resultRoot.wait();
      }
      Assert.eval(resultRoot.size() == desiredSize);
      int count = 0;
      for (Iterator i = resultRoot.keySet().iterator(); i.hasNext();) {
        i.next();
        count++;
        // System.out.println("Got:" + count++ + " for:" + o);
      }
      Assert.eval(count == desiredSize);

      System.out.println("Done:" + count + " expected:" + desiredSize );
      return true;
    }
  }
}