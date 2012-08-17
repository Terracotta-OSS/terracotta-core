/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ArchiveDetector.java
 *
 * Created on 31. Juli 2005, 00:00
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
import java.net.*;

import org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi.*;

/**
 * Detects archive files solely by scanning file paths -
 * usually by testing for file name suffixes like <i>.zip</i> or the
 * like.
 * Whenever an archive file is recognized, the
 * {@link #getArchiveDriver(String)} method returns an instance of the
 * {@link ArchiveDriver} interface which allows to access it.
 * <p>
 * <code>ArchiveDetector</code> instances are assigned to <code>File</code>
 * instances in the following way:
 * <ol>
 * <li>If an archive detector is explicitly provided as a parameter to the
 *     constructor of the <code>File</code> class or any other method which
 *     creates <code>File</code> instances (e.g. <code>listFiles(*)</code>),
 *     then this archive detector is used.
 * <li>Otherwise, the archive detector returned by
 *     {@link File#getDefaultArchiveDetector} is used.
 *     This is initially set to the predefined instance {@link #DEFAULT}.
 *     Both the class property and the predefined instance can be customized.
 * </ol>
 * <p>
 * An archive file which has been recognized by an archive detector is said
 * to be a <i>prospective archive file</i>.
 * On the first read or write access to a prospective archive file, TrueZIP
 * checks its <i>true state</i> in cooperation with the {@link ArchiveDriver}.
 * If the true state of the file turns out to be actually a directory or not
 * to be compatible to the archive file format, it's said to be a <i>false
 * positive</i> archive file.
 * TrueZIP implements the appropriate behavior for all read or write
 * operations according to the true state.
 * Thanks to this design, TrueZIP detects and handles all kinds of false
 * positives correctly.
 * <p>
 * Implementations must be (virtually) immutable and hence thread safe.
 * <p>
 * Rather than implementing <code>ArchiveDetector</code> directly, it's easier
 * to instantiate or subclass the {@link DefaultArchiveDetector} class.
 * This class provides a registry for archive file suffixes and archive drivers
 * which can be easily customized via configuration files or Java code.
 * <p>
 * Since TrueZIP 6.4, although it's not required, it's recommended for
 * implementations to implement the {@link java.io.Serializable} interface,
 * too, so that {@link org.terracotta.agent.repkg.de.schlichtherle.io.File} instances which use it can
 * be serialized.
 * 
 * @see DefaultArchiveDetector
 * @see File
 * @see ArchiveDriver
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 */
public interface ArchiveDetector extends FileFactory {

    //
    // Predefined default implementations:
    // 

    /**
     * Never recognizes archive files in a path.
     * This can be used as the end of a chain of
     * <code>DefaultArchiveDetector</code> instances or if archive files
     * shall be treated like ordinary files rather than (virtual) directories.
     *
     * @see DefaultArchiveDetector
     */
    DefaultArchiveDetector NULL = new DefaultArchiveDetector(""); // or null
    
    /**
     * Recognizes the archive file suffixes defined by the key
     * <code>DEFAULT</code> in the configuration file(s).
     * If only TrueZIP's default configuration file is used, then this is set
     * so that no additional JARs are required on the runtime class path.
     * <p>
     * This archive detector is initially returned by
     * {@link File#getDefaultArchiveDetector}.
     * <p>
     * Note that the actual set of archive file suffixes detected by this
     * instance may be extended without prior notice in future releases.
     *
     * @see DefaultArchiveDetector How Configuration Files are located and
     *      processed by the DefaultArchiveDetector class
     */
    DefaultArchiveDetector DEFAULT = new DefaultArchiveDetector(
            GlobalArchiveDriverRegistry.INSTANCE.defaultSuffixes);

    /**
     * Recognizes all archive file suffixes registerd in the global registry
     * by the configuration file(s).
     * This requires <a href="{@docRoot}/overview-summary.html#defaults">additional JARs</a>
     * on the runtime class path.
     * <p>
     * Note that the actual set of archive file suffixes detected by this
     * instance may be extended without prior notice in future releases.
     *
     * @see DefaultArchiveDetector How Configuration Files are located and
     *      processed by the DefaultArchiveDetector class
     */
    DefaultArchiveDetector ALL = new DefaultArchiveDetector(
            GlobalArchiveDriverRegistry.INSTANCE.allSuffixes);

    //
    // The one and only method this interface really adds:
    //
    
    /**
     * Detects whether the given <code>path</code> identifies a prospective
     * archive file or not by applying heuristics to it and returns an
     * appropriate <code>ArchiveDriver</code> to use or <code>null</code>
     * if the path does not denote a prospective archive file or an
     * appropriate <code>ArchiveDriver</code> is not available for some
     * reason.
     * <p>
     * Please note that implementations <em>must not</em> check the actual
     * contents of the file identified by <code>path</code>!
     * This is because this method may be used to detect archive files
     * by their names before they are actually created or to detect archive
     * files which are enclosed in other archive files, in which case there
     * is no way to check the file contents in the real file system.
     * 
     * @param path The (not necessarily absolute) path of the prospective
     *        archive file.
     *        This does not actually need to be accessible in the real file
     *        system!
     * @return An <code>ArchiveDriver</code> instance for this archive file
     *         or <code>null</code> if the path does not denote an archive
     *         file (i.e. the path does not have a known suffix)
     *         or an appropriate <code>ArchiveDriver</code> is not available
     *         for some reason.
     * @throws NullPointerException If <code>path</code> is <code>null</code>.
     * @throws RuntimeException A subclass is thrown if loading or
     *         instantiating an archive driver class fails.
     */
    ArchiveDriver getArchiveDriver(String path);

    //
    // Specification of the (undocumented) contract inherited from FileFactory:
    //

    /**
     * Constructs a new {@link File} instance from the given
     * <code>blueprint</code>.
     * 
     * @param blueprint The file to use as a blueprint. If this is an instance
     *        of the {@link File} class, its fields are simply copied.
     *
     * @return A newly created instance of the class {@link File}.
     */
    File createFile(java.io.File blueprint);

    /**
     * This factory method is <em>not</em> for public use - do not use it!
     */
    // This is used by {@link File#getParentFile()} for fast file construction
    // without rescanning the entire path for archive files, which could even
    // lead to wrong results.
    // 
    // Calling this constructor with illegal arguments may result in
    // <code>IllegalArgumentException</code>, <code>AssertionError</code> or
    // may even silently fail!
    File createFile(java.io.File delegate, File innerArchive);

    /**
     * This factory method is <em>not</em> for public use - do not use it!
     */
    // This is used by some methods for fast file
    // construction without rescanning the pathname for archive files
    // when rewriting the pathname of an existing <code>File</code> instance.
    // <p>
    // Calling this method with illegal arguments may result in
    // <code>IllegalArgumentException</code>, <code>AssertionError</code> or
    // may even silently fail!
    File createFile(File blueprint, java.io.File delegate, File enclArchive);

    /**
     * Constructs a new {@link File} instance which uses this
     * {@link ArchiveDetector} to detect any archive files in its pathname.
     * 
     * @param path The pathname of the file.
     * @return A newly created instance of the class {@link File}.
     */
    File createFile(String path);

    /**
     * Constructs a new {@link File} instance which uses this
     * {@link ArchiveDetector} to detect any archive files in its pathname.
     * 
     * @param parent The parent pathname as a {@link String}.
     * @param child The child pathname as a {@link String}.
     *
     * @return A newly created instance of the class {@link File}.
     */
    File createFile(String parent, String child);

    /**
     * Constructs a new {@link File} instance which uses this
     * {@link ArchiveDetector} to detect any archive files in its pathname.
     * 
     * @param parent The parent pathname as a <code>File</code>.
     * @param child The child pathname as a {@link String}.
     *
     * @return A newly created instance of the class {@link File}.
     */
    File createFile(java.io.File parent, String child);

    /**
     * Constructs a new {@link File} instance from the given
     * <code>uri</code>. This method behaves similar to
     * {@link java.io.File#File(URI) new java.io.File(uri)} with the following
     * amendment:
     * If the URI matches the pattern
     * <code>(jar:)*file:(<i>path</i>!/)*<i>entry</i></code>, then the
     * constructed file object treats the URI like a (possibly ZIPped) file.
     * <p>
     * The newly created {@link File} instance uses this
     * {@link ArchiveDetector} to detect any archive files in its pathname.
     * 
     * @param uri an absolute, hierarchical URI with a scheme equal to
     *        <code>file</code> or <code>jar</code>, a non-empty path component,
     *        and undefined authority, query, and fragment components.
     *
     * @return A newly created instance of the class {@link File}.
     *
     * @throws NullPointerException if <code>uri</code> is <code>null</code>.
     * @throws IllegalArgumentException if the preconditions on the
     *         parameter <code>uri</code> do not hold.
     */
    File createFile(URI uri);

    /**
     * Creates a new {@link FileInputStream} to read the content of the
     * given file.
     * 
     * @param file The file to read.
     *
     * @return A newly created instance of the class {@link FileInputStream}.
     *
     * @throws FileNotFoundException On any I/O related issue when opening the file.
     */
    FileInputStream createFileInputStream(java.io.File file)
    throws FileNotFoundException;

    /**
     * Creates a new {@link FileOutputStream} to write the new content of the
     * given file.
     * 
     * @param file The file to write.
     * @param append If <code>true</code> the new content should be appended
     *        to the old content rather than overwriting it.
     *
     * @return A newly created instance of the class {@link FileOutputStream}.
     *
     * @throws FileNotFoundException On any I/O related issue when opening the file.
     */
    FileOutputStream createFileOutputStream(java.io.File file, boolean append)
    throws FileNotFoundException;
}
