/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ArchiveFileSystem.java
 *
 * Created on 3. November 2004, 21:57
 */
/*
 * Copyright 2005-2006 Schlichtherle IT Services
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

import javax.swing.Icon;

import org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.util.*;

/**
 * This class implements a virtual file system of archive entries for use
 * by the archive controller provided to the constructor.
 * <p>
 * <b>WARNING:</b>This class is <em>not</em> thread safe!
 * All calls to non-static methods <em>must</em> be synchronized on the
 * respective <tt>ArchiveController</tt> object!
 * 
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0 (refactored from the former <code>ZipFileSystem</code>)
 */
final class ArchiveFileSystem implements Entry {

    /**
     * Denotes the entry name of the virtual root directory as a valid
     * directory entry name.
     * <p>
     * This constant <em>cannot</em> be used for identity comparison!
     *
     * @since TrueZIP 6.5
     */
    private static final String ROOT_DIRECTORY_NAME = SEPARATOR;

    /** The controller that this filesystem belongs to. */
    private final ArchiveFileSystemController controller;

    /** The read only status of this file system. */
    private final boolean readOnly;

    /**
     * The map of ArchiveEntries in this file system.
     * If this is a read-only file system, this is actually an unmodifiable
     * map.
     * This field should be considered final!
     * <p>
     * Note that the ArchiveEntries in this map are shared with the 
     * {@link InputArchive} object provided to this class' constructor.
     */
    private Map master;

    /** The archive entry for the virtual root of this file system. */
    private final ArchiveEntry root;

    /** The number of times this file system has been modified (touched). */
    private long touched;

    /** For use by {@link #split} only! */
    private final String[] split = new String[2];

    /**
     * Creates a new archive file system and ensures its integrity.
     * The root directory is created with its last modification time set to
     * the system's current time.
     * The file system is modifiable and marked as touched!
     * 
     * @param controller The controller which will use this file system.
     *        This constructor will finally call
     *        {@link ArchiveFileSystemController#touch} once it has fully
     *        initialized this instance.
     */
    ArchiveFileSystem(final ArchiveFileSystemController controller)
    throws IOException {
        this.controller = controller;
        touched = 1;
        master = new LinkedHashMap(64);

        // Setup root.
        root = createArchiveEntry(ROOT_DIRECTORY_NAME);
        root.setTime(System.currentTimeMillis());
        master.put(ROOT_DIRECTORY_NAME, root);

        readOnly = false;
        controller.touch();
    }

    /**
     * Mounts the archive file system from <tt>archive</tt> and ensures its
     * integrity.
     * First, a root directory with the given last modification time is
     * created - it's never loaded from the archive!
     * Then the entries from the archive are loaded into the file system and
     * its integrity is checked:
     * Any missing parent directories are created using the system's current
     * time as their last modification time - existing directories will never
     * be replaced.
     * <p>
     * Note that the entries in this file system are shared with
     * <code>archive</code>.
     * 
     * @param controller The controller which will use this file system.
     *        This constructor will solely use the controller as a factory
     *        to create missing archive entries using
     *        {@link ArchiveFileSystemController#createArchiveEntry}.
     * @param archive The archive to mount the file system from.
     * @param rootTime The last modification time of the root of the mounted
     *        file system in milliseconds since the epoch.
     * @param readOnly If and only if <code>true</code>, any subsequent
     *        modifying operation will result in a
     *        {@link ArchiveReadOnlyException}.
     */
    ArchiveFileSystem(
            final ArchiveFileSystemController controller,
            final InputArchive archive,
            final long rootTime,
            final boolean readOnly) {
        this.controller = controller;

        final int iniCap = (int) (archive.getNumArchiveEntries() / 0.75f) + 1;
        master = new LinkedHashMap(iniCap);

        // Setup root.
        root = createArchiveEntry(ROOT_DIRECTORY_NAME);
        root.setTime(rootTime); // do NOT yet touch the file system!
        master.put(ROOT_DIRECTORY_NAME, root);

        Enumeration entries = archive.getArchiveEntries();
        while (entries.hasMoreElements()) {
            final ArchiveEntry entry = (ArchiveEntry) entries.nextElement();
            final String entryName = entry.getName();
            // Map entry if it doesn't address the virtual root directory.
            if (!ROOT_DIRECTORY_NAME.equals(entryName)
                    && !("." + SEPARATOR).equals(entryName)) {
                entry.setMetaData(new ArchiveEntryMetaData(entry));
                master.put(entryName, entry);
            }
        }

        // Now perform a file system check to fix missing parent directories.
        // This needs to be done separately!
        //entries = Collections.enumeration(master.values()); // concurrent modification!
        entries = archive.getArchiveEntries();
        while (entries.hasMoreElements()) {
            final ArchiveEntry entry = (ArchiveEntry) entries.nextElement();
            if (isLegalEntryName(entry.getName()))
                fixParents(entry);
        }

        // Reset master map to be unmodifiable if this is a readonly file system
        this.readOnly = readOnly;
        if (readOnly)
            master = Collections.unmodifiableMap(master);

        assert touched == 0; // don't call !isTouched() - preconditions not met yet!
    }

    /**
     * Checks whether the given entry entryName is a legal entry name.
     * A legal entry name does not denote the virtual root directory, the dot
     * directory (<code>&quot;.&quot;</code>) or the dot-dot directory
     * (<code>&quot;..&quot;</code>) or any of their descendants.
     */
    private static boolean isLegalEntryName(final String entryName) {
        final int l = entryName.length();

        if (l <= 0)
            return false; // never fix empty pathnames

        switch (entryName.charAt(0)) {
        case SEPARATOR_CHAR:
            return false; // never fix root or absolute pathnames

        case '.':
            if (l >= 2) {
                switch (entryName.charAt(1)) {
                case '.':
                    if (l >= 3) {
                        if (entryName.charAt(2) == SEPARATOR_CHAR) {
                            assert entryName.startsWith(".." + SEPARATOR);
                            return false;
                        }
                        // Fall through.
                    } else {
                        assert "..".equals(entryName);
                        return false;
                    }
                    break;

                case SEPARATOR_CHAR:
                    assert entryName.startsWith("." + SEPARATOR);
                    return false;

                default:
                    // Fall through.
                }
            } else {
                assert ".".equals(entryName);
                return false;
            }
            break;

        default:
            // Fall through.
        }

        return true;
    }

    /**
     * Called from a constructor to fix the parent directories of
     * <tt>entry</tt>, ensuring that all parent directories of the entry
     * exist and that they contain the respective child.
     * If a parent directory does not exist, it is created using an
     * unkown time as the last modification time - this is defined to be a
     * <i>ghost<i> directory.
     * If a parent directory does exist, the respective child is added
     * (possibly yet again) and the process is continued.
     */
    private void fixParents(final ArchiveEntry entry) {
        final String entryName = entry.getName();
        // When recursing into this method, it may be called with the root
        // directory as its parameter, so we may NOT skip the following test.
        if (isRoot(entryName) || entryName.charAt(0) == SEPARATOR_CHAR)
            return; // never fix root or empty or absolute pathnames
        assert isLegalEntryName(entryName);

        final String split[] = split(entryName);
        final String parentName = split[0];
        final String baseName = split[1];

        ArchiveEntry parent = (ArchiveEntry) master.get(parentName);
        if (parent == null) {
            parent = createArchiveEntry(parentName);
            master.put(parentName, parent);
        }

        fixParents(parent);
        parent.getMetaData().children.add(baseName);
    }

    /**
     * Splits the given entry name in a parent entry name and a base name.
     * 
     * @param entryName The name of the entry which's parent entry name and
     *        base name are to be returned.
     * @return The {@link #split} array, which will hold at least two strings:
     *         <ul>
     *         <li>Index 0 holds the parent entry name.
     *             If <code>entryName</code> is empty or equals
     *             <code>SEPARATOR</code>, this is <code>null</code>.
     *             Otherwise, this contains the parent name of the entry and
     *             <em>always</em> ends with an <code>SEPARATOR</code>.
     *         <li>Index 1 holds the base name.
     *             If <code>entryName</code> is empty or equals
     *             <code>SEPARATOR</code>, this is an empty string.
     *             Otherwise, this contains the base name of the entry and
     *             <em>never</em> contains an <code>SEPARATOR</code>.
     *         </ul>
     * @throws NullPointerException If <code>entryName</code> is
     *         <code>null</code>.
     */
    private final String[] split(final String entryName) {
        //return Paths.split(entryName, SEPARATOR_CHAR, split);
        return split(entryName, split);
    }

    // This method is package private only to enable unit tests!
    static String[] split(final String entryName, final String[] result) {
        assert entryName != null;
        assert result != null;
        assert result.length >= 2;

        // Calculate index of last character, ignoring trailing entry separator.
        int end = entryName.length();
        if (0 <= --end)
            if (entryName.charAt(end) == SEPARATOR_CHAR)
                end--;

        // Now look for the separator.
        int base = entryName.lastIndexOf(SEPARATOR_CHAR, end);
        end++; // convert end index to interval boundary

        // Finally split according to our findings.
        if (base != -1) { // found slash?
            base++;
            result[0] = entryName.substring(0, base); // include separator, may produce only separator!
            result[1] = entryName.substring(base, end); // between separator and trailing separator
        } else { // no slash
            if (end > 0) { // At least one character exists, excluding a trailing separator?
                result[0] = ROOT_DIRECTORY_NAME;
            } else {
                result[0] = null; // no parent
            }
            result[1] = entryName.substring(0, end); // between prefix and trailing separator
        }

        return result;
    }

    /**
     * Indicates whether this file system is read only or not.
     * The default is <tt>false</tt>.
     */
    boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Indicates whether this file system has been modified since
     * its time of creation or the last call to <tt>resetTouched()</tt>.
     */
    boolean isTouched() {
        assert controller.getFileSystem() == this;
        return touched != 0;
    }

    /**
     * Ensures that the controller's data structures required to output
     * entries are properly initialized and marks this virtual archive
     * file system as touched.
     *
     * @throws ArchiveReadOnlyExceptionn If this virtual archive file system
     *         is read only.
     * @throws IOException If setting up the required data structures in the
     *         controller fails for some reason.
     */
    private void touch() throws IOException {
        if (isReadOnly())
            throw new ArchiveReadOnlyException();

        // Order is important here because of exceptions!
        if (touched == 0)
            controller.touch();
        touched++;
    }

    /**
     * Returns an enumeration of all <code>ArchiveEntry</code> instances
     * in this file system.
     */
    Enumeration getArchiveEntries() {
        assert controller.getFileSystem() == this;
        return Collections.enumeration(master.values());
    }
    
    /**
     * Returns the virtual root directory of this file system.
     * This archive entry always exists.
     * It's name may depend on the archive type.
     * It's last modification time is guaranteed to be non-negative, so it's
     * <em>not</em> a ghost directory!
     */
    ArchiveEntry getRoot() {
        assert controller.getFileSystem() == this;
        return root;
    }

    /**
     * Returns <code>true</code> iff the given entry name refers to the
     * virtual root directory within this controller.
     */
    static final boolean isRoot(String entryName) {
        return ROOT_NAME == entryName; // possibly assigned by File.init(...)
    }

    /**
     * Looks up the specified entry in the file system and returns it or
     * <tt>null</tt> if not existent.
     */
    ArchiveEntry get(String entryName) {
        assert entryName != null;
        assert controller.getFileSystem() == this;
        return (ArchiveEntry) master.get(entryName);
    }

    /**
     * Equivalent to {@link #link(String, boolean, ArchiveEntry)
     * link(entryName, createParents, null)}.
     */
    Delta link(final String entryName, final boolean createParents)
    throws ArchiveFileSystemException {
        return link(entryName, createParents, null);
    }

    /**
     * Begins a &quot;create and link entry&quot; transaction to ensure that either a
     * new entry for the given <tt>entryName</tt> will be created or an
     * existing entry is replaced within this virtual archive file system.
     * <p>
     * This is the first step of a two-step process to create an archive entry
     * and link it into this virtual archive file system.
     * To commit the transaction, call {@link Delta#commit} on the returned object
     * after you have successfully conducted the operations which compose the
     * transaction.
     * <p>
     * Upon a <code>commit</code> operation, the last modification time of
     * the newly created and linked entries will be set to the system's
     * current time at the moment the transaction has begun and the file
     * system will be marked as touched at the moment the transaction has
     * been committed.
     * <p>
     * Note that there is no rollback operation: After this method returns,
     * nothing in the virtual file system has changed yet and all information
     * required to commit the transaction is contained in the returned object.
     * Hence, if the operations which compose the transaction fails, the
     * returned object may be safely collected by the garbage collector,
     * 
     * @param entryName The relative path name of the entry to create or replace.
     * @param createParents If <tt>true</tt>, any non-existing parent
     *        directory will be created in this file system with its last
     *        modification time set to the system's current time.
     * @param template If not <code>null</code>, then the newly created or
     *        replaced entry shall inherit as much properties from this
     *        instance as possible (with the exception of the name).
     *        This is typically used for archive copy operations and requires
     *        some support by the archive driver.
     * @return A transaction object. You must call its
     *         {@link Delta#commit} method in order to commit
     *         link the newly created entry into this virtual archive file
     *         system.
     * @throws ArchiveReadOnlyExceptionn If this virtual archive file system
     *         is read only.
     * @throws ArchiveFileSystemException If one of the following is true:
     *         <ul>
     *         <li><code>entryName</code> contains characters which are not
     *             supported by the archive file.
     *         <li>The entry name indicates a directory (trailing <tt>/</tt>)
     *             and its entry does already exist within this file system.
     *         <li>The entry is a file or directory and does already exist as
     *             the respective other type within this file system.
     *         <li>The parent directory does not exist and
     *             <tt>createParents</tt> is <tt>false</tt>.
     *         <li>One of the entry's parents denotes a file.
     *         </ul>
     */
    Delta link(
            final String entryName,
            final boolean createParents,
            final ArchiveEntry template)
    throws ArchiveFileSystemException {
        assert isRoot(entryName) || entryName.charAt(0) != SEPARATOR_CHAR;
        assert controller.getFileSystem() == this;

        if (isRoot(entryName))
            throw new ArchiveFileSystemException(entryName,
                    "virtual root directory cannot get replaced");

        return new LinkDelta(entryName, createParents, template);
    }

    /**
     * A simple transaction for creating (and hence probably replacing) and
     * linking an entry in this virtual archive file system.
     * 
     * @see #link
     */
    private final class LinkDelta extends AbstractDelta {
        final Element[] elements;

        private LinkDelta(
                final String entryName,
                final boolean createParents,
                final ArchiveEntry template)
        throws ArchiveFileSystemException {
            if (isReadOnly())
                throw new ArchiveReadOnlyException();
            try {
                elements = createElements(entryName, createParents, template, 1);
            } catch (CharConversionException cce) {
                final ArchiveFileSystemException afse
                        = new ArchiveFileSystemException(cce.toString());
                afse.initCause(cce);
                throw afse;
            }
        }

        private Element[] createElements(
                final String entryName,
                final boolean createParents,
                final ArchiveEntry template,
                final int level)
        throws ArchiveFileSystemException, CharConversionException {
            final String split[] = split(entryName);
            final String parentName = split[0]; // could be separator only to indicate root
            final String baseName = split[1];

            final Element[] elements;

            // Lookup parent entry, creating it where necessary and allowed.
            final ArchiveEntry parent = (ArchiveEntry) master.get(parentName);
            final ArchiveEntry entry;
            if (parent != null) {
                final ArchiveEntry oldEntry
                        = (ArchiveEntry) master.get(entryName);
                ensureMayBeReplaced(entryName, oldEntry);
                elements = new Element[level + 1];
                elements[0] = new Element(parentName, parent);
                entry = createArchiveEntry(entryName, template);
                elements[1] = new Element(baseName, entry);
            } else if (createParents) {
                elements = createElements(
                        parentName, createParents, null, level + 1);
                entry = createArchiveEntry(entryName, template);
                elements[elements.length - level]
                        = new Element(baseName, entry);
            } else {
                throw new ArchiveFileSystemException(entryName,
                        "missing parent directory");
            }

            return elements;
        }

        private void ensureMayBeReplaced(
                final String entryName,
                final ArchiveEntry oldEntry)
        throws ArchiveFileSystemException {
            final int end = entryName.length() - 1;
            if (entryName.charAt(end) == SEPARATOR_CHAR) { // entryName indicates directory
                if (oldEntry != null)
                    throw new ArchiveFileSystemException(entryName,
                            "directories cannot get replaced");
                if (master.get(entryName.substring(0, end)) != null)
                    throw new ArchiveFileSystemException(entryName,
                            "directories cannot replace files");
            } else { // entryName indicates file
                if (master.get(entryName + SEPARATOR) != null)
                    throw new ArchiveFileSystemException(entryName,
                            "files cannot replace directories");
            }
        }

        /** Links the entries into this virtual archive file system. */
        public void commit() throws IOException {
            assert controller.getFileSystem() == ArchiveFileSystem.this;
            assert elements.length >= 2;

            touch();

            final long time = System.currentTimeMillis();
            final int l = elements.length;

            ArchiveEntry parent = elements[0].entry;
            for (int i = 1; i < l ; i++) {
                final Element element = elements[i];
                final String baseName = element.baseName;
                final ArchiveEntry entry = element.entry;
                if (parent.getMetaData().children.add(baseName)
                        && parent.getTime() != ArchiveEntry.UNKNOWN) // never touch ghosts!
                    parent.setTime(time);
                master.put(entry.getName(), entry);
                parent = entry;
            }

            final ArchiveEntry entry = elements[l - 1].entry;
            if (entry.getTime() == ArchiveEntry.UNKNOWN)
                entry.setTime(time);
        }

        public ArchiveEntry getEntry() {
            assert controller.getFileSystem() == ArchiveFileSystem.this;

            return elements[elements.length - 1].entry;
        }
    } // class LinkDelta

    private static abstract class AbstractDelta implements Delta {
        /** A data class for use by subclasses. */
        static class Element {
            final String baseName;
            final ArchiveEntry entry;

            // This constructor is provided for convenience only.
            Element(String baseName, ArchiveEntry entry) {
                this.baseName = baseName; // may be null!
                assert entry != null;
                this.entry = entry;
            }
        }
    } // class AbstractDelta

    /**
     * This interface encapsulates the methods required to begin and commit
     * a simplified transaction (a delta) on this virtual archive file system.
     * <p>
     * Note that there is no <code>begin</code> or <code>rollback</code>
     * method in this class.
     * Instead, <code>begin</code> is expected to be implemented by the
     * constructor of the implementation and must not modify the file system,
     * so that an explicit <code>rollback</code> is not required.
     */
    interface Delta {

        /**
         * Returns the entry operated by this file system delta.
         */
        ArchiveEntry getEntry();

        /**
         * Commits the simplified transaction, possibly modifying the
         * enclosing virtual archive file system.
         *
         * @throws IOException If the commit operation fails for any I/O
         *         related reason.
         */
        void commit() throws IOException;
    } // interface Delta

    /**
     * Creates an archive entry which is going to be linked into this virtual
     * archive file system in the near future.
     * The returned entry has properly initialized meta data, but is
     * otherwise left as created by the archive driver.
     * 
     * @param entryName The path name of the entry to create or replace.
     *        This must be a relative path name.
     * @param blueprint If not <code>null</code>, then the newly created entry
     *        shall inherit as much attributes from this object as possible
     *        (with the exception of the name).
     *        This is typically used for archive copy operations and requires
     *        some support by the archive driver.
     * @return An {@link ArchiveEntry} created by the archive driver and
     *         properly initialized with meta data.
     * @throws CharConversionException If <code>entryName</code> contains
     *         characters which are not supported by the archive file.
     */
    private ArchiveEntry createArchiveEntry(
            final String entryName,
            final ArchiveEntry blueprint)
    throws CharConversionException {
        final ArchiveEntry entry
                = controller.createArchiveEntry(entryName, blueprint);
        entry.setMetaData(new ArchiveEntryMetaData(entry));
        return entry;
    }

    /**
     * Like {@link #createArchiveEntry}, but throws an
     * <code>AssertionError</code> instead of
     * <code>CharConversionException</code>.
     *
     * @throws AssertionError If a {@link CharConversionException} occurs.
     */
    private ArchiveEntry createArchiveEntry(final String entryName) {
        try {
            return createArchiveEntry(entryName, null);
        } catch (CharConversionException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * If this method returns, the entry identified by the given
     * <tt>entryName</tt> has been successfully deleted from the virtual
     * archive file system.
     * If the entry is a directory, it must be empty for successful deletion.
     * 
     * @throws ArchiveReadOnlyExceptionn If the virtual archive file system is
     *         read only.
     * @throws ArchiveIllegalOperationException If the operation failed for
     *         any other reason.
     */
    private void unlink(final String entryName)
    throws IOException {
        if (isRoot(entryName))
            throw new ArchiveFileSystemException(entryName,
                    "virtual root directory cannot get unlinked");

        try {
            final ArchiveEntry entry = (ArchiveEntry) master.remove(entryName);
            if (entry == null)
                throw new ArchiveFileSystemException(entryName,
                        "entry does not exist");
            if (entry == root
                    || entry.isDirectory()
                        && entry.getMetaData().children.size() != 0) {
                master.put(entryName, entry); // Restore file system
                throw new ArchiveFileSystemException(entryName,
                        "directory is not empty");
            }
            final String split[] = split(entryName);
            final String parentName = split[0];
            final ArchiveEntry parent = (ArchiveEntry) master.get(parentName);
            assert parent != null : "The parent directory of \"" + entryName
                        + "\" is missing - archive file system is corrupted!";
            final boolean ok = parent.getMetaData().children.remove(split[1]);
            assert ok : "The parent directory of \"" + entryName
                        + "\" does not contain this entry - archive file system is corrupted!";
            touch();
            if (parent.getTime() != ArchiveEntry.UNKNOWN) // never touch ghosts!
                parent.setTime(System.currentTimeMillis());
        } catch (UnsupportedOperationException unmodifiableMap) {
            throw new ArchiveReadOnlyException();
        }
    }

    //
    // File system operations used by the ArchiveController class:
    //
    
    boolean exists(final String entryName) {
        return get(entryName) != null
            || get(entryName + SEPARATOR) != null;
    }
    
    boolean isFile(final String entryName) {
        /*ArchiveEntry entry = get(entryName);
        if (entry == null)
            entry = get(entryName + SEPARATOR);
        return entry != null && !entry.isDirectory();*/
        return get(entryName) != null;
    }
    
    boolean isDirectory(final String entryName) {
        /*ArchiveEntry entry = get(entryName + SEPARATOR);
        if (entry == null)
            entry = get(entryName);
        return entry != null && entry.isDirectory();*/
        return get(entryName + SEPARATOR) != null;
    }

    Icon getOpenIcon(final String entryName) {
        assert !isRoot(entryName);

        ArchiveEntry entry = get(entryName);
        if (entry == null)
            entry = get(entryName + SEPARATOR);
        return entry != null ? entry.getOpenIcon() : null;
    }

    Icon getClosedIcon(final String entryName) {
        assert !isRoot(entryName);

        ArchiveEntry entry = get(entryName);
        if (entry == null)
            entry = get(entryName + SEPARATOR);
        return entry != null ? entry.getClosedIcon() : null;
    }
    
    boolean canWrite(final String entryName) {
        return !isReadOnly() && exists(entryName);
    }

    boolean setReadOnly(final String entryName) {
        return isReadOnly() && exists(entryName);
    }
    
    long length(final String entryName) {
        final ArchiveEntry entry = get(entryName);
        if (entry == null || entry.isDirectory())
            return 0;

        // TODO: Review: Can we avoid this special case?
        // It's probably Zip32Driver specific!
        // This entry is a plain file in the file system.
        // If entry.getSize() returns ArchiveEntry.UNKNOWN, the length is yet unknown.
        // This may happen if e.g. a ZIP entry has only been partially
        // written, i.e. not yet closed by another thread, or if this is a
        // ghost directory.
        // As this is not specified in the contract of the File class, return
        // 0 in this case instead.
        final long length = entry.getSize();
        return length != ArchiveEntry.UNKNOWN ? length : 0;
    }

    long lastModified(final String entryName) {
        ArchiveEntry entry = get(entryName);
        if (entry == null)
            entry = get(entryName + SEPARATOR);
        if (entry != null) {
            // Depending on the driver type, entry.getTime() could return
            // a negative value. E.g. this is the default value that the
            // ArchiveDriver uses for newly created entries in order to
            // indicate an unknown time.
            // As this is not specified in the contract of the File class,
            // 0 is returned in this case instead.
            final long time = entry.getTime();
            return time >= 0 ? time : 0;
        }
        // This entry does not exist.
        return 0;
    }

    boolean setLastModified(final String entryName, final long time)
    throws IOException {
        if (time < 0)
            throw new IllegalArgumentException(entryName +
                    " (negative entry modification time)");

        if (isReadOnly())
            return false;

        ArchiveEntry entry = get(entryName);
        if (entry == null) {
            entry = get(entryName + SEPARATOR);
            if (entry == null) {
                // This entry does not exist.
                return false;
            }
        }

        // Order is important here!
        touch();
        entry.setTime(time);

        return true;
    }
    
    String[] list(final String entryName) {
        // Lookup the entry as a directory.
        final ArchiveEntry entry = get(entryName + SEPARATOR);
        if (entry != null)
            return entry.getMetaData().list();
        else
            return null; // does not exist as a directory
    }
    
    String[] list(
            final String entryName,
            final FilenameFilter filenameFilter,
            final File dir) {
        // Lookup the entry as a directory.
        final ArchiveEntry entry = get(entryName + SEPARATOR);
        if (entry != null)
            if (filenameFilter != null)
                return entry.getMetaData().list(filenameFilter, dir);
            else
                return entry.getMetaData().list(); // most efficient
        else
            return null; // does not exist as directory
    }

    File[] listFiles(
            final String entryName,
            final FilenameFilter filenameFilter,
            final File dir,
            final FileFactory factory) { // deprecated warning is OK!
        // Lookup the entry as a directory.
        final ArchiveEntry entry = get(entryName + SEPARATOR);
        if (entry != null)
            return entry.getMetaData().listFiles(filenameFilter, dir, factory);
        else
            return null; // does not exist as a directory
    }
    
    File[] listFiles(
            final String entryName,
            final FileFilter fileFilter,
            final File dir,
            final FileFactory factory) { // deprecated warning is OK!
        // Lookup the entry as a directory.
        final ArchiveEntry entry = get(entryName + SEPARATOR);
        if (entry != null)
            return entry.getMetaData().listFiles(fileFilter, dir, factory);
        else
            return null; // does not exist as a directory
    }

    void mkdir(String entryName, boolean createParents)
    throws IOException {
        link(entryName + SEPARATOR, createParents).commit();
    }
    
    void delete(final String entryName)
    throws IOException {
        assert isRoot(entryName) || entryName.charAt(0) != SEPARATOR_CHAR;

        if (get(entryName) != null) {
            unlink(entryName);
            return;
        }

        final String dirEntryName = entryName + SEPARATOR;
        if (get(dirEntryName) != null) {
            unlink(dirEntryName);
            return;
        }

        throw new ArchiveFileSystemException(entryName,
                "archive entry does not exist");
    }

    //
    // Exceptions:
    //

    /**
     * This exception is thrown when a client application tries to perform an
     * illegal operation on an archive file system.
     * <p>
     * This exception is private by intention: Clients applications should not
     * even know about the existence of virtual archive file systems.
     */
    static class ArchiveFileSystemException extends IOException {
        /** The entry's path name. */
        private final String entryName;

        private ArchiveFileSystemException(String message) {
            super(message);
            this.entryName = null;
        }

        private ArchiveFileSystemException(String entryName, String message) {
            super(message);
            this.entryName = entryName;
        }

        public String getMessage() {
            // For performance reasons, this string is constructed on demand
            // only!
            return entryName != null
                    ? entryName + " (" + super.getMessage() + ")"
                    : super.getMessage();
        }
    }

    /**
     * This exception is thrown when a client tries to modify a read only
     * virtual archive file system.
     */
    static class ArchiveReadOnlyException extends ArchiveFileSystemException {
        private ArchiveReadOnlyException() {
            super("Archive file is read-only!");
        }
    }
}
