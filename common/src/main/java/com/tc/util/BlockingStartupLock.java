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
package com.tc.util;

import com.tc.io.TCFile;

import java.io.IOException;
import java.nio.channels.OverlappingFileLockException;

/**
 * Class for testing if it is safe to startup a process on a specified directory (i.e. will it corrupt a db)
 */
public class BlockingStartupLock extends AbstractStartupLock implements StartupLock {

  public BlockingStartupLock(TCFile location, boolean retries) {
    super(location, retries);
  }

  @Override
  protected void requestLock(TCFile tcFile) {
    try {
      this.isBlocked = true;
      lock = channel.lock();
    } catch (OverlappingFileLockException e) {
      // File is already locked in this thread or virtual machine
      throw new AssertionError(e);
    } catch (IOException ioe) {
      throw new TCDataFileLockingException("Unable to acquire file lock on '" + tcFile.getFile().getAbsolutePath()
                                           + "'.  Aborting Terracotta server instance startup.");
    } finally {
      this.isBlocked = false;
    }
  }

  @Override
  public boolean isBlocked() {
    return this.isBlocked;
  }
}
