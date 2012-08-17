/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ArchiveControllers.java
 *
 * Created on 4. Januar 2007, 14:19
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


import java.io.*;
import java.lang.ref.*;
import java.util.*;
import java.util.logging.*;

import org.terracotta.agent.repkg.de.schlichtherle.io.archive.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi.*;
import org.terracotta.agent.repkg.de.schlichtherle.key.*;

/**
 * Provides static utility methods for {@link ArchiveController}s.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.5
 */
final class ArchiveControllers {

    private static final String CLASS_NAME
            = "de/schlichtherle/io/ArchiveControllers".replace('/', '.'); // support code obfuscation!
    private static final Logger logger = Logger.getLogger(CLASS_NAME, CLASS_NAME);

    /**
     * The map of all archive controllers.
     * The keys are plain {@link java.io.File} instances and the values
     * are either <code>ArchiveController</code>s or {@link WeakReference}s
     * to <code>ArchiveController</code>s.
     * All access to this map must be externally synchronized!
     */
    private static final Map controllers = new WeakHashMap();

    private static final Comparator REVERSE_CONTROLLERS = new Comparator() {
        public int compare(Object o1, Object o2) {
            return  ((ArchiveController) o2).getTarget().compareTo(
                    ((ArchiveController) o1).getTarget());
        }
    };

    //
    // Static initializers.
    //

    static {
        Runtime.getRuntime().addShutdownHook(ShutdownHook.SINGLETON);
    }

    /** This class cannot get instantiated. */
    private ArchiveControllers() {
    }

    /**
     * Factory method returning an {@link ArchiveController} object for the
     * given archive file.
     * <p>
     * <b>Note:</b>
     * <ul>
     * <li>Neither <code>file</code> nor the enclosing archive file(s)
     *     need to actually exist for this to return a valid <code>ArchiveController</code>.
     *     Just the parent directories of <code>file</code> need to look like either
     *     an ordinary directory or an archive file, e.g. their lowercase
     *     representation needs to have a .zip or .jar ending.</li>
     * <li>It is an error to call this method on a target file which is
     *     not a valid name for an archive file</li>
     * </ul>
     */
    static ArchiveController get(final File file) {
        assert file != null;
        assert file.isArchive();

        java.io.File target = file.getDelegate();
        try {
            target = target.getCanonicalFile();
        } catch (IOException failure) {
            target = Files.normalize(target.getAbsoluteFile());
        }

        final ArchiveDriver driver = file.getArchiveDetector()
                .getArchiveDriver(target.getPath());

        ArchiveController controller = null;
        boolean reconfigure = false;
        try {
            synchronized (controllers) {
                final Object value = controllers.get(target);
                if (value instanceof Reference) {
                    controller = (ArchiveController) ((Reference) value).get();
                    // Check that the controller hasn't been garbage collected
                    // meanwhile!
                    if (controller != null) {
                        // If required, reconfiguration of the ArchiveController
                        // must be deferred until we have released the lock on
                        // controllers in order to prevent dead locks.
                        reconfigure = controller.getDriver() != driver;
                        return controller;
                    }
                } else if (value != null) {
                    // Do NOT reconfigure this ArchiveController with another
                    // ArchiveDetector: This controller is touched, i.e. it
                    // most probably has mounted the virtual file system and
                    // using another ArchiveDetector could potentially break
                    // the umount process.
                    // In effect, for an application this means that the
                    // reconfiguration of a previously used ArchiveController
                    // is only guaranteed to happen if
                    // (1) File.umount() or File.umount() has been called and
                    // (2) a new File instance referring to the previously used
                    // archive file as either the file itself or one
                    // of its ancestors is created with a different
                    // ArchiveDetector.
                    return (ArchiveController) value;
                }

                final File enclArchive = file.getEnclArchive();
                final ArchiveController enclController;
                final String enclEntryName;
                if (enclArchive != null) {
                    enclController = enclArchive.getArchiveController();
                    enclEntryName = file.getEnclEntryName();
                } else {
                    enclController = null;
                    enclEntryName = null;
                }

                // TODO: Refactor this to a more flexible design which supports
                // different umount strategies, like update or append.
                controller = new UpdatingArchiveController(
                        target, enclController, enclEntryName, driver);
            }
        } finally {
            if (reconfigure) {
                controller.writeLock().lock();
                try {
                    controller.setDriver(driver);
                } finally {
                    controller.writeLock().unlock();
                }
            }
        }

        return controller;
    }

    /**
     * Associates the given archive controller to the target file.
     *
     * @param target The target file. This must not be <code>null</code> or
     *        an instance of the <code>File</code> class in this package!
     * @param controller An {@link ArchiveController} or a
     *        {@link WeakReference} to an archive controller.
     */
    static final void set(final java.io.File target, final Object controller) {
        assert target != null;
        assert !(target instanceof File);
        assert controller instanceof ArchiveController
            || ((WeakReference) controller).get() instanceof ArchiveController;

        synchronized (controllers) {
            controllers.put(target, controller);
        }
    }

    /**
     * Updates all archive files in the real file system which's canonical
     * path name start with <code>prefix</code> with the contents of their
     * virtual file system, resets all cached state and deletes all temporary
     * files.
     * This method is thread safe.
     * 
     * @param prefix The prefix of the canonical path name of the archive files
     *        which shall get updated - <code>null</code> is not allowed!
     *        If the canonical pathname of an archive file does not start with
     *        this string, then it is not updated.
     * @param waitInputStreams Suppose any other thread has still one or more
     *        archive entry input streams open.
     *        Then if and only if this parameter is <code>true</code>, this
     *        method will wait until all other threads have closed their
     *        archive entry input streams.
     *        Archive entry input streams opened (and not yet closed) by the
     *        current thread are always ignored.
     *        If the current thread gets interrupted while waiting, it will
     *        stop waiting and proceed normally as if this parameter were
     *        <code>false</code>.
     *        Be careful with this parameter value: If a stream has not been
     *        closed because the client application does not always properly
     *        close its streams, even on an {@link IOException} (which is a
     *        typical bug in many Java applications), then this method may
     *        not return until the current thread gets interrupted!
     * @param closeInputStreams Suppose there are any open input streams
     *        for any archive entries because the application has forgot to
     *        close all {@link FileInputStream} objects or another thread is
     *        still busy doing I/O on an archive.
     *        Then if this parameter is <code>true</code>, an update is forced
     *        and an {@link ArchiveBusyWarningException} is finally thrown to
     *        indicate that any subsequent operations on these streams
     *        will fail with an {@link ArchiveEntryStreamClosedException}
     *        because they have been forced to close.
     *        This may also be used to recover an application from a
     *        {@link FileBusyException} thrown by a constructor of
     *        {@link FileInputStream} or {@link FileOutputStream}.
     *        If this parameter is <code>false</code>, the respective archive
     *        file is <em>not</em> updated and an {@link ArchiveBusyException}
     *        is thrown to indicate that the application must close all entry
     *        input streams first.
     * @param waitOutputStreams Similar to <code>waitInputStreams</code>,
     *        but applies to archive entry output streams instead.
     * @param closeOutputStreams Similar to <code>closeInputStreams</code>,
     *        but applies to archive entry output streams instead.
     *        If this parameter is <code>true</code>, then
     *        <code>closeInputStreams</code> must be <code>true</code>, too.
     *        Otherwise, an <code>IllegalArgumentException</code> is thrown.
     * @param umount If <code>true</code>, all temporary files get deleted, too.
     *        Thereafter, the archive controller will behave as if it has just been
     *        created and any subsequent operations on its entries will remount
     *        the virtual file system from the archive file again.
     *        Use this to allow subsequent changes to the archive files
     *        by other processes or via the <code>java.io.File*</code> classes
     *        <em>before</em> this package is used for read or write access to
     *        these archive files again.
     * @throws ArchiveBusyWarningExcepion If a archive file has been updated
     *         while the application is using any open streams to access it
     *         concurrently.
     *         These streams have been forced to close and the entries of
     *         output streams may contain only partial data.
     * @throws ArchiveWarningException If only warning conditions occur
     *         throughout the course of this method which imply that the
     *         respective archive file has been updated with
     *         constraints, such as a failure to set the last modification
     *         time of the archive file to the last modification time of its
     *         virtual root directory.
     * @throws ArchiveBusyException If an archive file could not get updated
     *         because the application is using an open stream.
     *         No data is lost and the archive file can still get updated by
     *         calling this method again.
     * @throws ArchiveException If any error conditions occur throughout the
     *         course of this method which imply loss of data.
     *         This usually means that at least one of the archive files
     *         has been created externally and was corrupted or it cannot
     *         get updated because the file system of the temp file or target
     *         file folder is full.
     * @throws NullPointerException If <code>prefix</code> is <code>null</code>.
     * @throws IllegalArgumentException If <code>closeInputStreams</code> is
     *         <code>false</code> and <code>closeOutputStreams</code> is
     *         <code>true</code>.
     */
    static void umount(
            final String prefix,
            final boolean waitInputStreams,
            final boolean closeInputStreams,
            final boolean waitOutputStreams,
            final boolean closeOutputStreams,
            final boolean umount)
    throws ArchiveException {
        if (prefix == null)
            throw new NullPointerException();
        if (!closeInputStreams && closeOutputStreams)
            throw new IllegalArgumentException();

        int controllersTotal = 0, controllersTouched = 0;
        logger.log(Level.FINE, "update.entering", // NOI18N
                new Object[] {
            prefix,
            Boolean.valueOf(waitInputStreams),
            Boolean.valueOf(closeInputStreams),
            Boolean.valueOf(waitOutputStreams),
            Boolean.valueOf(closeOutputStreams),
            Boolean.valueOf(umount),
        });
        try {
            // Reset statistics if it hasn't happened yet.
            CountingReadOnlyFile.init();
            CountingOutputStream.init();
            try {
                // Used to chain archive exceptions.
                ArchiveException exceptionChain = null;

                // The general algorithm is to sort the targets in descending order
                // of their pathnames (considering the system's default name
                // separator character) and then walk the array in reverse order to
                // call the umount() method on each respective archive controller.
                // This ensures that an archive file will always be updated
                // before its enclosing archive file.
                final Enumeration e = new ControllerEnumeration(
                        prefix, REVERSE_CONTROLLERS);
                while (e.hasMoreElements()) {
                    final ArchiveController controller
                            = (ArchiveController) e.nextElement();
                    controller.writeLock().lock();
                    try {
                        if (controller.isTouched())
                            controllersTouched++;
                        try {
                            // Upon return, some new ArchiveWarningException's may
                            // have been generated. We need to remember them for
                            // later throwing.
                            controller.umount(exceptionChain,
                                    waitInputStreams, closeInputStreams,
                                    waitOutputStreams, closeOutputStreams,
                                    umount, true);
                        } catch (ArchiveException exception) {
                            // Updating the archive file or wrapping it back into
                            // one of it's enclosing archive files resulted in an
                            // exception for some reason.
                            // We are bullheaded and store the exception chain for
                            // later throwing only and continue updating the rest.
                            exceptionChain = exception;
                        }
                    } finally {
                        controller.writeLock().unlock();
                    }
                    controllersTotal++;
                }

                // Reorder exception chain if necessary to support conditional
                // exception catching based on their priority (i.e. class).
                if (exceptionChain != null)
                    throw (ArchiveException) exceptionChain.sortPriority();
            } finally {
                CountingReadOnlyFile.resetOnInit();
                CountingOutputStream.resetOnInit();
            }
        } catch (ArchiveException failure) {
            logger.log(Level.FINE, "update.throwing", failure);// NOI18N
            throw failure;
        }
        logger.log(Level.FINE, "update.exiting", // NOI18N
                new Object[] {
            new Integer(controllersTotal),
            new Integer(controllersTouched)
        });
    }

    static final ArchiveStatistics getLiveArchiveStatistics() {
        return LiveArchiveStatistics.SINGLETON;
    }

    //
    // Static member classes and interfaces.
    //

    /**
     * TrueZIP's singleton shutdown hook for the JVM.
     * This shutdown hook is always run, even if the JVM terminates due to an
     * uncatched Throwable.
     * Only a JVM crash could prevent this, but this is an extremely rare
     * situation.
     */
    static final class ShutdownHook extends Thread {
        /** The singleton instance. */
        private static final ShutdownHook SINGLETON = new ShutdownHook();

        /**
         * The set of files to delete when the shutdown hook is run.
         * When iterating over it, its elements are returned in insertion order.
         */
        static final Set deleteOnExit
                = Collections.synchronizedSet(new LinkedHashSet());

        /** You cannot instantiate this singleton class. */
        private ShutdownHook() {
            super("TrueZIP ArchiveController Shutdown Hook");
            setPriority(Thread.MAX_PRIORITY);
            // Force loading the key manager now in order to prevent class
            // loading in the shutdown hook. This may help with environments
            // (app servers) which disable class loading in a shutdown hook.
            PromptingKeyManager.getInstance();
        }

        /**
         * Deletes all files that have been marked by
         * {@link File#deleteOnExit} and finally unmounts all controllers.
         * <p>
         * Logging and password prompting will be disabled (they wouldn't work
         * in a JVM shutdown hook anyway) in order to provide a deterministic
         * behaviour and in order to avoid RuntimeExceptions or even Errors
         * in the API.
         * <p>
         * Any exceptions thrown throughout the umount will be printed on
         * standard error output.
         * <p>
         * Note that this method is <em>not</em> re-entrant and should not be
         * directly called except for unit testing (you couldn't do a unit test
         * on a shutdown hook otherwise, could you?).
         */
        public void run() {
            synchronized (PromptingKeyManager.class) {
                try { // paranoid, but safe.
                    PromptingKeyManager.setPrompting(false);
                    logger.setLevel(Level.OFF);

                    for (Iterator i = deleteOnExit.iterator(); i.hasNext(); ) {
                        final File file = (File) i.next();
                        if (file.exists() && !file.delete()) {
                            System.err.println(
                                    file.getPath() + ": failed to deleteOnExit()!");
                        }
                    }
                } finally {
                    try {
                        umount("", false, true, false, true, true);
                    } catch (ArchiveException ouch) {
                        ouch.printStackTrace();
                    }
                }
            }
        }
    } // class ShutdownHook

    private static final class LiveArchiveStatistics
            implements ArchiveStatistics {
        private static final LiveArchiveStatistics SINGLETON
                = new LiveArchiveStatistics();

        /** You cannot instantiate this singleton class. */
        private LiveArchiveStatistics() {
        }

        public long getUpdateTotalByteCountRead() {
            return CountingReadOnlyFile.getTotal();
        }

        public long getUpdateTotalByteCountWritten() {
            return CountingOutputStream.getTotal();
        }

        public int getArchivesTotal() {
            // This is not 100% correct:
            // Controllers which have been removed from the WeakReference
            // VALUE in the map meanwhile, but not yet removed from the map
            // are counted as well.
            // But hey, this is only statistics, right?
            return controllers.size();
        }

        public int getArchivesTouched() {
            int result = 0;

            final Enumeration e = new ControllerEnumeration();
            while (e.hasMoreElements()) {
                final ArchiveController c = (ArchiveController) e.nextElement();
                c.readLock().lock();
                try {
                    if (c.isTouched())
                        result++;
                } finally {
                    c.readLock().unlock();
                }
            }

            return result;
        }

        public int getTopLevelArchivesTotal() {
            int result = 0;

            final Enumeration e = new ControllerEnumeration();
            while (e.hasMoreElements()) {
                final ArchiveController c = (ArchiveController) e.nextElement();
                if (c.getEnclController() == null)
                    result++;
            }

            return result;
        }

        public int getTopLevelArchivesTouched() {
            int result = 0;

            final Enumeration e = new ControllerEnumeration();
            while (e.hasMoreElements()) {
                final ArchiveController c = (ArchiveController) e.nextElement();
                c.readLock().lock();
                try {
                    if (c.getEnclController() == null && c.isTouched())
                        result++;
                } finally {
                    c.readLock().unlock();
                }
            }

            return result;
        }
    } // class LiveStatistics

    private static final class ControllerEnumeration implements Enumeration {
        private final Iterator it;

        ControllerEnumeration() {
            this("", null);
        }

        ControllerEnumeration(final String prefix, final Comparator c) {
            assert prefix != null;

            final Set snapshot;
            synchronized (controllers) {
                if (c != null) {
                    snapshot = new TreeSet(c);
                } else {
                    snapshot = new HashSet((int) (controllers.size() / 0.75f));
                }

                final Iterator it = controllers.values().iterator();
                while (it.hasNext()) {
                    Object value = it.next();
                    if (value instanceof Reference) {
                        value = ((Reference) value).get(); // dereference
                        if (value == null) {
                            // This may happen if there are no more strong
                            // references to the controller and it has been
                            // removed from the weak reference in the hash
                            // map's value before it's been removed from the
                            // hash map's key (shit happens)!
                            continue;
                        }
                    }
                    assert value != null;
                    assert value instanceof ArchiveController;
                    if (((ArchiveController) value).getPath().startsWith(prefix))
                        snapshot.add(value);
                }
            }

            it = snapshot.iterator();
        }

        public boolean hasMoreElements() {
            return it.hasNext();
        }

        public Object nextElement() {
            return it.next();
        }
    } // class ControllerEnumeration
}
