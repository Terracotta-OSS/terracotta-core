/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import com.tc.test.TCTestCase;

/**
 * Unit test for {@link CompoundConfigItemListener}.
 */
public class CompoundConfigItemListenerTest extends TCTestCase {

  private MockConfigItemListener     listener1;
  private MockConfigItemListener     listener2;

  private CompoundConfigItemListener listener;

  private Object                     oldValue1;
  private Object                     oldValue2;
  private Object                     newValue1;
  private Object                     newValue2;

  public void setUp() throws Exception {
    this.listener1 = new MockConfigItemListener();
    this.listener2 = new MockConfigItemListener();

    this.listener = new CompoundConfigItemListener();

    this.oldValue1 = new Object();
    this.oldValue2 = new Object();
    this.newValue1 = new Object();
    this.newValue2 = new Object();
  }

  public void testAll() throws Exception {
    try {
      this.listener.addListener(null);
      fail("Didn't get NPE on no listener");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      this.listener.removeListener(null);
      fail("Didn't get NPE on no listener");
    } catch (NullPointerException npe) {
      // ok
    }

    this.listener.valueChanged(this.oldValue1, this.newValue1);
    assertEquals(0, this.listener1.getNumValueChangeds());
    assertEquals(0, this.listener2.getNumValueChangeds());

    this.listener1.reset();
    this.listener2.reset();

    this.listener.addListener(this.listener1);
    this.listener.valueChanged(this.oldValue1, this.newValue1);

    assertEquals(1, this.listener1.getNumValueChangeds());
    assertSame(this.oldValue1, this.listener1.getLastOldValue());
    assertSame(this.newValue1, this.listener1.getLastNewValue());
    assertEquals(0, this.listener2.getNumValueChangeds());

    this.listener1.reset();
    this.listener2.reset();

    this.listener.addListener(this.listener2);
    this.listener.valueChanged(this.oldValue2, this.newValue2);

    assertEquals(1, this.listener1.getNumValueChangeds());
    assertSame(this.oldValue2, this.listener1.getLastOldValue());
    assertSame(this.newValue2, this.listener1.getLastNewValue());
    assertEquals(1, this.listener2.getNumValueChangeds());
    assertSame(this.oldValue2, this.listener2.getLastOldValue());
    assertSame(this.newValue2, this.listener2.getLastNewValue());

    this.listener1.reset();
    this.listener2.reset();

    this.listener.removeListener(this.listener1);
    this.listener.valueChanged(this.oldValue1, this.newValue1);

    assertEquals(0, this.listener1.getNumValueChangeds());
    assertEquals(1, this.listener2.getNumValueChangeds());
    assertSame(this.oldValue1, this.listener2.getLastOldValue());
    assertSame(this.newValue1, this.listener2.getLastNewValue());

    this.listener1.reset();
    this.listener2.reset();

    this.listener.removeListener(this.listener2);
    this.listener.valueChanged(this.oldValue2, this.newValue2);

    assertEquals(0, this.listener1.getNumValueChangeds());
    assertEquals(0, this.listener2.getNumValueChangeds());
  }

}
