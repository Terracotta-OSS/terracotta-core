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
package com.tc.lang;


import com.tc.logging.NullTCLogger;

import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

public class StartupHelperTest extends TestCase {

  public void testException() throws Throwable {
    final AtomicReference<Throwable> error = new AtomicReference(null);

    ThreadGroup group = new ThreadGroup("group") {
      @Override
      public void uncaughtException(Thread t, Throwable e) {
        error.set(e);
      }
    };

    final RuntimeException re = new RuntimeException("da bomb");

    StartupHelper helper = new StartupHelper(group, new StartupHelper.StartupAction() {
      @Override
      public void execute() throws Throwable {
        throw re;
      }
    });

    try {
      helper.startUp();
    } catch (RuntimeException e) {
      //
    }

    RuntimeException thrown = (RuntimeException) error.get();
    if (thrown == null) {
      fail("no exception delivered to group");
    }

    assertTrue(thrown == re);
  }

  public void testGroup() throws Throwable {
    final TCThreadGroup group = new TCThreadGroup(new ThrowableHandlerImpl(new NullTCLogger()));

    StartupHelper helper = new StartupHelper(group, new StartupHelper.StartupAction() {
      @Override
      public void execute() throws Throwable {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        if (tg != group) { throw new AssertionError("wrong thread group: " + tg); }
      }
    });

    helper.startUp();
  }

}
