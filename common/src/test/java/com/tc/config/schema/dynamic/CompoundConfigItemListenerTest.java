/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

  @Override
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
