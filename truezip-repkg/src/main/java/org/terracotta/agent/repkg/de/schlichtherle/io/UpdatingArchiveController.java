/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * UpdatingArchiveController.java
 *
 * Created on 28. Maerz 2006, 17:40
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
import java.util.*;
import java.util.logging.*;

import org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.rof.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.util.*;

/**
 * This archive controller implements the mounting/unmounting strategy
 * by performing a full update of the target archive file.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 */
final class UpdatingArchiveController extends ArchiveFileSystemController {

    //
    // Static fields.
    //

    private static final String CLASS_NAME
            = "de/schlichtherle/io/UpdatingArchiveController".replace('/', '.'); // support code obfuscation!
    private static final Logger logger = Logger.getLogger(CLASS_NAME, CLASS_NAME);

    /** Prefix for temporary files created by this class. */
    static final String TEMP_FILE_PREFIX = "tzp-ctrl";

    /**
     * Suffix for temporary files created by this class
     * - should <em>not</em> be <code>null</code> for enhanced unit tests.
     */
    static final String TEMP_FILE_SUFFIX = ".tmp";

    //
    // Instance fields.
    //

    /**
     * The actual archive file as a plain <code>java.io.File</code> object
     * which serves as the input file for the virtual file system managed
     * by this {@link ArchiveController} object.
     * Note that this will be set to a tempory file if the archive file is
     * enclosed within another archive file.
     */
    private java.io.File inFile;

    /**
     * An {@link InputArchive} object used to mount the virtual file system
     * and read the entries from the archive file.
     */
    private InputArchive inArchive;

    /**
     * Plain <code>java.io.File</code> object used for temporary output.
     * Maybe identical to <code>inFile</code>.
     */
    private java.io.File outFile;

    /**
     * The (possibly temporary) {@link OutputArchive} we are writing newly
     * created or modified entries to.
     */
    private OutputArchive outArchive;

    /**
     * Whether or not nesting this archive file to its enclosing
     * archive file has been deferred.
     */
    private boolean needsReassembly;

    //
    // Constructors.
    //

    UpdatingArchiveController(
            java.io.File target,
            ArchiveController enclController,
            String enclEntryName,
            ArchiveDriver driver) {
        super(target, enclController, enclEntryName, driver);
    }

    //
    // Methods.
    //

    void mount(final boolean autoCreate)
    throws IOException {
        assert writeLock().isLocked();
        assert inArchive == null;
        assert outFile == null;
        assert outArchive == null;
        assert getFileSystem() == null;

        // Do the logging part and leave the work to mount0.
        logger.log(Level.FINER, "mount.entering", // NOI18N
                new Object[] {
                    getPath(),
                    Boolean.valueOf(autoCreate),
        });
        try {
            mount0(autoCreate);
        } catch (IOException ex) {
            assert writeLock().isLocked();
            assert inArchive == null;
            assert outFile == null;
            assert outArchive == null;
            assert getFileSystem() == null;

            // Log at FINER level. This is mostly because of false positives.
            logger.log(Level.FINER, "mount.throwing", ex); // NOI18N
            throw ex;
        }
        logger.log(Level.FINER, "mount.exiting"); // NOI18N

        assert writeLock().isLocked();
        assert autoCreate || inArchive != null;
        assert autoCreate || outFile == null;
        assert autoCreate || outArchive == null;
        assert getFileSystem() != null;
    }

    private void mount0(final boolean autoCreate)
    throws IOException {
        // We need to mount the virtual file system from the input file.
        // and so far we have not successfully opened the input file.
        if (isRfsEntryTarget()) {
            // The target file of this controller is NOT enclosed
            // in another archive file.
            // Test modification time BEFORE opening the input file!
            if (inFile == null)
                inFile = getTarget();
            final long time = inFile.lastModified();
            if (time != 0) {
                // The archive file exists.
                // Thoroughly test read-only status BEFORE opening
                // the device file!
                final boolean isReadOnly = !Files.isWritableOrCreatable(inFile);
                try {
                    initInArchive(inFile);
                } catch (IOException ex) {
                    // Wrap cause so that a matching catch block can assume
                    // that it can access the target in the real file system.
                    throw new RfsEntryFalsePositiveException(ex);
                }
                setFileSystem(new ArchiveFileSystem(
                        this, inArchive, time, isReadOnly));
            } else if (!autoCreate) {
                // The archive file does not exist and we may not create it
                // automatically.
                throw new ArchiveFileNotFoundException("may not create");
            } else {
                // The archive file does NOT exist, but we may create
                // it automatically.
                // Setup output first to implement fail-fast behavior.
                // This may fail e.g. if the target file is a RAES
                // encrypted ZIP file and the user cancels password
                // prompting.
                ensureOutArchive(); // required!
                setFileSystem(new ArchiveFileSystem(this));
            }
        } else {
            // The target file of this controller IS (or appears to be)
            // enclosed in another archive file.
            if (inFile == null) {
                unwrap(getEnclController(), getEnclEntryName(), autoCreate);
            } else {
                // The enclosed archive file has already been updated and the
                // file previously used for output has been left over to be
                // reused as our input in order to skip the lengthy process
                // of searching for the right enclosing archive controller
                // to extract the entry which is our target.
                try {
                    initInArchive(inFile);
                } catch (IOException ex) {
                    // This is very unlikely unless someone has tampered with
                    // the temporary file or this controller is managing an
                    // RAES encrypted ZIP file and the client application has
                    // inadvertently called KeyManager.resetKeyProviders() or
                    // similar and the subsequent repetitious prompting for
                    // the key has unfortunately been cancelled by the user.
                    // Now the next problem is that we cannot always generate
                    // a false positive exception with the correct enclosing
                    // controller because we haven't searched for it.
                    // Anyway, this is so unlikely that we simply throw a
                    // false positive exception and cross fingers that the
                    // controller and entry name information will not be used.
                    // When assertions are enabled, we prefer to treat this as
                    // a bug.
                    assert false : "We should never get here! Read the source code comments for full details.";
                    throw new FileArchiveEntryFalsePositiveException(
                            getEnclController(), // probably not correct!
                            getEnclEntryName(), // dito
                            ex);
                }
                // Note that the archive file system must be read-write
                // because we are reusing a file which has been previously
                // used to output modifications to it!
                // Similarly, the last modification time of the left over
                // output file has been set to the last modification time of
                // its virtual root directory.
                // Nice trick, isn't it?!
                setFileSystem(new ArchiveFileSystem(
                        this, inArchive, inFile.lastModified(), false));
            }
        }
    }

    private void unwrap(
            final ArchiveController controller,
            final String entryName,
            final boolean autoCreate)
    throws IOException {
        assert controller != null;
        //assert !controller.readLock().isLocked();
        //assert !controller.writeLock().isLocked();
        assert entryName != null;
        assert !ROOT_NAME.equals(entryName);
        assert inFile == null;

        try {
            // We want to allow as much concurrency as possible, so we will
            // write lock the controller only if we need to update it first
            // or the controller's target shall be automatically created.
            final ReentrantLock lock = autoCreate
                    ? controller.writeLock()
                    : controller.readLock();
            controller.readLock().lock();
            if (controller.hasNewData(entryName) || autoCreate) {
                controller.readLock().unlock();
                class Locker implements IORunnable {
                    public void run() throws IOException {
                        // Update controller if the entry already has new data.
                        // This needs to be done first before we can access the
                        // file system since controller.createInputStream(entryName)
                        // would do the same and controller.update() would
                        // invalidate the file system reference.
                        controller.autoUmount(entryName);

                        // Keep a lock for the actual unwrapping.
                        // If this is an ordinary mounting procedure where the
                        // file system shall not be created automatically, then
                        // we MUST NOT hold a write lock while unwrapping and
                        // mounting the file system.
                        // This is to prevent dead locks when using RAES
                        // encrypted ZIP files with JFileChooser where the user
                        // may be prompted for a password by the EDT while one
                        // of JFileChooser's background file loading threads is
                        // holding a read lock for the same controller and
                        // waiting for the EDT to be accessible in order to
                        // prompt the user for the same controller's target file,
                        // too.
                        lock.lock(); // keep lock upon return
                    }
                } // class Locker
                controller.runWriteLocked(new Locker());
            }
            try {
                unwrapFromLockedController(controller, entryName, autoCreate);
            } finally {
                lock.unlock();
            }
        } catch (DirectoryArchiveEntryFalsePositiveException ex) {
            // We could as well have catched this exception in the inner
            // try-catch block where we access the controller's file system,
            // but then we would still hold the lock on controller, which
            // is not necessary while accessing the file system of its
            // enclosing controller.
            if (ex.getEnclController() == controller)
                throw ex; // just created - pass on

            unwrap( controller.getEnclController(),
                    controller.enclEntryName(entryName),
                    autoCreate);
        }
    }

    private void unwrapFromLockedController(
            final ArchiveController controller,
            final String entryName,
            final boolean autoCreate)
    throws IOException {
        assert controller != null;
        assert controller.readLock().isLocked() || controller.writeLock().isLocked();
        assert entryName != null;
        assert !ROOT_NAME.equals(entryName);
        assert inFile == null;

        final ArchiveFileSystem controllerFileSystem;
        try {
            controllerFileSystem = controller.autoMount(
                    autoCreate && File.isLenient());
        } catch (RfsEntryFalsePositiveException ex) {
            assert false : "FIXME: Explain or remove this!";
            // Unwrap cause so that we don't catch recursively here and
            // disable any other matching catch blocks for ex.
            throw (IOException) ex.getCause();
        }
        if (controllerFileSystem.isFile(entryName)) {
            // This archive file DOES exist in the enclosing archive.
            // The input file is only temporarily used for the
            // archive file entry.
            final java.io.File tmp = Temps.createTempFile(
                    TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
            // We do properly delete our temps, so this is not required.
            // In addition, this would be dangerous as the deletion
            // could happen before our shutdown hook has a chance to
            // process this controller!!!
            //tmp.deleteOnExit();
            try {
                // Now extract the entry to the temporary file.
                File.cp(controller.createInputStream0(entryName),
                        new java.io.FileOutputStream(tmp));
                // Don't keep tmp if this fails: our caller couldn't reproduce
                // the proper exception on a second try!
                try {
                    initInArchive(tmp);
                } catch (IOException ex) {
                    throw new FileArchiveEntryFalsePositiveException(
                            controller, entryName, ex);
                }
                setFileSystem(new ArchiveFileSystem(this, inArchive,
                        controllerFileSystem.lastModified(entryName),
                        controllerFileSystem.isReadOnly()));
                inFile = tmp; // init on success only!
            } catch (Throwable ex) {
                // ex could be a NoClassDefFoundError if
                // target is an RAES encrypted ZIP file and
                // Bouncycastle's Lightweight Crypto API is not
                // in the classpath.
                // We are just catching all kinds of Throwables
                // to make sure that we always delete the newly
                // created temp file.
                // Finally, we pass on the catched exception.
                if (!tmp.delete()) {
                    // This should normally never happen...
                    final IOException ioe = new IOException(
                            tmp.getPath()
                            + " (couldn't delete corrupted input file)");
                    ioe.initCause(ex);
                    throw ioe;
                }
                if (ex instanceof IOException)
                    throw (IOException) ex;
                else if (ex instanceof RuntimeException)
                    throw (RuntimeException) ex;
                else
                    throw (Error) ex; // must be Error, throws ClassCastException otherwise!
            }
        } else if (controllerFileSystem.isDirectory(entryName)) {
            throw new DirectoryArchiveEntryFalsePositiveException(
                    controller, entryName,
                    new FileNotFoundException("cannot read directories"));
        } else if (!autoCreate) {
            // The entry does NOT exist in the enclosing archive
            // file and we may not create it automatically.
            throw new ArchiveFileNotFoundException("may not create");
        } else {
            assert autoCreate;
            assert controller.writeLock().isLocked();

            // The entry does NOT exist in the enclosing archive
            // file, but we may create it automatically.
            // TODO: Document this: Why do we need to pass File.isLenient()
            // instead of just true?
            final ArchiveFileSystem.Delta delta
                    = controllerFileSystem.link(
                        entryName, File.isLenient());

            // This may fail if e.g. the target file is an RAES
            // encrypted ZIP file and the user cancels password
            // prompting.
            ensureOutArchive();

            // Now try to create the entry in the enclosing controller.
            try {
                delta.commit();
            } catch (IOException ex) {
                // The delta on the *enclosing* controller failed.
                // Hence, we need to revert our state changes.
                try {
                    try {
                        outArchive.close();
                    } finally {
                        outArchive = null;
                    }
                } finally {
                    boolean deleted = outFile.delete();
                    assert deleted;
                    outFile = null;
                }

                throw ex;
            }

            setFileSystem(new ArchiveFileSystem(this));
        }
    }

    /**
     * Initializes <code>inArchive</code> with a newly created
     * {@link InputArchive} for reading <code>inFile</code>.
     *
     * @throws IOException On any I/O related issue with <code>inFile</code>.
     */
    private void initInArchive(final java.io.File inFile)
    throws IOException {
        assert writeLock().isLocked();
        assert inArchive == null;

        logger.log(Level.FINEST, "initInArchive.entering", inFile); // NOI18N
        try {
            ReadOnlyFile rof = new SimpleReadOnlyFile(inFile);
            if (isRfsEntryTarget())
                rof = new CountingReadOnlyFile(rof);
            try {
                inArchive = getDriver().createInputArchive(this, rof);
            } catch (Throwable ex) {
                // ex could be a NoClassDefFoundError if target is an RAES
                // encrypted ZIP file and Bouncycastle's Lightweight
                // Crypto API is not in the classpath.
                // We are just catching all kinds of Throwables to make sure
                // that we close the read only file.
                // Finally, we will pass on the catched exception.
                rof.close();
                if (ex instanceof IOException)
                    throw (IOException) ex;
                else if (ex instanceof RuntimeException)
                    throw (RuntimeException) ex;
                else if (ex instanceof Error)
                    throw (Error) ex;
                else
                    throw new AssertionError(ex); // cannot happen!
            }
            inArchive.setMetaData(new InputArchiveMetaData(this, inArchive));
        } catch (IOException ex) {
            assert inArchive == null;
            logger.log(Level.FINEST, "initInArchive.throwing", ex); // NOI18N
            throw ex;
        }
        logger.log(Level.FINEST, "initInArchive.exiting", // NOI18N
                new Integer(inArchive.getNumArchiveEntries()));

        assert inArchive != null;
    }

    InputStream createInputStream(
            final ArchiveEntry entry,
            final ArchiveEntry dstEntry)
    throws IOException {
        assert entry != null;
        assert readLock().isLocked() || writeLock().isLocked();
        assert !hasNewData(entry.getName());
        assert !entry.isDirectory();

        final InputStream in
                = inArchive.getMetaData().createInputStream(entry, dstEntry);
        if (in == null)
            throw new ArchiveEntryNotFoundException(entry.getName(),
                    "bad archive driver: returned illegal null value");
        return in;
    }

    OutputStream createOutputStream(
            final ArchiveEntry entry,
            final ArchiveEntry srcEntry)
    throws IOException {
        assert entry != null;
        assert writeLock().isLocked();
        assert !hasNewData(entry.getName());
        assert !entry.isDirectory();

        ensureOutArchive();
        final OutputStream out
                = outArchive.getMetaData().createOutputStream(entry, srcEntry);
        if (out == null)
            throw new ArchiveEntryNotFoundException(entry.getName(),
                    "bad archive driver: returned illegal null value");
        return out;
    }

    void touch() throws IOException {
        assert writeLock().isLocked();
        ensureOutArchive();
        super.touch();
    }

    private void ensureOutArchive()
    throws IOException {
        assert writeLock().isLocked();

        if (outArchive != null)
            return;

        java.io.File tmp = outFile;
        if (tmp == null) {
            if (isRfsEntryTarget() && !getTarget().isFile()) {
                tmp = getTarget();
            } else {
                // Use a new temporary file as the output archive file.
                tmp = Temps.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
                // We do properly delete our temps, so this is not required.
                // In addition, this would be dangerous as the deletion
                // could happen before our shutdown hook has a chance to
                // process this controller!!!
                //tmp.deleteOnExit();
            }
        }

        try {
            initOutArchive(tmp);
        } catch (TransientIOException ex) {
            // Currently we do not have any use for this wrapper exception
            // when creating output archives, so we unwrap the transient
            // cause here.
            throw ex.getTransientCause();
        }
        outFile = tmp; // init outFile on success only!
    }

    /**
     * Initializes <code>outArchive</code> with a newly created
     * {@link OutputArchive} for writing <code>outFile</code>.
     * This method will delete <code>outFile</code> if it has successfully
     * opened it for overwriting, but failed to write the archive file header.
     *
     * @throws IOException On any I/O related issue with <code>outFile</code>.
     */
    private void initOutArchive(final java.io.File outFile)
    throws IOException {
        assert writeLock().isLocked();
        assert outArchive == null;

        logger.log(Level.FINEST, "initOutArchive.entering", outFile); // NOI18N
        try {
            OutputStream out = new java.io.FileOutputStream(outFile);
            // If we are actually writing to the target file,
            // we want to log the byte count.
            if (outFile == getTarget())
                out = new CountingOutputStream(out);
            try {
                outArchive = getDriver().createOutputArchive(this, out, inArchive);
            } catch (Throwable ex) {
                // ex could be a NoClassDefFoundError if target is an RAES
                // encrypted archive file and Bouncycastle's Lightweight
                // Crypto API is not in the classpath.
                // We are just catching all kinds of Throwables to make sure
                // that we delete the newly created temp file.
                // Finally, we will pass on the catched exception.
                out.close();
                if (!outFile.delete()) {
                    // This could happen in situations where the file system
                    // allows us to open the file for overwriting, then
                    // overwriting failed (e.g. because of a cancelled password
                    // for an RAES encrypted ZIP file) and finally the file
                    // system also denied deleting the corrupted file.
                    // Shit happens!
                    final IOException ioe = new IOException(outFile.getPath()
                            + " (couldn't delete corrupted output file)");
                    ioe.initCause(ex);
                    throw ioe;
                }
                if (ex instanceof IOException)
                    throw (IOException) ex;
                else if (ex instanceof RuntimeException)
                    throw (RuntimeException) ex;
                else if (ex instanceof Error)
                    throw (Error) ex;
                else
                    throw new AssertionError(ex); // cannot happen!
            }
            outArchive.setMetaData(new OutputArchiveMetaData(this, outArchive));
        } catch (IOException ex) {
            assert outArchive == null;
            logger.log(Level.FINEST, "initOutArchive.throwing", ex); // NOI18N
            throw ex;
        }
        logger.log(Level.FINEST, "initOutArchive.exiting"); // NOI18N

        assert outArchive != null;
    }

    boolean hasNewData(String entryName) {
        assert readLock().isLocked() || writeLock().isLocked();
        return outArchive != null && outArchive.getArchiveEntry(entryName) != null;
    }

    void umount(
            final ArchiveException exceptionChain,
            final boolean waitInputStreams,
            final boolean closeInputStreams,
            final boolean waitOutputStreams,
            final boolean closeOutputStreams,
            final boolean umount,
            final boolean reassemble)
    throws ArchiveException {
        assert closeInputStreams || !closeOutputStreams; // closeOutputStreams => closeInputStreams
        assert !umount || reassemble; // umount => reassemble
        assert writeLock().isLocked();
        assert inArchive == null || inFile != null; // input archive => input file
        assert !isTouched() || outArchive != null; // file system touched => output archive
        assert outArchive == null || outFile != null; // output archive => output file

        // Do the logging part and leave the work to update0.
        logger.log(Level.FINER, "umount.entering", // NOI18N
                new Object[] {
                    getPath(),
                    exceptionChain,
                    Boolean.valueOf(waitInputStreams),
                    Boolean.valueOf(closeInputStreams),
                    Boolean.valueOf(waitOutputStreams),
                    Boolean.valueOf(closeOutputStreams),
                    Boolean.valueOf(umount),
                    Boolean.valueOf(reassemble),
        });
        try {
            umount0(exceptionChain,
                    waitInputStreams, closeInputStreams,
                    waitOutputStreams, closeOutputStreams,
                    umount, reassemble);
        } catch (ArchiveException ex) {
            logger.log(Level.FINER, "umount.throwing", ex); // NOI18N
            throw ex;
        }
        logger.log(Level.FINER, "umount.exiting"); // NOI18N
    }

    private void umount0(
            final ArchiveException exceptionChain,
            final boolean waitInputStreams,
            final boolean closeInputStreams,
            final boolean waitOutputStreams,
            final boolean closeOutputStreams,
            final boolean umount,
            final boolean reassemble)
    throws ArchiveException {
        ArchiveException newExceptionChain = exceptionChain;

        // Check output streams first, because closeInputStreams may be
        // true and closeOutputStreams may be false in which case we
        // don't even need to check open input streams if there are
        // some open output streams.
        if (outArchive != null) {
            final OutputArchiveMetaData outMetaData = outArchive.getMetaData();
            final int outStreams = outMetaData.waitAllOutputStreamsByOtherThreads(
                    waitOutputStreams ? 0 : 50);
            if (outStreams > 0) {
                if (!closeOutputStreams)
                    throw new ArchiveOutputBusyException(
                            newExceptionChain, getPath(), outStreams);
                newExceptionChain = new ArchiveOutputBusyWarningException(
                        newExceptionChain, getPath(), outStreams);
            }
        }
        if (inArchive != null) {
            final InputArchiveMetaData inMetaData = inArchive.getMetaData();
            final int inStreams = inMetaData.waitAllInputStreamsByOtherThreads(
                    waitInputStreams ? 0 : 50);
            if (inStreams > 0) {
                if (!closeInputStreams)
                    throw new ArchiveInputBusyException(
                            newExceptionChain, getPath(), inStreams);
                newExceptionChain = new ArchiveInputBusyWarningException(
                        newExceptionChain, getPath(), inStreams);
            }
        }

        try {
            if (isTouched()) {
                needsReassembly = true;
                try {
                    newExceptionChain = update(newExceptionChain);
                    assert getFileSystem() == null;
                    assert inArchive == null;
                } finally {
                    assert outArchive == null;
                }
                try {
                    if (reassemble) {
                        newExceptionChain = reassemble(newExceptionChain);
                        needsReassembly = false;
                    }
                } finally {
                    shutdownStep3(umount && !needsReassembly);
                }
            } else if (reassemble && needsReassembly) {
                // Nesting this archive file to its enclosing archive file
                // has been deferred until now.
                assert outFile == null; // isTouched() otherwise!
                assert inFile != null; // !needsReassembly otherwise!
                // Beware: inArchive or fileSystem may be initialized!
                shutdownStep2(newExceptionChain);
                outFile = inFile;
                inFile = null;
                try {
                    newExceptionChain = reassemble(newExceptionChain);
                    needsReassembly = false;
                } finally {
                    shutdownStep3(umount && !needsReassembly);
                }
            } else if (umount) {
                assert reassemble;
                assert !needsReassembly;
                shutdownStep2(newExceptionChain);
                shutdownStep3(true);
            } else {
                // This may happen if File.update() or File.umount() has
                // been called and no modifications have been applied to
                // this ArchiveController since its creation or last update.
                assert outArchive == null;
            }
        } catch (ArchiveException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ArchiveException(newExceptionChain, ex);
        } finally {
            setScheduled(needsReassembly);
        }

        if (newExceptionChain != exceptionChain)
            throw newExceptionChain;
    }

    final int waitAllInputStreamsByOtherThreads(long timeout) {
        return inArchive != null
                ? inArchive.getMetaData().waitAllInputStreamsByOtherThreads(timeout)
                : 0;
    }

    final int waitAllOutputStreamsByOtherThreads(long timeout) {
        return outArchive != null
                ? outArchive.getMetaData().waitAllOutputStreamsByOtherThreads(timeout)
                : 0;
    }

    /**
     * Updates all nodes in the virtual file system to the (temporary) output
     * archive file.
     * <p>
     * <b>This method is intended to be called by <code>update()</code> only!</b>
     *
     * @param exceptionChain the head of a chain of exceptions created so far.
     * @return If any warning exception condition occurs throughout the course
     *         of this method, an {@link ArchiveWarningException} is created
     *         (but not thrown), prepended to <code>exceptionChain</code> and
     *         finally returned.
     *         If multiple warning exception conditions occur, the prepended
     *         exceptions are ordered by appearance so that the <i>last</i>
     *         exception created is the head of the returned exception chain.
     * @throws ArchiveException If any exception condition occurs throughout
     *         the course of this method, an {@link ArchiveException}
     *         is created, prepended to <code>exceptionChain</code> and finally
     *         thrown unless it's an {@link ArchiveWarningException}.
     */
    private ArchiveException update(ArchiveException exceptionChain)
    throws ArchiveException {
        assert writeLock().isLocked();
        assert isTouched();
        assert outArchive != null;
        assert checkNoDeletedEntriesWithNewData(exceptionChain) == exceptionChain;

        final ArchiveFileSystem fileSystem = getFileSystem();
        final ArchiveEntry root = fileSystem.getRoot();
        try {
            try {
                try {
                    exceptionChain = shutdownStep1(exceptionChain);

                    ArchiveWarningException inputEntryCorrupted = null;
                    ArchiveWarningException outputEntryCorrupted = null;

                    final Enumeration e = fileSystem.getArchiveEntries();
                    while (e.hasMoreElements()) {
                        final ArchiveEntry entry = (ArchiveEntry) e.nextElement();
                        final String entryName = entry.getName();
                        if (hasNewData(entryName))
                            continue; // we have already written this entry
                        if (entry.isDirectory()) {
                            if (root == entry)
                                continue; // never write the virtual root directory
                            if (entry.getTime() < 0)
                                continue; // never write ghost directories
                            // 'entry' will never be used again, so it is safe
                            // to hand over this entry from the InputArchive
                            // to the OutputArchive.
                            outArchive.getOutputStream(entry, null).close();
                        } else if (inArchive != null && inArchive.getArchiveEntry(entryName) != null) {
                            assert entry == inArchive.getArchiveEntry(entryName);
                            InputStream in;
                            try {
                                in = inArchive.getInputStream(entry, entry);
                                assert in != null;
                            } catch (IOException ex) {
                                if (inputEntryCorrupted == null) {
                                    exceptionChain = inputEntryCorrupted
                                            = new ArchiveWarningException(
                                                exceptionChain,
                                                getPath() + " (skipped one or more corrupted archive entries in the input)",
                                                ex);
                                }
                                continue;
                            }
                            try {
                                // 'entry' will never be used again, so it is
                                // safe to hand over this entry from the
                                // InputArchive to the OutputArchive.
                                final OutputStream out = outArchive
                                        .getOutputStream(entry, entry);
                                try {
                                    Streams.cat(in, out);
                                } catch (InputIOException ex) {
                                    if (outputEntryCorrupted == null) {
                                        exceptionChain = outputEntryCorrupted
                                                = new ArchiveWarningException(
                                                    exceptionChain,
                                                    getPath() + " (one or more archive entries in the output are corrupted)",
                                                    ex);
                                    }
                                } finally {
                                    out.close();
                                }
                            } finally {
                                try {
                                    in.close();
                                } catch (IOException ex) {
                                    if (inputEntryCorrupted == null) {
                                        exceptionChain = inputEntryCorrupted
                                                = new ArchiveWarningException(
                                                    exceptionChain,
                                                    getPath() + " (one or more archive entries in the input are corrupted)",
                                                    ex);
                                    }
                                    throw ex;
                                }
                            }
                        } else {
                            // The entry is an archive file which has been
                            // newly created and not yet been reassembled
                            // into this (potentially new) archive file.
                            // Write an empty entry now as a marker in order to
                            // recreate the entry when the file system gets
                            // remounted from the archive file.
                            outArchive.getOutputStream(entry, null).close();
                        }
                    } // while (e.hasMoreElements())
                } finally {
                    // We MUST do cleanup here because (1) any entries in the
                    // filesystem which were successfully written (this is the
                    // normal case) have been modified by the OutputArchive
                    // and thus cannot get used anymore to access the input;
                    // and (2) if there has been any IOException on the
                    // output archive there is no way to recover from it.
                    shutdownStep2(exceptionChain);
                }
            } catch (IOException ex) {
                // The output file is corrupted! We must remove it now to
                // prevent it from being reused as the input file.
                // We do this even if the output file is the target file, i.e.
                // the archive file has just been created, because it
                // doesn't make any sense to keep a corrupted archive file:
                // There is no way to recover it and it could spoil any
                // attempts to redo the file operations, because TrueZIP would
                // normaly correctly identify it as a false positive archive
                // file and would not allow to treat it like a directory again.
                boolean deleted = outFile.delete();
                outFile = null;
                assert deleted;
                throw ex;
            }
        } catch (ArchiveException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ArchiveException(exceptionChain,
                    getPath() + " (could not update archive file - all changes are lost)",
                    ex);
        }

        // Set the last modification time of the output archive file
        // to the last modification time of the virtual root directory,
        // hence preserving it.
        if (!outFile.setLastModified(root.getTime()))
            exceptionChain = new ArchiveWarningException(exceptionChain,
                    getPath() + " (couldn't preserve last modification time)");

        return exceptionChain;
    }

    private ArchiveException checkNoDeletedEntriesWithNewData(
            ArchiveException exceptionChain) {
        assert isTouched();
        assert getFileSystem() != null;

        // Check if we have written out any entries that have been
        // deleted from the master directory meanwhile and prepare
        // to throw a warning exception.
        final ArchiveFileSystem fileSystem = getFileSystem();
        final Enumeration e = outArchive.getArchiveEntries();
        while (e.hasMoreElements()) {
            final ArchiveEntry entry = (ArchiveEntry) e.nextElement();
            final String entryName = entry.getName();
            /*final String entryName
                    = Paths.normalize(entry.getName(), ENTRY_SEPARATOR_CHAR);*/
            if (fileSystem.get(entryName) == null) {
                // The entry has been written out already, but also
                // has been deleted from the master directory meanwhile.
                // Create a warning exception, but do not yet throw it.
                exceptionChain = new ArchiveWarningException(exceptionChain,
                        getPath() + " (couldn't remove archive entry: " + entryName + ")");
            }
        }

        return exceptionChain;
    }

    /**
     * Uses the updated output archive file to reassemble the
     * target archive file, which may be an entry in an enclosing
     * archive file.
     * <p>
     * <b>This method is intended to be called by <code>update()</code> only!</b>
     *
     * @param exceptionChain the head of a chain of exceptions created so far.
     * @return If any warning condition occurs throughout the course of this
     *         method, a <code>ArchiveWarningException</code> is created (but not
     *         thrown), prepended to <code>exceptionChain</code> and finally
     *         returned.
     *         If multiple warning conditions occur,
     *         the prepended exceptions are ordered by appearance so that the
     *         <i>last</i> exception created is the head of the returned
     *         exception chain.
     * @return If any warning exception condition occurs throughout the course
     *         of this method, an {@link ArchiveWarningException} is created
     *         (but not thrown), prepended to <code>exceptionChain</code> and
     *         finally returned.
     *         If multiple warning exception conditions occur, the prepended
     *         exceptions are ordered by appearance so that the <i>last</i>
     *         exception created is the head of the returned exception chain.
     * @throws ArchiveException If any exception condition occurs throughout
     *         the course of this method, an {@link ArchiveException}
     *         is created, prepended to <code>exceptionChain</code> and finally
     *         thrown unless it's an {@link ArchiveWarningException}.
     */
    private ArchiveException reassemble(ArchiveException exceptionChain)
    throws ArchiveException {
        assert writeLock().isLocked();

        if (isRfsEntryTarget()) {
            // The archive file managed by this object is NOT enclosed in
            // another archive file.
            if (outFile != getTarget()) {
                // The archive file existed before and we have written
                // to a temporary output file.
                // Now copy the temporary output file to the target file,
                // preserving the last modification time and counting the
                // output.
                try {
                    final OutputStream out = new CountingOutputStream(
                            new java.io.FileOutputStream(getTarget()));
                    final InputStream in;
                    try {
                        in = new java.io.FileInputStream(outFile);
                    } catch (IOException ex) {
                        out.close();
                        throw ex;
                    }
                    File.cp(in , out); // always closes in and out
                } catch (IOException cause) {
                    throw new ArchiveException(
                            exceptionChain,
                            getPath()
                                + " (could not reassemble archive file - all changes are lost)",
                            cause);
                }

                // Set the last modification time of the target archive file
                // to the last modification time of the output archive file,
                // which has been set to the last modification time of the root
                // directory during update(...).
                final long time = outFile.lastModified();
                if (time != 0 && !getTarget().setLastModified(time)) {
                    exceptionChain = new ArchiveWarningException(
                            exceptionChain,
                            getPath()
                                + " (couldn't preserve last modification time)");
                }
            }
        } else {
            // The archive file managed by this archive controller IS
            // enclosed in another archive file.
            try {
                wrap(getEnclController(), getEnclEntryName());
            } catch (IOException cause) {
                throw new ArchiveException(
                        exceptionChain,
                        getEnclController().getPath() + "/" + getEnclEntryName()
                            + " (could not update archive entry - all changes are lost)",
                        cause);
            }
        }

        return exceptionChain;
    }

    private void wrap(
            final ArchiveController controller,
            final String entryName)
    throws IOException {
        assert writeLock().isLocked();
        assert controller != null;
        //assert !controller.readLock().isLocked();
        //assert !controller.writeLock().isLocked();
        assert entryName != null;
        assert !ROOT_NAME.equals(entryName);

        controller.runWriteLocked(new IORunnable() {
            public void run() throws IOException {
                wrapToWriteLockedController(controller, entryName);
            }
        });
    }

    private void wrapToWriteLockedController(
            final ArchiveController controller,
            final String entryName)
    throws IOException {
        assert controller != null;
        assert controller.writeLock().isLocked();
        assert entryName != null;
        assert !ROOT_NAME.equals(entryName);

        // Write the updated output archive file as an entry
        // to its enclosing archive file, preserving the
        // last modification time of the root directory as the last
        // modification time of the entry.
        final InputStream in = new java.io.FileInputStream(outFile);
        try {
            // We know that the enclosing controller's entry is not a false
            // positive, so we may safely pass in null as the destination
            // de.schlichtherle.io.File.
            Files.cp0(true, outFile, in, controller, entryName);
        } finally {
            in.close();
        }
    }

    /**
     * Resets the archive controller to its initial state - all changes to the
     * archive file which have not yet been updated get lost!
     * <p>
     * Thereafter, the archive controller will behave as if it has just been
     * created and any subsequent operations on its entries will remount
     * the virtual file system from the archive file again.
     */
    void reset() throws IOException {
        assert writeLock().isLocked();

        ArchiveException exceptionChain = shutdownStep1(null);
        shutdownStep2(exceptionChain);
        shutdownStep3(true);
        setScheduled(false);

        if (exceptionChain != null)
            throw exceptionChain;
    }

    protected void finalize() throws Throwable {
        try {
            logger.log(Level.FINEST, "finalize.entering", getPath()); // NOI18N
            // Note: If fileSystem or inArchive are not null, then the controller
            // has been used to perform read operations.
            // If outArchive is not null, the controller has been used to perform
            // write operations, but however, all file system transactions
            // must have failed.
            // Otherwise, the fileSystem would have been marked as touched and
            // we should never be made elegible for finalization!
            // Tactical note: Assertions don't work in a finalizer, so we use
            // logging.
            if (isTouched() || readLock().isLocked() || writeLock().isLocked())
                logger.log(Level.SEVERE, "finalize.invalidState", getPath());
            shutdownStep1(null);
            shutdownStep2(null);
            shutdownStep3(true);
        } finally {
            super.finalize();
        }
    }

    /**
     * Closes and disconnects all entry streams of the output and input
     * archive.
     */
    private ArchiveException shutdownStep1(ArchiveException exceptionChain) {
        if (outArchive != null)
            exceptionChain = outArchive.getMetaData().closeAllOutputStreams(
                    exceptionChain);
        if (inArchive != null)
            exceptionChain = inArchive.getMetaData().closeAllInputStreams(
                    exceptionChain);

        return exceptionChain;
    }

    /**
     * Discards the file system and closes the output and input archive.
     */
    private void shutdownStep2(ArchiveException exceptionChain)
    throws IOException {
        final ArchiveException oldExceptionChain = exceptionChain;

        super.reset(); // discard file system

        // The output archive must be closed BEFORE the input archive is
        // closed. This is because the input archive has been presented
        // to output archive as the "source" when it was created and may
        // be using the input archive when its closing to retrieve some
        // meta data information.
        // E.g. Zip32OutputArchive copies the postamble from the
        // Zip32InputArchive when it closes.
        if (outArchive != null) {
            try {
                outArchive.close();
            } catch (IOException ex) {
                exceptionChain = new ArchiveException(exceptionChain, ex);
            } finally {
                outArchive = null;
            }
        }

        if (inArchive != null) {
            try {
                inArchive.close();
            } catch (IOException ex) {
                exceptionChain = new ArchiveException(exceptionChain, ex);
            } finally {
                inArchive = null;
            }
        }

        if (exceptionChain != oldExceptionChain)
            throw exceptionChain;
    }

    /**
     * Cleans up temporary files.
     * 
     * @param deleteOutFile If this parameter is <code>true</code>,
     *        this method also deletes the temporary output file unless it's
     *        the target archive file (i.e. unless the archive file has been
     *        newly created).
     */
    private void shutdownStep3(final boolean deleteOutFile) {
        if (inFile != null) {
            if (inFile != getTarget()) {
                boolean deleted = inFile.delete();
                assert deleted;
            }
            inFile = null;
        }

        if (outFile != null) {
            if (deleteOutFile) {
                if (outFile != getTarget()) {
                    boolean deleted = outFile.delete();
                    assert deleted;
                }
            } else {
                //assert outFile != target; // may have been newly created
                assert outFile.isFile();
                inFile = outFile;
            }
            outFile = null;
        }

        if (deleteOutFile)
            needsReassembly = false;
    }
}
