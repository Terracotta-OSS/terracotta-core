/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ArchiveStatistics.java
 *
 * Created on 5. April 2006, 09:48
 */
/*
 * Copyright 2006 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terracotta.agent.repkg.de.schlichtherle.io;

/**
 * A proxy interface which encapsulates statistics about the total set of
 * archives operated by this package.
 * Client applications should never implement this interface; simply because
 * there is no need to and because this interface may be amended over time.
 *
 * @see File#getLiveArchiveStatistics
 *
 * @author  Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 */
public interface ArchiveStatistics {
    
    /**
     * Returns the total number of bytes read from all <em>non-enclosed</em>
     * archive files which are updated during {@link File#update()} or
     * {@link File#umount()}.
     * <p>
     * Please note that this method counts input from top level archive
     * files which require an update only, i.e. archive files which are
     * actually updated throughout the course of {@link File#update()} or
     * {@link File#update()} and are not enclosed in other archive
     * files and hence are present in the real file system.
     * <p>
     * This method is intended to be used for progress monitors and is a rough
     * indicator about what is going on inside the TrueZIP API.
     * The return value will be reset automatically where required,
     * so if this value is going to <code>0</code> again you know that a knew
     * update cycle has begun.
     * Other than this, you should not rely on its actual value.
     * <p>
     * For an example how to use this please refer to the source
     * code for <code>nzip.ProgressMonitor</code> in the base package.
     *
     * @see File#update
     * @see File#umount
     */
    long getUpdateTotalByteCountRead();
    
    /**
     * Returns the total number of bytes written to all <em>non-enclosed</em>
     * archive files which are updated during {@link File#update()} or
     * {@link File#umount()}.
     * <p>
     * Please note that this method counts output to top level archive
     * files which require an update only, i.e. archive files which are
     * actually updated throughout the course of {@link File#update()} or
     * {@link File#update()} and are not enclosed in other archive
     * files and hence are present in the real file system.
     * <p>
     * This method is intended to be used for progress monitors and is a rough
     * indicator about what is going on inside the TrueZIP API.
     * The return value will be reset automatically where required,
     * so if this value is going to <code>0</code> again you know that a knew
     * update cycle has begun.
     * Other than this, you should not rely on its actual value.
     * <p>
     * For an example how to use this please refer to the source
     * code for <code>nzip.ProgressMonitor</code> in the base package.
     *
     * @see File#update
     * @see File#umount
     */
    long getUpdateTotalByteCountWritten();

    /**
     * Returns the total number of archives operated by this package.
     */
    int getArchivesTotal();
    
    /**
     * Returns the number of archives which have been changed and
     * hence need to be processed on the next call to {@link File#update} or
     * {@link File#umount}.
     * Note that you should <em>not</em> use the returned value to call
     * <code>File.update()</code> or <code>File.umount()</code> only
     * conditionally - this is unreliable!
     * Instead, you should always call one of those methods unconditionally.
     */
    int getArchivesTouched();

    /**
     * Returns the total number of top level archives operated by this package.
     */
    int getTopLevelArchivesTotal();
    
    /**
     * Returns the number of top level archives which have been changed and
     * hence need to be processed on the next call to {@link File#update} or
     * {@link File#umount}.
     * Note that you should <em>not</em> use the returned value to call
     * <code>File.update()</code> or <code>File.umount()</code> only
     * conditionally - this is unreliable!
     * Instead, you should always call one of those methods unconditionally.
     */
    int getTopLevelArchivesTouched();
}
