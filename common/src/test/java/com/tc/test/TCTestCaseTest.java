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
package com.tc.test;

import com.tc.util.runtime.Vm;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class TCTestCaseTest extends TCTestCase {

  public TCTestCaseTest() {
    if (Vm.isIBM()) {
      disableTest();
    }
  }

  public void testHeapDump() throws IOException {
    assertEquals(0, getHprofs().length);
    dumpHeap(getTempDirectory());
    assertEquals(1, getHprofs().length);
    dumpHeap(getTempDirectory());
    assertEquals(2, getHprofs().length);
    dumpHeap(getTempDirectory());
  }

  private File[] getHprofs() throws IOException {
    return getTempDirectory().listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(".hprof");
      }
    });
  }

}
