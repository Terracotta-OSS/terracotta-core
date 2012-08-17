/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ArchiveController.java
 *
 * Created on 23. Oktober 2004, 20:41
 */
/*
 * Copyright 2004-2006 Schlichtherle IT Services
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
import java.lang.ref.*;
import java.util.*;
import java.util.logging.*;

import javax.swing.Icon;

import org.terracotta.agent.repkg.de.schlichtherle.io.ArchiveFileSystem.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.archive.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.rof.*;
import org.terracotta.agent.repkg.de.schlichtherle.key.*;

/**
 * This is the base class for any archive controller, providing all the
 * essential services required by the {@link File} class to implement its
 * behaviour.
 * Each instance of this class manages a globally unique archive file
 * (the <i>target file</i>) in order to allow random access to it as if it
 * were a regular directory in the real file system.
 * <p>
 * In terms of software patterns, an <code>ArchiveController</code> is
 * similar to a Builder, with the <code>ArchiveDriver</code> interface as
 * its Abstract Factory.
 * However, an archive controller does not necessarily build a new archive.
 * It may also simply be used to access an existing archive for read-only
 * operations, such as listing its top level directory, or reading entry data.
 * Whatever type of operation it's used for, an archive controller provides
 * and controls <em>all</em> access to any particular archive file by the
 * client application and deals with the rather complex details of its
 * states and transitions.
 * <p>
 * Each instance of this class maintains a virtual file system, provides input
 * and output streams for the entries of the archive file and methods
 * to update the contents of the virtual file system to the target file
 * in the real file system.
 * In cooperation with the {@link File} class, it also knows how to deal with
 * nested archive files (such as <code>"outer.zip/inner.tar.gz"</code>
 * and <i>false positives</i>, i.e. plain files or directories or file or
 * directory entries in an enclosing archive file which have been incorrectly
 * recognized to be <i>prospective archive files</i> by the
 * {@link ArchiveDetector} interface.
 * <p>
 * To ensure that for each archive file there is at most one
 * <code>ArchiveController</code>, the path name of the archive file (called
 * <i>target</i>) is canonicalized, so it doesn't matter whether the
 * {@link File} class addresses an archive file as <code>"archive.zip"</code>
 * or <code>"/dir/archive.zip"</code> if <code>"/dir"</code> is the client
 * application's current directory.
 * <p>
 * Note that in general all of its methods are reentrant on exceptions.
 * This is important because the {@link File} class may repeatedly call them,
 * triggered by the client application. Of course, depending on the context,
 * some or all of the archive file's data may be lost in this case.
 * For more information, please refer to {@link File#umount} and
 * {@link File#update}.
 * <p>
 * This class is actually the abstract base class for any archive controller.
 * It encapsulates all the code which is not depending on a particular entry
 * synchronization strategy and the corresponding state of the controller.
 * Though currently unused, this is intended to be helpful for future
 * extensions of TrueZIP, where different synchronization strategies may be
 * implemented.
 * 
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 */
abstract class ArchiveController
        implements Archive, Entry {

    /**
     * A weak reference to this archive controller.
     * This field is for exclusive use by {@link #setScheduled(boolean)}.
     */
    private final WeakReference weakThis = new WeakReference(this);

    /**
     * the canonicalized or at least normalized absolute path name
     * representation of the target file.
     */
    private final java.io.File target;

    /**
     * The archive controller of the enclosing archive, if any.
     */
    private final ArchiveController enclController;

    /**
     * The name of the entry for this archive in the enclosing archive, if any.
     */
    private final String enclEntryName;

    /**
     * The {@link ArchiveDriver} to use for this controller's target file.
     */
    private /*volatile*/ ArchiveDriver driver;

    private final ReentrantLock  readLock;
    private final ReentrantLock writeLock;

    //
    // Constructors.
    //

    /**
     * This constructor schedules this controller to be thrown away if no
     * more <code>File</code> objects are referring to it.
     * The subclass must update this schedule according to the controller's
     * state.
     * For example, if the controller has started to update some entry data,
     * it must call {@link #setScheduled} in order to force the
     * controller to be updated on the next call to {@link #umount} even if
     * no more <code>File</code> objects are referring to it.
     * Otherwise, all changes may get lost!
     * 
     * @see #setScheduled(boolean)
     */
    ArchiveController(
            final java.io.File target,
            final ArchiveController enclController,
            final String enclEntryName,
            final ArchiveDriver driver) {
        assert target != null;
        assert target.isAbsolute();
        assert (enclController != null) == (enclEntryName != null);
        assert driver != null;

        this.target = target;
        this.enclController = enclController;
        this.enclEntryName = enclEntryName;
        this.driver = driver;

        ReadWriteLock rwl = new ReentrantReadWriteLock();
        this.readLock  = rwl.readLock();
        this.writeLock = rwl.writeLock();

        setScheduled(false);
    }

    //
    // Methods.
    //

    final ReentrantLock readLock() {
        return readLock;
    }

    final ReentrantLock writeLock() {
        return writeLock;
    }

    /**
     * Runs the given {@link IORunnable} while this controller has
     * acquired its write lock regardless of the state of its read lock.
     * You must use this method if this controller may have acquired a
     * read lock in order to prevent a dead lock.
     * <p>
     * <b>Warning:</b> This method temporarily releases the read lock
     * before the write lock is acquired and the runnable is run!
     * Hence, the runnable should recheck the state of the controller
     * before it proceeds with any write operations.
     *
     * @param runnable The {@link IORunnable} to run while the write
     *        lock is acquired.
     *        No read lock is acquired while it's running.
     */
    final void runWriteLocked(IORunnable runnable)
    throws IOException {
        // A read lock cannot get upgraded to a write lock.
        // Hence the following mess is required.
        // Note that this is not just a limitation of the current
        // implementation in JSE 5: If automatic upgrading were implemented,
        // two threads holding a read lock try to upgrade concurrently,
        // they would dead lock each other!
        final int lockCount = readLock().lockCount();
        for (int c = lockCount; c > 0; c--)
            readLock().unlock();

        // The current thread may get deactivated here!
        writeLock().lock();
        try {
            try {
                runnable.run();
            } finally {
                // Restore lock count - effectively downgrading the lock
                for (int c = lockCount; c > 0; c--)
                    readLock().lock();
            }
        } finally {
            writeLock().unlock();
        }
    }

    /**
     * Returns the canonical or at least normalized absolute
     * <code>java.io.File</code> object for the archive file to control.
     */
    final java.io.File getTarget() {
        return target;
    }

    public final String getPath() {
        return target.getPath();
    }

    /**
     * Returns <code>true</code> iff the given entry name refers to the
     * virtual root directory within this controller.
     */
    static final boolean isRoot(String entryName) {
        return ROOT_NAME == entryName; // possibly assigned by File.init(...)
    }

    /**
     * Returns the {@link ArchiveController} of the enclosing archive file,
     * if any.
     */
    final ArchiveController getEnclController() {
        return enclController;
    }

    /**
     * Returns the entry name of this controller within the enclosing archive
     * file, if any.
     */
    final String getEnclEntryName() {
        return enclEntryName;
    }

    final String enclEntryName(final String entryName) {
        return isRoot(entryName)
                ? enclEntryName
                : enclEntryName + SEPARATOR + entryName;
    }

    private final boolean isEnclosedBy(ArchiveController wannabe) {
        assert wannabe != null;
        if (enclController == wannabe)
            return true;
        if (enclController == null)
            return false;
        return enclController.isEnclosedBy(wannabe);
    }

    /**
     * Returns the driver instance which is used for the target archive.
     * All access to this method must be externally synchronized on this
     * controller's read lock!
     * 
     * @return A valid reference to an {@link ArchiveDriver} object
     *         - never <code>null</code>.
     */
    final ArchiveDriver getDriver() {
        return driver;
    }

    /**
     * Sets the driver instance which is used for the target archive.
     * All access to this method must be externally synchronized on this
     * controller's write lock!
     * 
     * @param driver A valid reference to an {@link ArchiveDriver} object
     *        - never <code>null</code>.
     */
    final void setDriver(ArchiveDriver driver) {
        // This affects all subsequent creations of the driver's products
        // (In/OutputArchive and ArchiveEntry) and hence ArchiveFileSystem.
        // Normally, these are initialized together in mountFileSystem(...)
        // which is externally synchronized on this controller's write lock,
        // so we don't need to be afraid of this.
        this.driver = driver;
    }

    /**
     * Returns <code>true</code> if and only if the target file of this
     * controller should be considered to be a file or directory in the real
     * file system (RFS).
     * Note that the target doesn't need to exist for this method to return
     * <code>true</code>.
     */
    final boolean isRfsEntryTarget() {
        // May be called from FileOutputStream while unlocked!
        //assert readLock().isLocked() || writeLock().isLocked();

        // True iff not enclosed or the enclosing archive file is actually
        // a plain directory.
        return enclController == null
                || enclController.getTarget().isDirectory();
    }

    /**
     * Returns <code>true</code> if and only if the file system has been
     * touched.
     */
    abstract boolean isTouched();

    /**
     * (Re)schedules this archive controller for the next call to
     * {@link ArchiveControllers#umount(String, boolean, boolean, boolean, boolean, boolean)}.
     * 
     * @param scheduled If set to <code>true</code>, this controller and hence
     *        its target archive file is guaranteed to get updated during the
     *        next call to <code>ArchiveControllers.umount()</code> even if
     *        there are no more {@link File} instances referring to it
     *        meanwhile.
     *        Call this method with this parameter value whenever the virtual
     *        file system has been touched, i.e. modified.
     *        <p>
     *        If set to <code>false</code>, this controller is conditionally
     *        scheduled to get updated.
     *        In this case, the controller gets automatically removed from
     *        the controllers weak hash map and discarded once the last file
     *        object directly or indirectly referring to it has been discarded
     *        unless <code>setScheduled(true)</code> has been called meanwhile.
     *        Call this method if the archive controller has been newly created
     *        or successfully updated.
     */
    final void setScheduled(final boolean scheduled) {
        assert weakThis.get() != null || !scheduled; // (garbage collected => no scheduling) == (scheduling => not garbage collected)

        ArchiveControllers.set( getTarget(),
                                scheduled ? (Object) this : weakThis);
    }

    /**
     * Tests if the archive entry with the given name has received or is
     * currently receiving new data via an output stream.
     * As an implication, the entry cannot receive new data from another
     * output stream before the next call to {@link #umount}.
     * Note that for directories this method will always return
     * <code>false</code>!
     */
    abstract boolean hasNewData(String entryName);

    /**
     * Returns the virtual archive file system mounted from the target file.
     * This method is reentrant with respect to any exceptions it may throw.
     * <p>
     * <b>Warning:</b> Either the read or the write lock of this controller
     * must be acquired while this method is called!
     * If only a read lock is acquired, but a write lock is required, this
     * method will temporarily release all locks, so any preconditions must be
     * checked again upon return to protect against concurrent modifications!
     * 
     * @param create If the archive file does not exist and this is
     *        <code>true</code>, a new file system with only a virtual root
     *        directory is created with its last modification time set to the
     *        system's current time.
     * @return A valid archive file system - <code>null</code> is never returned.
     * @throws FalsePositiveException
     * @throws IOException On any other I/O related issue with the target file
     *         or the target file of any enclosing archive file's controller.
     */
    abstract ArchiveFileSystem autoMount(boolean create)
    throws IOException;

    /**
     * Unmounts the archive file only if the archive file has already new
     * data for <code>entryName</code>.
     * <p>
     * <b>Warning:</b> As a side effect, all data structures returned by this
     * controller get reset (filesystem, entries, streams, etc.)!
     * As an implication, this method requires external synchronization on
     * this controller's write lock!
     * <p>
     * <b>TODO:</b> Consider adding configuration switch to allow overwriting
     * an archive entry to the same output archive multiple times, whereby
     * only the last written entry would be added to the central directory
     * of the archive (unless the archive type doesn't support this).
     * 
     * @see #umount(ArchiveException, boolean, boolean, boolean, boolean, boolean, boolean)
     * @see ArchiveException
     */
    final void autoUmount(final String entryName)
    throws ArchiveException {
        assert writeLock().isLocked();
        if (hasNewData(entryName))
            umount(null, true, false, true, false, false, false);
    }

    /**
     * Synchronizes the contents of the target archive file managed by this
     * archive controller to the real file system.
     * <p>
     * <b>Warning:</b> As a side effect, all data structures returned by this
     * controller get reset (filesystem, entries, streams, etc.)!
     * As an implication, this method requires external synchronization on
     * this controller's write lock!
     * 
     * @param waitInputStreams See {@link ArchiveControllers#umount}.
     * @param closeInputStreams See {@link ArchiveControllers#umount}.
     * @param waitOutputStreams See {@link ArchiveControllers#umount}.
     * @param closeOutputStreams See {@link ArchiveControllers#umount}.
     * @param umount See {@link ArchiveControllers#umount}.
     * @param reassemble Let's assume this archive file is enclosed
     *        in another archive file.
     *        Then if this parameter is <code>true</code>, the updated archive
     *        file is also written to its enclosing archive file.
     *        Note that this parameter <em>must</em> be set if <code>umount</code>
     *        is set as well. Failing to comply to this requirement may throw
     *        a {@link java.lang.AssertionError} and will incur loss of data!
     * @see #autoUmount
     * @see ArchiveException
     * @throws ArchiveException If any exception condition occurs throughout
     *         the course of this method, an {@link ArchiveException}
     *         is created, prepended to <code>exceptionChain</code> and finally
     *         thrown.
     */
    abstract void umount(
            ArchiveException exceptionChain,
            final boolean waitInputStreams,
            final boolean closeInputStreams,
            final boolean waitOutputStreams,
            final boolean closeOutputStreams,
            final boolean umount,
            final boolean reassemble)
    throws ArchiveException;

    // TODO: Document this!
    abstract int waitAllInputStreamsByOtherThreads(long timeout);

    // TODO: Document this!
    abstract int waitAllOutputStreamsByOtherThreads(long timeout);

    /**
     * Resets the archive controller to its initial state - all changes to the
     * archive file which have not yet been updated get lost!
     * Thereafter, the archive controller will behave as if it has just been
     * created and any subsequent operations on its entries will remount
     * the virtual file system from the archive file again.
     * <p>
     * This method should be overridden by subclasses, but must still be
     * called when doing so.
     */
    abstract void reset()
    throws IOException;

    public String toString() {
        return getClass().getName() + "@" + System.identityHashCode(this) + "(" + getPath() + ")";
    }

    //
    // File system operations used by the File* classes.
    // Stream operations:
    //

    /**
     * A factory method returning an input stream which is positioned
     * at the beginning of the given entry in the target archive file.
     * 
     * @param entryName An entry in the virtual archive file system
     *        - <code>null</code> or <code>""</code> is not permitted.
     * @return A valid <code>InputStream</code> object
     *         - <code>null</code> is never returned.
     * @throws FileNotFoundException If the entry cannot get read for
     *         any reason.
     */
    final InputStream createInputStream(final String entryName)
    throws FileNotFoundException {
        assert entryName != null;

        try {
            return createInputStream0(entryName);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.createInputStream(enclEntryName(entryName));
        } catch (FileNotFoundException ex) { // includes RfsEntryFalsePositiveException!
            throw ex;
        } catch (ArchiveBusyException ex) {
            throw new FileBusyException(ex);
        } catch (IOException ioe) {
            final FileNotFoundException fnfe
                    = new FileNotFoundException(ioe.toString());
            fnfe.initCause(ioe);
            throw fnfe;
        }
    }

    InputStream createInputStream0(final String entryName)
    throws IOException {
        assert entryName != null;

        readLock().lock();
        try {
            if (isRoot(entryName)) {
                try {
                    final boolean directory = isDirectory0(entryName); // detect false positives
                    assert directory : "The root entry must be a directory!";
                } catch (FalsePositiveException ex) {
                    if (!(ex.getCause() instanceof FileNotFoundException))
                        throw ex;
                }
                throw new ArchiveEntryNotFoundException(entryName,
                        "cannot read (potential) virtual root directory");
            } else {
                if (hasNewData(entryName)) {
                    runWriteLocked(new IORunnable() {
                        public void run() throws IOException {
                            autoUmount(entryName);
                        }
                    });
                }

                final ArchiveEntry entry = autoMount(false).get(entryName); // lookup file entries only!
                if (entry == null)
                    throw new ArchiveEntryNotFoundException(entryName,
                            "no such file entry");

                return createInputStream(entry, null);
            }
        } finally {
            readLock().unlock();
        }
    }

    /**
     * <b>Important:</b>
     * <ul>
     * <li>This controller's read <em>or</em> write lock must be acquired.
     * <li><code>entry</code> must not have received
     *     {@link #hasNewData new data}.
     * <ul>
     */
    abstract InputStream createInputStream(
            ArchiveEntry entry,
            ArchiveEntry dstEntry)
    throws IOException;

    /**
     * A factory method returning an <code>OutputStream</code> allowing to
     * (re)write the given entry in the target archive file.
     * 
     * @param entryName An entry in the virtual archive file system
     *        - <code>null</code> or <code>""</code> is not permitted.
     * @return A valid <code>OutputStream</code> object
     *         - <code>null</code> is never returned.
     * @throws FileNotFoundException If the entry cannot get (re)written for
     *         any reason.
     */
    final OutputStream createOutputStream(
            final String entryName,
            final boolean append)
    throws FileNotFoundException {
        assert entryName != null;

        try {
            return createOutputStream0(entryName, append);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.createOutputStream(enclEntryName(entryName),
                    append);
        } catch (FileNotFoundException ex) { // includes RfsEntryFalsePositiveException!
            throw ex;
        } catch (ArchiveBusyException ex) {
            throw new FileBusyException(ex);
        } catch (IOException ioe) {
            final FileNotFoundException fnfe
                    = new FileNotFoundException(ioe.toString());
            fnfe.initCause(ioe);
            throw fnfe;
        }
    }

    OutputStream createOutputStream0(
            final String entryName,
            final boolean append)
    throws IOException {
        assert entryName != null;

        final InputStream in;
        final OutputStream out;

        writeLock().lock();
        try {
            if (isRoot(entryName)) {
                try {
                    final boolean directory = isDirectory0(entryName); // detect false positives
                    assert directory : "The root entry must be a directory!";
                } catch (FalsePositiveException ex) {
                    if (!(ex.getCause() instanceof FileNotFoundException))
                        throw ex;
                }
                throw new ArchiveEntryNotFoundException(entryName,
                        "cannot write (potential) virtual root directory");
            } else {
                autoUmount(entryName);

                final boolean lenient = File.isLenient();
                final ArchiveFileSystem fileSystem = autoMount(lenient);

                in = append && fileSystem.isFile(entryName)
                        ? createInputStream0(entryName)
                        : null;

                // Start creating or overwriting the archive entry.
                // Note that this will fail if the entry already exists as a
                // directory.
                final Delta delta = fileSystem.link(entryName, lenient);

                // Create output stream.
                out = createOutputStream(delta.getEntry(), null);

                // Now link the entry into the file system.
                delta.commit();
            }
        } finally {
            writeLock().unlock();
        }

        if (in != null) {
            try {
                Streams.cat(in, out);
            } finally {
                in.close();
            }
        }
        return out;
    }

    /**
     * <b>Important:</b>
     * <ul>
     * <li>This controller's <em>write</em> lock must be acquired.
     * <li><code>entry</code> must not have received
     *     {@link #hasNewData new data}.
     * <ul>
     */
    abstract OutputStream createOutputStream(
            ArchiveEntry entry,
            ArchiveEntry srcEntry)
    throws IOException;

    //
    // File system operations used by the File class.
    // Read only operations:
    //

    final boolean exists(final String entryName)
    throws RfsEntryFalsePositiveException {
        try {
            return exists0(entryName);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.exists(enclEntryName(entryName));
        } catch (RfsEntryFalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private final boolean exists0(final String entryName)
    throws IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.exists(entryName);
        } finally {
            readLock().unlock();
        }
    }

    final boolean isFile(final String entryName)
    throws RfsEntryFalsePositiveException {
        try {
            return isFile0(entryName);
        } catch (FileArchiveEntryFalsePositiveException ex) {
            // TODO: Document this!
            if (isRoot(entryName)
            && ex.getCause() instanceof FileNotFoundException)
                return false;
            return enclController.isFile(enclEntryName(entryName));
        } catch (DirectoryArchiveEntryFalsePositiveException ex) {
            return enclController.isFile(enclEntryName(entryName));
        } catch (RfsEntryFalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private final boolean isFile0(final String entryName)
    throws IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.isFile(entryName);
        } finally {
            readLock().unlock();
        }
    }

    final boolean isDirectory(final String entryName)
    throws RfsEntryFalsePositiveException {
        try {
            return isDirectory0(entryName);
        } catch (FileArchiveEntryFalsePositiveException ex) {
            return false;
        } catch (DirectoryArchiveEntryFalsePositiveException ex) {
            return enclController.isDirectory(enclEntryName(entryName));
        } catch (RfsEntryFalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private final boolean isDirectory0(final String entryName)
    throws IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.isDirectory(entryName);
        } finally {
            readLock().unlock();
        }
    }

    final Icon getOpenIcon(final String entryName)
    throws RfsEntryFalsePositiveException {
        try {
            return getOpenIcon0(entryName);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.getOpenIcon(enclEntryName(entryName));
        } catch (RfsEntryFalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
    }

    private final Icon getOpenIcon0(final String entryName)
    throws IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false); // detect false positives!
            return isRoot(entryName)
                    ? getDriver().getOpenIcon(this)
                    : fileSystem.getOpenIcon(entryName);
        } finally {
            readLock().unlock();
        }
    }

    final Icon getClosedIcon(final String entryName)
    throws RfsEntryFalsePositiveException {
        try {
            return getClosedIcon0(entryName);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.getOpenIcon(enclEntryName(entryName));
        } catch (RfsEntryFalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
    }

    private final Icon getClosedIcon0(final String entryName)
    throws IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false); // detect false positives!
            return isRoot(entryName)
                    ? getDriver().getClosedIcon(this)
                    : fileSystem.getClosedIcon(entryName);
        } finally {
            readLock().unlock();
        }
    }

    final boolean canRead(final String entryName)
    throws RfsEntryFalsePositiveException {
        try {
            return canRead0(entryName);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.canRead(enclEntryName(entryName));
        } catch (RfsEntryFalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private final boolean canRead0(final String entryName)
    throws IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.exists(entryName);
        } finally {
            readLock().unlock();
        }
    }

    final boolean canWrite(final String entryName)
    throws RfsEntryFalsePositiveException {
        try {
            return canWrite0(entryName);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.canWrite(enclEntryName(entryName));
        } catch (RfsEntryFalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private final boolean canWrite0(final String entryName)
    throws IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.canWrite(entryName);
        } finally {
            readLock().unlock();
        }
    }

    final long length(final String entryName)
    throws RfsEntryFalsePositiveException {
        try {
            return length0(entryName);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.length(enclEntryName(entryName));
        } catch (RfsEntryFalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return 0;
        }
    }

    private final long length0(final String entryName)
    throws IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.length(entryName);
        } finally {
            readLock().unlock();
        }
    }

    final long lastModified(final String entryName)
    throws RfsEntryFalsePositiveException {
        try {
            return lastModified0(entryName);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.lastModified(enclEntryName(entryName));
        } catch (RfsEntryFalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return 0;
        }
    }

    private final long lastModified0(final String entryName)
    throws IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.lastModified(entryName);
        } finally {
            readLock().unlock();
        }
    }

    final String[] list(final String entryName)
    throws RfsEntryFalsePositiveException {
        try {
            return list0(entryName);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.list(enclEntryName(entryName));
        } catch (RfsEntryFalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
    }

    private final String[] list0(final String entryName)
    throws IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.list(entryName);
        } finally {
            readLock().unlock();
        }
    }

    final String[] list(
            final String entryName,
            final FilenameFilter filenameFilter,
            final File dir)
    throws RfsEntryFalsePositiveException {
        try {
            return list0(entryName, filenameFilter, dir);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.list(enclEntryName(entryName),
                    filenameFilter, dir);
        } catch (RfsEntryFalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
    }

    private final String[] list0(
            final String entryName,
            final FilenameFilter filenameFilter,
            final File dir)
    throws IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.list(entryName, filenameFilter, dir);
        } finally {
            readLock().unlock();
        }
    }

    final File[] listFiles(
            final String entryName,
            final FilenameFilter filenameFilter,
            final File dir,
            final FileFactory factory)
    throws RfsEntryFalsePositiveException {
        try {
            return listFiles0(entryName, filenameFilter, dir, factory);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.listFiles(enclEntryName(entryName),
                    filenameFilter, dir, factory);
        } catch (RfsEntryFalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
    }

    private final File[] listFiles0(
            final String entryName,
            final FilenameFilter filenameFilter,
            final File dir,
            final FileFactory factory)
    throws IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.listFiles(entryName, filenameFilter, dir, factory);
        } finally {
            readLock().unlock();
        }
    }

    final File[] listFiles(
            final String entryName,
            final FileFilter fileFilter,
            final File dir,
            final FileFactory factory)
    throws RfsEntryFalsePositiveException {
        try {
            return listFiles0(entryName, fileFilter, dir, factory);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.listFiles(enclEntryName(entryName),
                    fileFilter, dir, factory);
        } catch (RfsEntryFalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
    }

    private final File[] listFiles0(
            final String entryName,
            final FileFilter fileFilter,
            final File dir,
            final FileFactory factory)
    throws IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.listFiles(entryName, fileFilter, dir, factory);
        } finally {
            readLock().unlock();
        }
    }

    //
    // File system operations used by the File class.
    // Write operations:
    //

    final boolean setReadOnly(final String entryName)
    throws RfsEntryFalsePositiveException {
        try {
            return setReadOnly0(entryName);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.setReadOnly(enclEntryName(entryName));
        } catch (RfsEntryFalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private final boolean setReadOnly0(final String entryName)
    throws IOException {
        writeLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.setReadOnly(entryName);
        } finally {
            writeLock().unlock();
        }
    }

    final boolean setLastModified(
            final String entryName,
            final long time)
    throws RfsEntryFalsePositiveException {
        try {
            return setLastModified0(entryName, time);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.setLastModified(enclEntryName(entryName),
                    time);
        } catch (RfsEntryFalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private final boolean setLastModified0(
            final String entryName,
            final long time)
    throws IOException {
        writeLock().lock();
        try {
            autoUmount(entryName);
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.setLastModified(entryName, time);
        } finally {
            writeLock().unlock();
        }
    }

    final boolean createNewFile(
            final String entryName,
            final boolean autoCreate)
    throws IOException {
        try {
            return createNewFile0(entryName, autoCreate);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.createNewFile(enclEntryName(entryName),
                    autoCreate);
        }
    }

    private final boolean createNewFile0(
            final String entryName,
            final boolean autoCreate)
    throws IOException {
        assert !isRoot(entryName);

        writeLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(autoCreate);
            if (fileSystem.exists(entryName))
                return false;

            // If we got until here without an exception,
            // write an empty file now.
            createOutputStream0(entryName, false).close();

            return true;
        } finally {
            writeLock().unlock();
        }
    }

    final boolean mkdir(
            final String entryName,
            final boolean autoCreate)
    throws RfsEntryFalsePositiveException {
        try {
            mkdir0(entryName, autoCreate);
            return true;
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.mkdir(enclEntryName(entryName), autoCreate);
        } catch (RfsEntryFalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private final void mkdir0(final String entryName, final boolean autoCreate)
    throws IOException {
        writeLock().lock();
        try {
            if (isRoot(entryName)) {
                // This is the virtual root of an archive file system, so we
                // are actually working on the controller's target file.
                if (isRfsEntryTarget()) {
                    if (target.exists())
                        throw new IOException("target file exists already!");
                } else {
                    if (enclController.exists(enclEntryName))
                        throw new IOException("target file exists already!");
                }
                // Ensure file system existence.
                autoMount(true);
            } else { // !isRoot(entryName)
                // This file is a regular archive entry.
                final ArchiveFileSystem fileSystem = autoMount(autoCreate);
                fileSystem.mkdir(entryName, autoCreate);
            }
        } finally {
            writeLock().unlock();
        }
    }

    final boolean delete(final String entryName)
    throws RfsEntryFalsePositiveException {
        try {
            delete0(entryName);
            return true;
        } catch (DirectoryArchiveEntryFalsePositiveException ex) {
            return enclController.delete(enclEntryName(entryName));
        } catch (FileArchiveEntryFalsePositiveException ex) {
            // TODO: Document this!
            if (isRoot(entryName)
            && !enclController.isDirectory(enclEntryName(entryName))
            && ex.getCause() instanceof FileNotFoundException)
                return false;
            return enclController.delete(enclEntryName(entryName));
        } catch (RfsEntryFalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private final void delete0(final String entryName)
    throws IOException {
        writeLock().lock();
        try {
            autoUmount(entryName);

            if (isRoot(entryName)) {
                // Get the file system or die trying!
                final ArchiveFileSystem fileSystem;
                try {
                    fileSystem = autoMount(false);
                } catch (FalsePositiveException ex) {
                    // The File instance is going to delete the target file
                    // anyway, so we need to reset now.
                    try {
                        reset();
                    } catch (IOException cannotHappen) {
                        throw new AssertionError(cannotHappen);
                    }
                    throw ex;
                }

                // We are actually working on the controller's target file.
                // Do not use the number of entries in the file system
                // for the following test - it's size would count absolute
                // pathnames as well!
                final String[] members = fileSystem.list(entryName);
                if (members != null && members.length != 0)
                    throw new IOException("archive file system not empty!");
                final int outputStreams = waitAllOutputStreamsByOtherThreads(50);
                // TODO: Review: This policy may be changed - see method start.
                assert outputStreams <= 0
                        : "Entries for open output streams should not be deletable!";
                // Note: Entry for open input streams ARE deletable!
                final int inputStreams = waitAllInputStreamsByOtherThreads(50);
                if (inputStreams > 0 || outputStreams > 0)
                    throw new IOException("archive file has open streams!");
                reset();
                // Just in case our target is an RAES encrypted ZIP file,
                // forget it's password as well.
                // TODO: Review: This is an archive driver dependency!
                // Calling it doesn't harm, but please consider a more opaque
                // way to model this.
                PromptingKeyManager.resetKeyProvider(getPath());
                // Delete the target file or the entry in the enclosing
                // archive file, too.
                if (isRfsEntryTarget()) {
                    // The target file of the controller is NOT enclosed
                    // in another archive file.
                    if (!target.delete())
                        throw new IOException("couldn't delete archive file!");
                } else {
                    // The target file of the controller IS enclosed in
                    // another archive file.
                    enclController.delete0(enclEntryName(entryName));
                }
            } else { // !isRoot(entryName)
                final ArchiveFileSystem fileSystem = autoMount(false);
                fileSystem.delete(entryName);
            }
        } finally {
            writeLock().unlock();
        }
    }

    //
    // Exception classes.
    // Note that these are all inner classes, not just static member classes.
    //

    /**
     * Thrown if a controller's target file is a false positive archive file
     * which actually exists as a plain file or directory in the real file
     * system or in an enclosing archive file.
     * <p>
     * Instances of this class are always associated with an
     * <code>IOException</code> as their cause.
     */
    abstract class FalsePositiveException extends FileNotFoundException {
        private final boolean cacheable;

        /**
         * Creates a new <code>FalsePositiveException</code>.
         * 
         * @param cause The cause for this exception.
         *        If this is an instance of {@link TransientIOException},
         *        then its transient cause is unwrapped and used as the cause
         *        of this exception instead and
         *        {@link FalsePositiveException#isCacheable} is set to return
         *        <code>false</code>.
         */
        private FalsePositiveException(IOException cause) {
            // This exception type is never passed to the client application,
            // so a descriptive message would be waste of performance.
            //super(cause.toString());
            assert cause != null;
            // A transient I/O exception is just a wrapper exception to mark
            // the real transient cause, therefore we can safely throw it away.
            // We must do this in order to allow the File class to inspect
            // the real transient cause and act accordingly.
            final boolean trans = cause instanceof TransientIOException;
            super.initCause(trans ? cause.getCause() : cause);
            cacheable = !trans;
        }

        /**
         * Returns the archive controller which has thrown this exception.
         * This is the controller which detected the false positive archive
         * file.
         */
        ArchiveController getController() {
            return ArchiveController.this;
        }

        /**
         * Returns <code>true</code> if and only if there is no cause
         * associated with this exception or it is safe to cache it.
         */
        boolean isCacheable() {
            return cacheable;
        }
    } // class FalsePositiveException

    /**
     * Thrown if a controller's target file is a false positive archive file
     * which actually exists as a plain file or directory in the real file
     * system.
     * <p>
     * Instances of this class are always associated with an
     * <code>IOException</code> as their cause.
     */
    final class RfsEntryFalsePositiveException extends FalsePositiveException {

        /**
         * Creates a new <code>RfsEntryFalsePositiveException</code>.
         *
         * @param cause The cause for this exception.
         *        If this is an instance of {@link TransientIOException},
         *        then its transient cause is unwrapped and used as the cause
         *        of this exception instead and
         *        {@link FalsePositiveException#isCacheable} is set to return
         *        <code>false</code>.
         */
        RfsEntryFalsePositiveException(IOException cause) {
            super(cause);
        }
    } // class RfsEntryFalsePositiveException

    /**
     * Thrown if a controller's target file is a false positive archive file
     * which actually exists as a plain file or directory in an enclosing
     * archive file.
     * <p>
     * Instances of this class are always associated with an
     * <code>IOException</code> as their cause.
     */
    abstract class ArchiveEntryFalsePositiveException extends FalsePositiveException {
        private final ArchiveController enclController;
        private final String enclEntryName;

        /**
         * Creates a new <code>ArchiveEntryFalsePositiveException</code>.
         * 
         * @param enclController The controller in which the archive file
         *        exists as a false positive.
         *        This must be an enclosing controller.
         * @param enclEntryName The entry name which is a false positive
         *        archive file.
         *        <code>null</code> is not permitted.
         * @param cause The cause for this exception.
         *        If this is an instance of {@link TransientIOException},
         *        then its transient cause is unwrapped and used as the cause
         *        of this exception instead and
         *        {@link FalsePositiveException#isCacheable} is set to return
         *        <code>false</code>.
         */
        private ArchiveEntryFalsePositiveException(
                ArchiveController enclController,
                String enclEntryName,
                IOException cause) {
            super(cause);
            assert enclController != ArchiveController.this;
            assert isEnclosedBy(enclController);
            assert enclEntryName != null;
            this.enclController = enclController;
            this.enclEntryName = enclEntryName;
        }

        /**
         * Returns the controller which's target file contains the
         * false positive archive file as an archive entry.
         * Never <code>null</code>.
         * <p>
         * Note that this is not the same
         */
        ArchiveController getEnclController() {
            return enclController;
        }

        /**
         * Returns the entry name of the false positive archive file.
         * Never <code>null</code>.
         */
        String getEnclEntryName() {
            return enclEntryName;
        }
    } // class ArchiveEntryFalsePositiveException

    /**
     * Thrown if a controller's target file is a false positive archive file
     * which actually exists as a plain file in an enclosing archive file.
     * <p>
     * Instances of this class are always associated with an
     * <code>IOException</code> as their cause.
     */
    final class FileArchiveEntryFalsePositiveException
            extends ArchiveEntryFalsePositiveException {

        /**
         * Creates a new <code>FileArchiveEntryFalsePositiveException</code>.
         *
         * @param enclController The controller in which the archive file
         *        exists as a false positive.
         *        This must be an enclosing controller.
         * @param enclEntryName The entry name which is a false positive
         *        archive file.
         *        <code>null</code> is not permitted.
         * @param cause The cause for this exception.
         *        If this is an instance of {@link TransientIOException},
         *        then its transient cause is unwrapped and used as the cause
         *        of this exception instead and
         *        {@link FalsePositiveException#isCacheable} is set to return
         *        <code>false</code>.
         */
        FileArchiveEntryFalsePositiveException(
                ArchiveController enclController,
                String enclEntryName,
                IOException cause) {
            super(enclController, enclEntryName, cause);
        }
    } // class FileArchiveEntryFalsePositiveException

    /**
     * Thrown if a controller's target file is a false positive archive file
     * which actually exists as a plain directory in an enclosing archive file.
     * <p>
     * Instances of this class are always associated with an
     * <code>IOException</code> as their cause.
     */
    final class DirectoryArchiveEntryFalsePositiveException
            extends ArchiveEntryFalsePositiveException {

        /**
         * Creates a new <code>DirectoryArchiveEntryFalsePositiveException</code>.
         *
         * @param enclController The controller in which the archive file
         *        exists as a false positive.
         *        This must be an enclosing controller.
         * @param enclEntryName The entry name which is a false positive
         *        archive file.
         *        <code>null</code> is not permitted.
         * @param cause The cause for this exception.
         *        If this is an instance of {@link TransientIOException},
         *        then its transient cause is unwrapped and used as the cause
         *        of this exception instead and
         *        {@link FalsePositiveException#isCacheable} is set to return
         *        <code>false</code>.
         */
        DirectoryArchiveEntryFalsePositiveException(
                ArchiveController enclController,
                String enclEntryName,
                IOException cause) {
            super(enclController, enclEntryName, cause);
        }
    } // class DirectoryArchiveEntryFalsePositiveException

    /**
     * Thrown if a controller's target file does not exist or is not
     * accessible.
     * May be thrown by {@link #autoMount(boolean)} if automatic creation of
     * the target file is not allowed.
     */
    final class ArchiveFileNotFoundException extends FileNotFoundException {

        ArchiveFileNotFoundException(String msg) {
            super(msg);
        }

        public String getMessage() {
            String msg = super.getMessage();
            if (msg != null)
                return getPath() + " (" + msg + ")";
            else
                return getPath();
        }
    } // class ArchiveFileNotFoundException

    /**
     * Thrown if an archive entry does not exist
     * or is not accessible.
     * May be thrown by {@link #createInputStream} or
     * {@link #createOutputStream}.
     */
    final class ArchiveEntryNotFoundException extends FileNotFoundException {
        private final String entryName;

        ArchiveEntryNotFoundException(final String entryName, final String msg) {
            super(msg);
            assert entryName != null;
            assert msg != null;
            this.entryName = entryName;
        }

        public String getMessage() {
            String path = getPath();
            if (!isRoot(entryName))
                path += File.separator
                     + entryName.replace(SEPARATOR_CHAR, File.separatorChar);
            String msg = super.getMessage();
            if (msg != null)
                path += " (" + msg + ")";
            return path;
        }
    } // class ArchiveEntryNotFoundException
}
