/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ArchiveDriver.java
 *
 * Created on 25. Februar 2006, 16:59
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


import java.io.CharConversionException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.Icon;

import org.terracotta.agent.repkg.de.schlichtherle.io.File;
import org.terracotta.agent.repkg.de.schlichtherle.io.FileInputStream;
import org.terracotta.agent.repkg.de.schlichtherle.io.FileOutputStream;
import org.terracotta.agent.repkg.de.schlichtherle.io.archive.Archive;
import org.terracotta.agent.repkg.de.schlichtherle.io.rof.ReadOnlyFile;

/**
 * This "driver" interface is an abstract factory which is used in a builder
 * pattern to read and write archives of any particular type
 * - such as ZIP, TZP, JAR, TAR, TAR.GZ, TAR.BZ2 or others.
 * Archive drivers may be shared by multiple archive controllers and instances
 * of the {@link org.terracotta.agent.repkg.de.schlichtherle.io.ArchiveDetector} interface.
 * <p>
 * The following requirements must be met by any implementation:
 * <ul>
 * <li>Implementations must be thread safe and (at least virtually) immutable:
 *     Otherwise, the class {@link org.terracotta.agent.repkg.de.schlichtherle.io.File} may seem to
 *     behave non-deterministic and may even throw exceptions where it
 *     shouldn't.
 * <li>Implementations must not assume that they are used as singletons:
 *     Multiple instances of an implementation may be used for the same
 *     archive type.
 * <li>If the driver should be supported by the driver registration process of
 *     the class {@link org.terracotta.agent.repkg.de.schlichtherle.io.DefaultArchiveDetector},
 *     a no-arguments constructor must be provided.
 * <li>Although not required, since TrueZIP 6.4 it's recommended for
 *     implementations to implement the {@link java.io.Serializable} interface,
 *     too, so that {@link org.terracotta.agent.repkg.de.schlichtherle.io.File} instances which are
 *     indirectly referring to it can be serialized.
 * </ul>
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 */
public interface ArchiveDriver {

    /**
     * Creates a new input archive for <code>archive</code>
     * from the given read only file.
     * <p>
     * Note that if an exception is thrown, the method must be reentrant!
     * In addition, the exception type determines the behaviour of the classes
     * {@link File}, {@link FileInputStream} and {@link FileOutputStream}
     * as follows:
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Exception type</td>
     *   <th>{@link File#isFile}</th>
     *   <th>{@link File#isDirectory}</th>
     *   <th>{@link File#exists}</th>
     *   <th>{@link File#delete}</th>
     * </tr>
     * <tr>
     *   <td>{@link FileNotFoundException}</td>
     *   <td><b><code>false</code></b></td>
     *   <td><code>false</code></td>
     *   <td><code>true</code></td>
     *   <td><code>true</code> (unless prohibited by the real file system)</td>
     * </tr>
     * <tr>
     *   <td>{@link IOException}</td>
     *   <td><b><code>true</code></b></td>
     *   <td><code>false</code></td>
     *   <td><code>true</code></td>
     *   <td><code>true</code> (unless prohibited by the real file system)</td>
     * </tr>
     * </table>
     * 
     * @param archive The abstract archive representation which TrueZIP's
     *        internal <code>ArchiveController</code> is processing
     *        - never <code>null</code>.
     * @param rof The {@link ReadOnlyFile} to read the actual archive
     *        contents from - never <code>null</code>.
     *        Hint: If you'ld prefer to have an <code>InputStream</code>,
     *        you could decorate this parameter with a
     *        {@link org.terracotta.agent.repkg.de.schlichtherle.io.rof.ReadOnlyFileInputStream}.
     * @return A new input archive instance.
     * @throws TransientIOException If calling this method for the same
     *         archive file again may finally succeed.
     *         This exception is associated with another <code>IOException</code>
     *         as its cause which is unwrapped and interpreted as below.
     * @throws FileNotFoundException If the input archive is inaccessible
     *         for any reason and you would like the package
     *         {@link org.terracotta.agent.repkg.de.schlichtherle.io} to mask the archive as a special
     *         file which cannot get read, written or deleted.
     * @throws IOException On any other I/O or data format related issue
     *         when reading the input archive and you would like the package
     *         {@link org.terracotta.agent.repkg.de.schlichtherle.io} to treat the archive like a regular
     *         file which may be read, written or deleted.
     * @see InputArchive
     */
    InputArchive createInputArchive(
            Archive archive,
            ReadOnlyFile rof)
    throws IOException;

    /**
     * Creates a new archive entry for <code>entryName</code>
     * for use with an {@link OutputArchive}.
     * 
     * @param archive The abstract archive representation which TrueZIP's
     *        internal <code>ArchiveController</code> is processing
     *        - never <code>null</code>.
     * @param entryName A valid archive entry name  - never <code>null</code>.
     * @param template If not <code>null</code>, then the newly created entry
     *        shall inherit as much attributes from this object as possible
     *        (with the exception of the name).
     *        This is typically used for archive copy operations.
     *        Note that there is no guarantee on the runtime type of this
     *        object; it may have been created by other drivers.
     *        It is safe to ignore the <code>metaData</code> property when
     *        copying entries.
     * @return A new archive entry instance.
     * @throws CharConversionException If <code>name</code> contains
     *         illegal characters.
     * @see <a href="ArchiveEntry.html#entryName">Requirements for Archive Entry Names</a>
     */
    ArchiveEntry createArchiveEntry(
            Archive archive,
            String entryName,
            ArchiveEntry template)
    throws CharConversionException;

    /**
     * Creates a new output archive for <code>archive</code>
     * from the given output stream.
     * 
     * @param archive The abstract archive representation which TrueZIP's
     *        internal <code>ArchiveController</code> is processing
     *        - never <code>null</code>.
     * @param out The {@link OutputStream} to write the archive entries to
     *        - never <code>null</code>.
     * @param source The source {@link InputArchive} if
     *        <code>archive</code> is going to get updated.
     *        If not <code>null</code>, this is guaranteed to be a product
     *        of this driver's {@link #createInputArchive} method.
     *        This may be used to copy some meta data which is specific to
     *        the type of archive this driver supports.
     *        For example, this could be used to copy the comment of a ZIP
     *        file.
     * @return A new output archive instance.
     * @throws TransientIOException If calling this method for the same
     *         archive file again may finally succeed.
     *         This exception is associated with another <code>IOException</code>
     *         as its cause which is unwrapped and interpreted as below.
     * @throws FileNotFoundException If the output archive is inaccessible
     *         for any reason.
     * @throws IOException On any other I/O or data format related issue
     *         when writing the output archive.
     * @see OutputArchive
     */
    OutputArchive createOutputArchive(
            Archive archive,
            OutputStream out,
            InputArchive source)
    throws IOException;
    
    /**
     * Returns the icon that {@link org.terracotta.agent.repkg.de.schlichtherle.io.swing.tree.FileTreeCellRenderer}
     * should display for the given archive.
     *
     * @param archive The abstract archive representation which TrueZIP's
     *        internal <code>ArchiveController</code> is processing
     *        - never <code>null</code>.
     * @return The icon that should be displayed for the given archive if is
     *         is open/expanded in the view.
     *         If <code>null</code> is returned, a default icon should be used.
     */
    Icon getOpenIcon(Archive archive);

    /**
     * Returns the icon that {@link org.terracotta.agent.repkg.de.schlichtherle.io.swing.FileSystemView}
     * and {@link org.terracotta.agent.repkg.de.schlichtherle.io.swing.tree.FileTreeCellRenderer} should
     * display for the given archive.
     *
     * @param archive The abstract archive representation which TrueZIP's
     *        internal <code>ArchiveController</code> is processing
     *        - never <code>null</code>.
     * @return The icon that should be displayed for the given archive if is
     *         is closed/collapsed in the view.
     *         If <code>null</code> is returned, a default icon should be used.
     */
    Icon getClosedIcon(Archive archive);

    /**
     * Archive drivers will be put into hash maps as keys,
     * so be sure to implement this properly.
     * Note that this is just a reinforcement of the general contract for
     * {@link Object#equals} and the best possible implementation is the
     * default implementation in <code>Object</code> which is most
     * discriminating.
     *
     * @since TrueZIP 6.4
     */
    boolean equals(Object o);

    /**
     * Archive drivers will be put into hash maps as keys,
     * so be sure to implement this properly.
     * Note that this is just a reinforcement of the general contract for
     * {@link Object#hashCode} and the best possible implementation is the
     * default implementation in <code>Object</code> which is most
     * discriminating.
     *
     * @since TrueZIP 6.4
     */
    int hashCode();
}
