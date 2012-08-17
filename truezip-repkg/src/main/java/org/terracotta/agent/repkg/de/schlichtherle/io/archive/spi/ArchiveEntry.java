/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ArchiveEntry.java
 *
 * Created on 26. Februar 2006, 19:08
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

package org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi;


import javax.swing.Icon;

import org.terracotta.agent.repkg.de.schlichtherle.io.ArchiveEntryMetaData;

/**
 * A simple interface for entries in an archive.
 * Drivers need to implement this interface in order to allow TrueZIP to
 * read and write entries for the supported archive types.
 * <p>
 * Implementations do <em>not</em> need to be thread safe:
 * Multithreading is addressed in the package {@link org.terracotta.agent.repkg.de.schlichtherle.io}.
 *
 * <h3><a name="entryName">Requirements for Archive Entry Names</a></h3>
 * <p>
 * TrueZIP 6 has the following requirements for archive entry names:
 * <ol>
 * <li>An entry name is a list of directory or file names whichs elements
 *     are separated by single separators ({@link #SEPARATOR}).
 * <li>An empty string (<code>&quot;&quot;</code>), a dot
 *     (<code>&quot;.&quot;</code>), or a dot-dot (<code>&quot;..&quot;</code>)
 *     is not permissible as a directory or file name.
 * <li>If an entry name starts with a separator, it's said to be
 *     <i>absolute</i>.
 *     Absolute entries are not accessible by client applications, but are
 *     retained if the archive is updated.
 * <li>If an entry name ends with a separator, it denotes a directory
 *     and vice versa.
 * </ol>
 * For example, <code>&quot;foo/bar&quot;</code> denotes a valid entry
 * name for a file, but <code>&quot;./abc/../foo/./def/./../bar/.&quot;</code>
 * would not, although both refer to the same entry.
 * <p>
 * As another example, <code>&quot;dir/&quot;</code> denotes a valid entry
 * name for a directory, but <code>&quot;dir&quot;</code> would not.
 * <p>
 * If an archive driver is to be written for an archive type which does not
 * support these requirements, an adapter for the entry name must be
 * implemented.
 * <p>
 * For example, the ZIP and TAR file formats conform to all but the second
 * requirement.
 * So the driver implementations for the archive types use
 * {@link org.terracotta.agent.repkg.de.schlichtherle.io.util.Paths#normalize(String, char)} to remove
 * any redundant elements from the path.
 * <p>
 * It's hoped that these requirements can be relaxed, but this would imply
 * some minor changes in the service provider interface for archive drivers,
 * so it's not going to happen in this major version number.
 *
 * @see org.terracotta.agent.repkg.de.schlichtherle.io.util.Paths#normalize(String, char) A utility
 *      method to remove empty and dot and dot-dot elements from an archive
 *      entry name
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 */
public interface ArchiveEntry {

    /**
     * The entry name separator as a string.
     *
     * @since TrueZIP 6.5
     * @see #SEPARATOR_CHAR
     */
    String SEPARATOR = "/";

    /**
     * The entry name separator as a character.
     *
     * @since TrueZIP 6.5
     * @see #SEPARATOR
     */
    char SEPARATOR_CHAR = '/';

    /** The unknown value for numeric properties. */
    byte UNKNOWN = -1;
    
    /**
     * Returns the archive entry name.
     *
     * @return A valid archive entry name.
     * @see <a href="#entryName">Requirements for Archive Entry Names</a>
     */
    String getName();

    /**
     * Returns <code>true</code> if and only if this entry represents a
     * directory.
     */
    boolean isDirectory();

    /**
     * Returns the (uncompressed) size of the archive entry in bytes,
     * or <code>UNKNOWN</code> if not specified.
     * This method is not meaningful for directory entries.
     */
    long getSize();

    // TODO: Add this in TrueZIP 7.
    /**
     * Sets the size of this archive entry.
     *
     * @param size The size of this archive entry in bytes.
     * @see #getSize
     */
    //void setSize(long size);

    /**
     * Returns the last modification time of this archive entry since the
     * epoch, or <code>UNKNOWN</code> if not specified.
     *
     * @see #setTime
     */
    long getTime();

    /**
     * Sets the last modification time of this archive entry.
     *
     * @param time The last modification time of this archive entry in
     *             milliseconds since the epoch.
     * @see #getTime
     */
    void setTime(long time);

    /**
     * Returns the icon that {@link org.terracotta.agent.repkg.de.schlichtherle.io.swing.tree.FileTreeCellRenderer}
     * should display for this entry if it is open/expanded in the view.
     * If <code>null</code> is returned, a default icon will be used,
     * depending on the type of this entry and its state in the view.
     */
    Icon getOpenIcon();

    /**
     * Returns the icon that {@link org.terracotta.agent.repkg.de.schlichtherle.io.swing.FileSystemView}
     * and {@link org.terracotta.agent.repkg.de.schlichtherle.io.swing.tree.FileTreeCellRenderer} should
     * display for this entry if it is closed/collapsed in the view.
     * If <code>null</code> is returned, a default icon will be used,
     * depending on the type of this entry and its state in the view.
     */
    Icon getClosedIcon();

    /**
     * Returns the meta data for this archive entry.
     * The default value is <code>null</code>.
     */
    ArchiveEntryMetaData getMetaData();

    /**
     * Sets the meta data for this archive entry.
     *
     * @param metaData The meta data - may not be <code>null</code>.
     */
    void setMetaData(ArchiveEntryMetaData metaData);
}
