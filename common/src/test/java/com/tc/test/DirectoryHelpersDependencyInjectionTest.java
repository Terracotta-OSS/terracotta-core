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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(TCExtension.class)
@ExtendWith(DirectoryHelperExtension.class)
//@TimeBomb("2999-12-31", Vm.isIBM(), null)
public class DirectoryHelpersDependencyInjectionTest {

  @CleanDirectory(true)
  private TempDirectoryHelper tempDirectoryHelper;

  private DataDirectoryHelper dataDirectoryHelper;

  @Test
  public void testDirectoryHelpersInjection() {

    Assertions.assertAll("Directory Helpers are not injected by TCExtension",
        () -> Assertions.assertNotNull(tempDirectoryHelper),
        () -> Assertions.assertNotNull(dataDirectoryHelper)
    );
  }
  
  @Test
  public void testHeapDump() throws IOException {
    assertEquals(0, getHprofs().length);
    TCExtension.dumpHeap(tempDirectoryHelper.getDirectory());
    assertEquals(1, getHprofs().length);
    TCExtension.dumpHeap(tempDirectoryHelper.getDirectory());
    assertEquals(2, getHprofs().length);
    TCExtension.dumpHeap(tempDirectoryHelper.getDirectory());
  }

  private File[] getHprofs() throws IOException {
    return tempDirectoryHelper.getDirectory().listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(".hprof");
      }
    });
  }
}
