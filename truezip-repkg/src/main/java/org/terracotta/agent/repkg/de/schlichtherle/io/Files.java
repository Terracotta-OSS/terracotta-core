/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * Files.java
 *
 * Created on 8. Januar 2007, 16:59
 */
/*
 * Copyright 2007 Schlichtherle IT Services
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
import java.util.*;

import org.terracotta.agent.repkg.de.schlichtherle.io.ArchiveController.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.ArchiveFileSystem.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.util.*;

/**
 * Provides static utility methods for {@link File}s.
 * Note that in contrast to the {@link File} class, the methods in this
 * class accept and return plain <code>java.io.File</code> instances.
 * Full advantage is taken if a parameter is actually an instance of the
 * <code>File</code> class in this package, however.
 * <p>
 * <b>TODO:</b> Consider making this class public in TrueZIP 7 and remove the
 * stub methods for the same purpose in {@link File}.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.5
 */
final class Files {

    /**
     * A lock used when copying data from one archive file to another.
     * This lock must be acquired before any other locks on the controllers
     * are acquired in order to prevent dead locks.
     */
    private static final CopyLock copyLock = new CopyLock();

    /** This class cannot get instantiated. */
    protected Files() {
    }

    /**
     * @see File#contains
     */
    public static boolean contains(java.io.File a, java.io.File b) {
        a = getCanOrAbsFile(a);
        b = getCanOrAbsFile(b);
        return contains(a.getPath(), b.getPath());
    }

    /**
     * Returns true if and only if the <code>pathA</code> contains
     * <code>pathB</code>.
     * 
     * @param pathA A valid file path.
     * @param pathB A valid file path.
     * @throws NullPointerException If any parameter is <code>null</code>.
     */
    static boolean contains(String pathA, String pathB) {
        // Windows is just case preserving, all others are case sensitive.
        if (File.separatorChar == '\\') {
            pathA = pathA.toLowerCase();
            pathB = pathB.toLowerCase();
        }
        if (!pathB.startsWith(pathA))
            return false;
        final int lengthA = pathA.length();
        final int lengthB = pathB.length();
        if (lengthA == lengthB)
            return true;
        else if (lengthA < lengthB)
            return pathB.charAt(lengthA) == File.separatorChar;
        return false;
    }

    /**
     * Returns the canonical form of the given file or the normalized absolute
     * form if resolving the prior fails.
     *
     * @return The canonical or absolute path of this file as a
     *         <code>java.io.File</code> instance.
     * @throws NullPointerException If <code>file</code> is <code>null</code>.
     */
    public static java.io.File getCanOrAbsFile(java.io.File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException ex) {
            final java.io.File parent = file.getParentFile();
            return normalize(parent != null
                    ? new java.io.File(getCanOrAbsFile(parent), file.getName())
                    : file.getAbsoluteFile());
        }
    }

    /**
     * Removes any <code>&quot;.&quot;</code> and <code>&quot;..&quot;</code>
     * directories from the path wherever possible.
     *
     * @param file The file instance which's path is to be normalized.
     * @return <code>file</code> if it was already in normalized form.
     *         Otherwise, an object which's runtime class is guaranteed to
     *         be <code>java.io.File</code>.
     */
    public static java.io.File normalize(final java.io.File file) {
        final String path = file.getPath();
        final String newPath = Paths.normalize(path, File.separatorChar);
        return newPath != path // mind contract of Paths.normalize!
                ? new java.io.File(newPath)
                : file;
    }

    /**
     * Returns <code>true</code> if the given file exists or can be created
     * and at least one byte can be successfully written to it - the file is
     * restored to its previous state afterwards.
     * This is a much stronger test than {@link File#canWrite()}.
     * <p>
     * Please note that if the file is actually open for reading or other
     * activities this method may not be able to reset the last modification
     * time of the file after testing, in which case <code>false</code> is
     * returned.
     * This is known to apply to the Windows platform, but not on Unix
     * platforms.
     */
    public static boolean isWritableOrCreatable(final java.io.File file) {
        try {
            if (!file.exists()) {
                final boolean created = file.createNewFile();
                boolean ok = isWritableOrCreatable(file);
                if (created && !file.delete())
                    ok = false; // be conservative!
                return ok;
            } else if (file.canWrite()) {
                // Some operating and file system combinations make File.canWrite()
                // believe that the file is writable although it's not.
                // We are not that gullible, so let's test this...
                final long time = file.lastModified();
                if (!file.setLastModified(time + 1)) {
                    // This may happen on Windows and normally means that
                    // somebody else has opened this file
                    // (regardless of read or write mode).
                    // Be conservative: We don't allow writing to this file!
                    return false;
                }

                boolean ok;
                try {
                    // Open the file for reading and writing, requiring any
                    // update to its contents to be written to the filesystem
                    // synchronously.
                    // As Dr. Simon White from Catalysoft, Cambridge, UK reported,
                    // "rws" does NOT work on Mac OS X with Apple's Java 1.5
                    // Release 1 (equivalent to Sun's Java 1.5.0_02), however
                    // it DOES work with Apple's Java 1.5 Release 3.
                    // He also confirmed that "rwd" works on Apple's
                    // Java 1.5 Release 1, so we use this instead.
                    // Thank you very much for spending the time to fix this
                    // issue, Dr. White!
                    final RandomAccessFile raf = new RandomAccessFile(file, "rwd");
                    try {
                        final boolean empty;
                        int octet = raf.read();
                        if (octet == -1) {
                            octet = 0; // assume first byte is 0
                            empty = true;
                        } else {
                            empty = false;
                        }

                        // Let's test if we can (over)write the first byte.
                        raf.seek(0);
                        raf.write((octet ^ -1) & 0xFF); // write complement
                        try {
                            // Rewrite original content and check success.
                            raf.seek(0);
                            raf.write(octet);
                            raf.seek(0);
                            final int check = raf.read();
                            // This should always return true unless the storage
                            // device is faulty.
                            ok = octet == check;
                        } finally {
                            if (empty)
                                raf.setLength(0);
                        }
                    } finally {
                        raf.close();
                    }
                } finally {
                    if (!file.setLastModified(time)) {
                        // This may happen on Windows and normally means that
                        // somebody else has opened this file meanwhile
                        // (regardless of read or write mode).
                        // Be conservative: We don't allow (further) writing to
                        // this file!
                        ok = false;
                    }
                }
                return ok;
            } else { // if (file.exists() && !file.canWrite()) {
                return false;
            }
        } catch (IOException ex) {
            return false; // don't allow writing if anything goes wrong!
        }
    }

    //
    // Move, copy and remove methods:
    //

    /**
     * Moves the source to the destination by recursively copying and deleting
     * its files and directories.
     * Hence, this file system operation works even with archive files or
     * entries within archive files, but is <em>not</em> atomic.
     * <p>
     * The name of this method is inspired by the Unix command line utility
     * <code>mv</code> although in most cases it performs a plain rename
     * operation rather than a copy-and-delete operation.
     *
     * @param src The source file or directory.
     *            This must exist.
     * @param dst The destination file or directory.
     *            This may or may not exist.
     *            If it does, its contents are overwritten.
     * @param detector The object used to detect any archive
     *        files in the path and configure their parameters.
     * @return Whether the operation succeeded or not.
     *         If it fails, the source and destination may contain only a
     *         subset of the source before this operation.
     *         However, each file has either been completely moved or not.
     * @see File#renameTo(java.io.File, ArchiveDetector)
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    public static final boolean mv(
            final java.io.File src,
            final java.io.File dst,
            final ArchiveDetector detector) {
        return !contains(src, dst) && mv0(src, dst, detector);
    }

    private static boolean mv0(
            final java.io.File src,
            final java.io.File dst,
            final ArchiveDetector detector) {
        boolean ok = true;
        if (src.isDirectory()) {
            final long srcLastModified = src.lastModified();
            final boolean srcIsArchived = src instanceof File
                    && ((File) src).getInnerArchive() != null;
            final boolean dstIsArchived = dst instanceof File
                    && ((File) dst).getInnerArchive() != null;
            final boolean srcIsGhost = srcIsArchived
                    && srcLastModified <= 0;
            if (!srcIsGhost || !dstIsArchived || !File.isLenient())
                dst.mkdir();
            final String[] members = src.list();
            if (!srcIsArchived && dstIsArchived) {
                // Create sorted entries if writing a new archive file.
                // This is courtesy only, so natural order is sufficient.
                Arrays.sort(members);
            }
            for (int i = 0, l = members.length; i < l; i++) {
                final String member = members[i];
                ok &= mv0(  detector.createFile(src, member),
                            detector.createFile(dst,  member),
                            detector);
            }
            if (!srcIsGhost)
                ok &= dst.setLastModified(srcLastModified);
        } else if (src.isFile()) { // !isDirectory()
            try {
                cp(true, src, dst);
            } catch (IOException ex) {
                ok = false;
            }
        } else {
            ok = false; // don't move special files!
        }
        return ok && src.delete(); // only unlink if ok!
    }

    /**
     * The name of this method is inspired by the Unix command line utility
     * <code>cp</code> with the <code>-r</code> option to operate recursively.
     *
     * @see File#copyAllTo(java.io.File, ArchiveDetector, ArchiveDetector)
     * @see File#archiveCopyAllTo(java.io.File, ArchiveDetector, ArchiveDetector)
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    public static final void cp_r(
            final boolean preserve,
            final java.io.File src,
            final java.io.File dst,
            final ArchiveDetector srcDetector,
            final ArchiveDetector dstDetector)
    throws IOException {
        if (contains(src, dst))
            throw new ContainsFileException(src, dst);
        cp_r0(preserve, src, dst, srcDetector, dstDetector);
    }

    /**
     * Unchecked parameters version.
     */
    private static void cp_r0(
            final boolean preserve,
            final java.io.File src,
            final java.io.File dst,
            final ArchiveDetector srcDetector,
            final ArchiveDetector dstDetector)
    throws IOException {
        if (src.isDirectory()) {
            final long srcLastModified = src.lastModified();
            final boolean srcIsArchived = src instanceof File
                    && ((File) src).getInnerArchive() != null;
            final boolean dstIsArchived = dst instanceof File
                    && ((File) dst).getInnerArchive() != null;
            final boolean srcIsGhost = srcIsArchived
                    && srcLastModified <= 0;
            if (!srcIsGhost || !dstIsArchived || !File.isLenient())
                if (!dst.mkdir() && !dst.isDirectory())
                    throw new IOException("destination is not a directory");
            final String[] members = src.list();
            if (!srcIsArchived && dstIsArchived) {
                // Create sorted entries if writing a new archive.
                // This is a courtesy only, so natural order is sufficient.
                Arrays.sort(members);
            }
            for (int i = 0, l = members.length; i < l; i++) {
                final String member = members[i];
                cp_r0(  preserve,
                        srcDetector.createFile(src, member),
                        dstDetector.createFile(dst, member),
                        srcDetector, dstDetector);
            }
            if (preserve && !srcIsGhost)
                if (!dst.setLastModified(srcLastModified))
                    throw new IOException("cannot set last modification time");
        } else if (src.isFile() && (!dst.exists() || dst.isFile())) {
            cp0(preserve, src, dst);
        } else {
            throw new IOException("cannot copy non-existent or special files");
        }
    }

    /**
     * The name of this method is inspired by the Unix command line utility
     * <code>cp</code>.
     *
     * @see File#cp(java.io.File, java.io.File)
     * @see File#cp_p(java.io.File, java.io.File)
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    public static final void cp(
            final boolean preserve,
            final java.io.File src,
            final java.io.File dst)
    throws IOException {
        if (contains(src, dst))
            throw new ContainsFileException(src, dst);
        cp0(preserve, src, dst);
    }

    /**
     * Unchecked parameters version.
     */
    private static void cp0(
            final boolean preserve,
            final java.io.File src,
            final java.io.File dst)
    throws IOException {
        assert src != null;
        assert dst != null;

        try {
            try {
                if (src instanceof File) {
                    final File srcFile = (File) src;
                    srcFile.ensureNotVirtualRoot("cannot read");
                    final String srcEntryName = srcFile.getEnclEntryName();
                    if (srcEntryName != null) {
                        cp0(preserve,
                            srcFile.getEnclArchive().getArchiveController(),
                            srcEntryName, dst);
                        return;
                    }
                }
            } catch (RfsEntryFalsePositiveException srcIsNotArchive) {
            }

            // Treat the source like a regular file.
            final InputStream in = new java.io.FileInputStream(src);
            try {
                cp0(preserve, src, in, dst);
            } finally {
                try {
                    in.close();
                } catch (IOException ex) {
                    throw new InputIOException(ex);
                }
            }
        } catch (FileNotFoundException ex) {
            throw ex;
        } catch (ArchiveBusyException ex) {
            throw new FileBusyException(ex);
        } catch (ArchiveFileSystemException afse) {
            final FileNotFoundException fnfe
                    = new FileNotFoundException(afse.toString());
            fnfe.initCause(afse);
            throw fnfe;
        } catch (IOException ex) {
            dst.delete();
            throw ex;
        }
    }

    /**
     * Copies a source file to a destination file, optionally preserving the
     * source's last modification time.
     * We already have an input stream to read the source file,
     * but we know nothing about the destination file yet.
     * Note that this method <em>never</em> closes the given input stream!
     *
     * @throws FileNotFoundException If either the source or the destination
     *         cannot get accessed.
     * @throws InputIOException If copying the data fails because of an
     *         IOException in the source.
     * @throws IOException If copying the data fails because of an
     *         IOException in the destination.
     */
    private static void cp0(
            final boolean preserve,
            final java.io.File src,
            final InputStream in,
            final java.io.File dst)
    throws IOException {
        try {
            if (dst instanceof File) {
                final File dstFile = (File) dst;
                dstFile.ensureNotVirtualRoot("cannot write");
                final String dstEntryName = dstFile.getEnclEntryName();
                if (dstEntryName != null) {
                    cp0(preserve, src, in,
                        dstFile.getEnclArchive().getArchiveController(),
                        dstEntryName);
                    return;
                }
            }
        } catch (RfsEntryFalsePositiveException dstIsNotArchive) {
        }

        // Treat the destination like a regular file.
        final OutputStream out = new java.io.FileOutputStream(dst);
        try {
            Streams.cat(in, out);
        } finally {
            out.close();
        }
        if (preserve && !dst.setLastModified(src.lastModified()))
            throw new IOException(dst.getPath() +
                    " (cannot preserve last modification time)");
    }

    /**
     * Copies a source file to a destination file, optionally preserving the
     * source's last modification time.
     * We know that the source file appears to be an entry in an archive
     * file, but we know nothing about the destination file yet.
     * <p>
     * Note that this method synchronizes on the class object in order
     * to prevent dead locks by two threads copying archive entries to the
     * other's source archive concurrently!
     *
     * @throws FalsePositiveException If the source or the destination is a
     *         false positive and the exception
     *         cannot get resolved within this method.
     * @throws InputIOException If copying the data fails because of an
     *         IOException in the source.
     * @throws IOException If copying the data fails because of an
     *         IOException in the destination.
     */
    private static void cp0(
            final boolean preserve,
            final ArchiveController srcController,
            final String srcEntryName,
            final java.io.File dst)
    throws IOException {
        // Do not assume anything about the lock status of the controller:
        // This method may be called from a subclass while a lock is acquired!
        //assert !srcController.readLock().isLocked();
        //assert !srcController.writeLock().isLocked();

        try {
            try {
                if (dst instanceof File) {
                    final File dstFile = (File) dst;
                    dstFile.ensureNotVirtualRoot("cannot write");
                    final String dstEntryName = dstFile.getEnclEntryName();
                    if (dstEntryName != null) {
                        cp0(preserve, srcController, srcEntryName,
                            dstFile.getEnclArchive().getArchiveController(),
                            dstEntryName);
                        return;
                    }
                }
            } catch (RfsEntryFalsePositiveException isNotArchive) {
                // Both the source and/or the destination may be false positives,
                // so we need to use the exception's additional information to
                // find out which controller actually detected the false positive.
                if (isNotArchive.getController() == srcController)
                    throw isNotArchive; // not my job - pass on!
            }

            final InputStream in;
            final long time;
            srcController.readLock().lock();
            try {
                in = srcController.createInputStream0(srcEntryName); // detects false positives!
                time = srcController.lastModified(srcEntryName);
            } finally {
                srcController.readLock().unlock();
            }

            // Treat the destination like a regular file.
            final OutputStream out;
            try {
                out = new java.io.FileOutputStream(dst);
            } catch (IOException ex) {
                try {
                    in.close();
                } catch (IOException inFailure) {
                    throw new InputIOException(inFailure);
                }
                throw ex;
            }

            cp(in, out);
            if (preserve && !dst.setLastModified(time))
                throw new IOException(dst.getPath() +
                        " (cannot preserve last modification time)");
        } catch (ArchiveEntryFalsePositiveException ex) {
            assert srcController == ex.getController();
            // Reroute call to the source's enclosing archive controller.
            cp0(preserve, srcController.getEnclController(),
                srcController.enclEntryName(srcEntryName),
                dst);
        }
    }

    /**
     * Copies a source file to a destination file, optionally preserving the
     * source's last modification time.
     * We know that the source and destination files both appear to be entries
     * in an archive file.
     *
     * @throws FalsePositiveException If the source or the destination is a
     *         false positive and the exception for the destination
     *         cannot get resolved within this method.
     * @throws InputIOException If copying the data fails because of an
     *         IOException in the source.
     * @throws IOException If copying the data fails because of an
     *         IOException in the destination.
     */
    private static void cp0(
            final boolean preserve,
            final ArchiveController srcController,
            final String srcEntryName,
            final ArchiveController dstController,
            final String dstEntryName)
    throws IOException {
        // Do not assume anything about the lock status of the controller:
        // This method may be called from a subclass while a lock is acquired!
        //assert !srcController.readLock().isLocked();
        //assert !srcController.writeLock().isLocked();
        //assert !dstController.readLock().isLocked();
        //assert !dstController.writeLock().isLocked();

        try {
            class IOStreamCreator implements IORunnable {
                InputStream in;
                OutputStream out;

                public void run() throws IOException {
                    // Update controllers.
                    // This may invalidate the file system object, so it must be
                    // done first in case srcController and dstController are the
                    // same!
                    class SrcControllerUpdater implements IORunnable {
                        public void run() throws IOException {
                            srcController.autoUmount(srcEntryName);
                            srcController.readLock().lock(); // downgrade to read lock upon return
                        }
                    } // class SrcControllerUpdater

                    final ArchiveEntry srcEntry, dstEntry;
                    final Delta delta;
                    srcController.runWriteLocked(new SrcControllerUpdater());
                    try {
                        dstController.autoUmount(dstEntryName);

                        // Get source archive entry.
                        final ArchiveFileSystem srcFileSystem
                                = srcController.autoMount(false);
                        srcEntry = srcFileSystem.get(srcEntryName);

                        // Get destination archive entry.
                        final boolean lenient = File.isLenient();
                        final ArchiveFileSystem dstFileSystem
                                = dstController.autoMount(lenient);
                        delta = dstFileSystem.link(dstEntryName,
                                lenient, preserve ? srcEntry : null);
                        dstEntry = delta.getEntry();

                        // Create input stream.
                        in = srcController.createInputStream(srcEntry, dstEntry);
                    } finally {
                        srcController.readLock().unlock();
                    }

                    try {
                        // Create output stream.
                        out = dstController.createOutputStream(dstEntry, srcEntry);

                        try {
                            // Now link the destination entry into the file system.
                            delta.commit();
                        } catch (IOException ex) {
                            out.close();
                            throw ex;
                        }
                    } catch (IOException ex) {
                        try {
                            in.close();
                        } catch (IOException inFailure) {
                            throw new InputIOException(inFailure);
                        }
                        throw ex;
                    }
                }
            } // class IOStreamCreator

            final IOStreamCreator streams = new IOStreamCreator();
            synchronized (copyLock) {
                dstController.runWriteLocked(streams);
            }

            // Finally copy the entry data.
            cp(streams.in, streams.out);
        } catch (ArchiveEntryFalsePositiveException ex) {
            // Both the source and/or the destination may be false positives,
            // so we need to use the exception's additional information to
            // find out which controller actually detected the false positive.
            if (dstController != ex.getController())
                throw ex; // not my job - pass on!

            // Reroute call to the destination's enclosing archive controller.
            cp0(preserve, srcController, srcEntryName,
                dstController.getEnclController(),
                dstController.enclEntryName(dstEntryName));
        }
    }

    /**
     * Copies a source file to a destination file, optionally preserving the
     * source's last modification time.
     * We already have an input stream to read the source file and the
     * destination appears to be an entry in an archive file.
     * Note that this method <em>never</em> closes the given input stream!
     * <p>
     * Note that this method synchronizes on the class object in order
     * to prevent dead locks by two threads copying archive entries to the
     * other's source archive concurrently!
     *
     * @throws FalsePositiveException If the destination is a
     *         false positive and the exception
     *         cannot get resolved within this method.
     * @throws InputIOException If copying the data fails because of an
     *         IOException in the source.
     * @throws IOException If copying the data fails because of an
     *         IOException in the destination.
     */
    static final void cp0(
            final boolean preserve,
            final java.io.File src,
            final InputStream in,
            final ArchiveController dstController,
            final String dstEntryName)
    throws IOException {
        // Do not assume anything about the lock status of the controller:
        // This method may be called from a subclass while a lock is acquired!
        //assert !dstController.readLock().isLocked();
        //assert !dstController.writeLock().isLocked();

        try {
            class OStreamCreator implements IORunnable {
                OutputStream out; // = null;

                public void run() throws IOException {
                    // Update controller.
                    // This may invalidate the file system object, so it must be
                    // done first in case srcController and dstController are the
                    // same!
                    dstController.autoUmount(dstEntryName);

                    final boolean lenient = File.isLenient();

                    // Get source archive entry.
                    final ArchiveEntry srcEntry = new RfsEntry(src);

                    // Get destination archive entry.
                    final ArchiveFileSystem dstFileSystem
                            = dstController.autoMount(lenient);
                    final Delta delta = dstFileSystem.link(dstEntryName,
                            lenient, preserve ? srcEntry : null);
                    final ArchiveEntry dstEntry = delta.getEntry();

                    // Create output stream.
                    out = dstController.createOutputStream(dstEntry, srcEntry);

                    // Now link the destination entry into the file system.
                    delta.commit();
                }
            }

            // Create the output stream while the destination controller is
            // write locked.
            final OStreamCreator stream = new OStreamCreator();
            dstController.runWriteLocked(stream);
            final OutputStream out = stream.out;

            // Finally copy the entry data.
            try {
                Streams.cat(in, out);
            } finally {
                out.close();
            }
        } catch (ArchiveEntryFalsePositiveException ex) {
            assert dstController == ex.getController();
            // Reroute call to the destination's enclosing ArchiveController.
            cp0(preserve, src, in,
                dstController.getEnclController(),
                dstController.enclEntryName(dstEntryName));
        }
    }

    /**
     * @see File#cp(InputStream, OutputStream)
     */
    public static void cp(
            final InputStream in,
            final OutputStream out)
    throws IOException {
        try {
            try {
                Streams.cat(in, out);
            } finally {
                out.close();
            }
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                throw new InputIOException(ex);
            }
        }
    }

    /**
     * Removes the entire directory tree represented by the parameter,
     * regardless whether it's a file or directory, whether the directory
     * is empty or not or whether the file or directory is actually an
     * archive file, an entry in an archive file or not enclosed in an
     * archive file at all.
     * <p>
     * The name of this method is inspired by the Unix command line utility
     * <code>rm</code> with the <code>-r</code> option to operate recursively.
     * <p>
     * This file system operation is <em>not</em> atomic.
     *
     * @return Whether or not the entire directory tree was successfully
     *         removed.
     */
    public static boolean rm_r(final java.io.File file) {
        boolean ok = true;
        if (file.isDirectory()) {
            // Note that listing the directory this way will cause a recursive
            // deletion if the directory is actually an archive file.
            // Although this does not provide best performance (the archive
            // file could simply be removed like an ordinary file), it ensures
            // that the state cached by the ArchiveController is not bypassed
            // and hence prevents a potential bug.
            java.io.File[] members = file.listFiles();
            for (int i = members.length; --i >= 0; )
                ok &= rm_r(members[i]);
        }
        return ok && file.delete();
    }

    //
    // Static member classes and interfaces.
    //

    /**
     * A lock used when copying data from one archive to another.
     * This lock must be acquired before any other locks on the controllers
     * are acquired in order to prevent dead locks.
     */
    private static class CopyLock { }
}
