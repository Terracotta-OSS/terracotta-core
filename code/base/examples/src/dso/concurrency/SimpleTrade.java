/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package dso.concurrency;

/**
 * Created by Alan Brown
 * Date: May 17, 2005
 * Time: 1:53:28 PM
 */

public class SimpleTrade {

    private int counter = 0;

    public synchronized void incrementCounter () {
	counter++;
    }

    public synchronized int getCounter() {
        return counter;
    }
}
