/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * FileOutputStream.java
 *
 * Created on 23. Oktober 2004, 01:08
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
 * A drop-in replacement for {@link java.io.FileOutputStream} which
 * provides transparent write access to archive entries as if they were
 * (virtual) files.
 * All file system operations in this class are
 * <a href="package-summary.html#atomicity">virtually atomic</a>.
 * <p>
 * To prevent exceptions to be thrown subsequently, client applications
 * should always close their streams using the following idiom:
 * <pre><code>
 * FileOutputStream fos = new FileOutputStream(file);
 * try {
 *     // access fos here
 * } finally {
 *     fos.close();
 * }
 * </code></pre>
 * <p>
 * Note that for various (mostly archive driver specific) reasons, the
 * <code>close()</code> method may throw an <code>IOException</code>, too.
 * Client applications need to deal with this appropriately, for example
 * by enclosing the entire block with another <code>try-catch</code>-block.
 * <p>
 * Client applications cannot write to an entry in an archive file if an
 * automatic update is required but cannot get performed because other
 * <code>FileInputStream</code> or <code>FileOutputStream</code> instances
 * haven't been closed or garbage collected yet.
 * A {@link FileBusyException} is thrown by the constructors of this class
 * in this case.
 * <p>
 * Whether or not a client application can write to more than one entry
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
 * @see File#setLenient
 * @author Christian Schlichtherle
 * @version @version@
 */
public class FileOutputStream extends FilterOutputStream {

    /**
     * Behaves like the super class, but also supports archive entry files.
     *
     * @throws FileBusyException If the path denotes an archive entry and the
     *         archive driver does not support to create an additional output
     *         stream for the archive file.
     * @throws FileNotFoundException On any other I/O related issue.
     */
    public FileOutputStream(String name)
    throws FileNotFoundException {
        super(createOutputStream(
                File.getDefaultArchiveDetector().createFile(name), false));
    }

    /**
     * Behaves like the super class, but also supports archive entry files.
     *
     * @throws FileBusyException If the path denotes an archive entry and the
     *         archive driver does not support to create an additional output
     *         stream for the archive file.
     * @throws FileNotFoundException On any other I/O related issue.
     */
    public FileOutputStream(String name, boolean append)
    throws FileNotFoundException {
        super(createOutputStream(
                File.getDefaultArchiveDetector().createFile(name), append));
    }

    /**
     * Behaves like the super class, but also supports archive entry files.
     *
     * @throws FileBusyException If the path denotes an archive entry and the
     *         archive driver does not support to create an additional output
     *         stream for the archive file.
     * @throws FileNotFoundException On any other I/O related issue.
     */
    public FileOutputStream(java.io.File file)
    throws FileNotFoundException {
        super(createOutputStream(file, false));
    }

    /**
     * Behaves like the super class, but also supports archive entry files.
     *
     * @throws FileBusyException If the path denotes an archive entry and the
     *         archive driver does not support to create an additional output
     *         stream for the archive file.
     * @throws FileNotFoundException On any other I/O related issue.
     */
    public FileOutputStream(java.io.File file, boolean append)
    throws FileNotFoundException {
        super(createOutputStream(file, append));
    }

    /**
     * Behaves like the super class.
     *
     * @throws FileBusyException If the path denotes an archive entry and the
     *         archive driver does not support to create an additional output
     *         stream for the archive file.
     * @since TrueZIP 6.4
     */
    public FileOutputStream(FileDescriptor fd) {
        super(new java.io.FileOutputStream(fd));
    }

    private static OutputStream createOutputStream(
            final java.io.File file,
            final boolean append)
    throws FileNotFoundException {
        try {
            if (file instanceof File) {
                final File smartFile = (File) file;
                smartFile.ensureNotVirtualRoot("cannot write");
                final File archive = smartFile.getEnclArchive();
                final String entryName = smartFile.getEnclEntryName();
                assert (archive != null) == (entryName != null);
                if (archive != null)
                    return archive.getArchiveController()
                            .createOutputStream(entryName, append);
            }
        } catch (RfsEntryFalsePositiveException isNotArchive) {
        }
        return new java.io.FileOutputStream(file, append);
    }

    public void write(byte[] buf, int off, int len) throws IOException {
        out.write(buf, off, len);
    }
}
