/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * FileInputStream.java
 *
 * Created on 24. Oktober 2004, 13:06
 */
/*
 * Copyright 2005-2007 Schlichtherle IT Services
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


import java.io.*;

import org.terracotta.agent.repkg.de.schlichtherle.io.ArchiveController.*;

/**
 * A drop-in replacement for {@link java.io.FileInputStream} which
 * provides transparent read access to archive entries as if they were
 * (virtual) files.
 * All file system operations in this class are
 * <a href="package-summary.html#atomicity">virtually atomic</a>.
 * <p>
 * To prevent exceptions to be thrown subsequently, client applications
 * should always close their streams using the following idiom:
 * <pre><code>
 * FileInputStream fis = new FileInputStream(file);
 * try {
 *     // access fis here
 * } finally {
 *     fis.close();
 * }
 * </code></pre>
 * <p>
 * Note that for various (mostly archive driver specific) reasons, the
 * <code>close()</code> method may throw an <code>IOException</code>, too.
 * Client applications need to deal with this appropriately, for example
 * by enclosing the entire block with another <code>try-catch</code>-block
 * <p>
 * Client applications cannot read from an entry in an archive file if an
 * automatic update is required but cannot get performed because other
 * <code>FileInputStream</code> or <code>FileOutputStream</code> instances
 * haven't been closed or garbage collected yet.
 * A {@link FileBusyException} is thrown by the constructors of this class
 * in this case.
 * <p>
 * Whether or not a client application can read from more than one entry
 * in the same archive archive file concurrently is an implementation
 * detail of the respective archive driver.
 * As of version 6.5, all archive drivers provided by TrueZIP don't restrict
 * this.
 * However, custom archive drivers provided by third parties may do so.
 * <p>
 * If a client application tries to exceed the number of entry streams
 * supported to operate on the same archive file concurrently, a
 * {@link FileBusyException} is thrown by the constructors of this class.
 * <p>
 * If you would like to use this class in order to copy files,
 * please consider using the <code>*copy*</code> methods in the {@link File}
 * class instead.
 * These methods provide ease of use, enhanced features, superior performance
 * and require less space in the temp file folder.
 *
 * @see <a href="package-summary.html#streams">Using Archive Entry Streams</a>
 * @see FileBusyException
 * @see File#cat
 * @see File#umount
 * @see File#update
 * @author Christian Schlichtherle
 * @version @version@
 */
public class FileInputStream extends FilterInputStream {

    /**
     * Behaves like the super class, but also supports archive entry files.
     *
     * @throws FileBusyException If the path denotes an archive entry and the
     *         archive driver does not support to create an additional stream
     *         for the archive file.
     * @throws FileNotFoundException On any other I/O related issue.
     */
    public FileInputStream(String name)
    throws FileNotFoundException {
        super(createInputStream(File.getDefaultArchiveDetector().createFile(name)));
    }

    /**
     * Behaves like the super class, but also supports archive entry files.
     *
     * @throws FileBusyException If the path denotes an archive entry and the
     *         archive driver does not support to create an additional stream
     *         for the archive file.
     * @throws FileNotFoundException On any other I/O related issue.
     */
    public FileInputStream(java.io.File file)
    throws FileNotFoundException {
        super(createInputStream(file));
    }

    /**
     * Behaves like the super class.
     *
     * @throws FileBusyException If the path denotes an archive entry and the
     *         archive driver does not support to create an additional stream
     *         for the archive file.
     * @since TrueZIP 6.4
     */
    public FileInputStream(FileDescriptor fd) {
        super(new java.io.FileInputStream(fd));
    }

    private static InputStream createInputStream(final java.io.File file)
    throws FileNotFoundException {
        try {
            if (file instanceof File) {
                final File smartFile = (File) file;
                smartFile.ensureNotVirtualRoot("cannot read");
                final File archive = smartFile.getEnclArchive();
                final String entryName = smartFile.getEnclEntryName();
                assert (archive != null) == (entryName != null);
                if (archive != null)
                    return archive.getArchiveController()
                            .createInputStream(entryName);
            }
        } catch (RfsEntryFalsePositiveException isNotArchive) {
        }
        return new java.io.FileInputStream(file);
    }

    public int read(byte b[]) throws IOException {
        return in.read(b, 0, b.length);
    }
}
