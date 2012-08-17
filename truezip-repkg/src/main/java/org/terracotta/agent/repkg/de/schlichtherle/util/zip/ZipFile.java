/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

package org.terracotta.agent.repkg.de.schlichtherle.util.zip;


import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.terracotta.agent.repkg.de.schlichtherle.io.rof.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.util.*;

/**
 * Drop-in replacement for {@link java.util.zip.ZipFile java.util.zip.ZipFile}.
 * <p>
 * Where the constructors of this class accept a <code>charset</code>
 * parameter, this is used to decode comments and entry names in the ZIP file.
 * However, if an entry has bit 11 set in its General Purpose Bit Flag,
 * then this parameter is ignored and "UTF-8" is used for this entry.
 * This in accordance to Appendix D of PKWARE's ZIP File Format Specification
 * (<a href="http://www.pkware.com/documents/casestudies/APPNOTE.TXT">http://www.pkware.com/documents/casestudies/APPNOTE.TXT</a>).
 * <p>
 * This class is able to skip a preamble like the one found in self extracting
 * archives.
 * <p>
 * The entries returned by this class are instances of
 * <code>de.schlichtherle.util.zip.ZipEntry</code> instead of
 * <code>java.util.zip.ZipEntry</code>.
 * <p>
 * Note that there is no <code>getName()</code> method.
 * <p>
 * This class is thread-safe.
 * </ul>
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @see ZipOutputStream
 */
public class ZipFile extends BasicZipFile {

    /**
     * Opens the given file for reading its ZIP contents,
     * assuming UTF-8 charset for file names.
     * 
     * @param name Name of the file.
     * @throws NullPointerException If <code>name</code> is <code>null</code>.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws ZipException If the file is not ZIP compatible.
     * @throws IOException On any other I/O related issue.
     */
    public ZipFile(String name)
    throws  NullPointerException,
            FileNotFoundException,
            ZipException,
            IOException {
        super(name);
    }

    /**
     * Opens the given file for reading its ZIP contents,
     * assuming the specified charset for file names.
     * 
     * @param name Name of the file.
     * @param charset The charset to use for comments and entry names.
     * @throws NullPointerException If <code>name</code> or <code>charset</code> is
     *         <code>null</code>.
     * @throws UnsupportedEncodingException If charset is not supported by
     *         this JVM.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws ZipException If the file is not ZIP compatible.
     * @throws IOException On any other I/O related issue.
     */
    public ZipFile(String name, String charset)
    throws  NullPointerException,
            UnsupportedEncodingException,
            FileNotFoundException,
            ZipException,
            IOException {
        super(name, charset);
    }

    /**
     * Opens the given file for reading its ZIP contents,
     * assuming the specified charset for file names.
     * 
     * @param name Name of the file.
     * @param charset The charset to use for comments and entry names.
     * @param preambled If this is <code>true</code>, then the ZIP compatible
     *        file may have a preamble.
     *        Otherwise, the ZIP compatible file must start with either a
     *        Local File Header (LFH) signature or an End Of Central Directory
     *        (EOCD) Header, causing this constructor to fail fast if the file
     *        is actually a false positive ZIP compatible file, i.e. not
     *        compatible to the ZIP File Format Specification.
     *        This may be used to read Self Extracting ZIP files (SFX), which
     *        usually contain the application code required for extraction in
     *        a preamble.
     *        This parameter is <code>true</code> by default.
     * @param postambled If this is <code>true</code>, then the ZIP compatible
     *        file may have a postamble of arbitrary length.
     *        Otherwise, the ZIP compatible file must not have a postamble
     *        which exceeds 64KB size, including the End Of Central Directory
     *        record (i.e. including the ZIP file comment), causing this
     *        constructor to fail fast if the file is actually a false positive
     *        ZIP compatible file, i.e. not compatible to the ZIP File Format
     *        Specification.
     *        This may be used to read Self Extracting ZIP files (SFX) with
     *        large postambles.
     *        This parameter is <code>false</code> by default.
     * @throws NullPointerException If <code>name</code> or <code>charset</code> is
     *         <code>null</code>.
     * @throws UnsupportedEncodingException If charset is not supported by
     *         this JVM.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws ZipException If the file is not ZIP compatible.
     * @throws IOException On any other I/O related issue.
     */
    public ZipFile(
            String name,
            String charset,
            boolean preambled,
            boolean postambled)
    throws  NullPointerException,
            UnsupportedEncodingException,
            FileNotFoundException,
            ZipException,
            IOException {
        super(name, charset, preambled, postambled);
    }

    /**
     * Opens the given file for reading its ZIP contents,
     * assuming UTF-8 charset for file names.
     * 
     * @param file The file.
     * @throws NullPointerException If <code>file</code> is <code>null</code>.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws ZipException If the file is not ZIP compatible.
     * @throws IOException On any other I/O related issue.
     */
    public ZipFile(File file)
    throws  NullPointerException,
            FileNotFoundException,
            ZipException,
            IOException {
        super(file);
    }

    /**
     * Opens the given file for reading its ZIP contents,
     * assuming the specified charset for file names.
     * 
     * @param file The file.
     * @param charset The charset to use for comments and entry names.
     * @throws NullPointerException If <code>file</code> or <code>charset</code> is
     *         <code>null</code>.
     * @throws UnsupportedEncodingException If charset is not supported by
     *         this JVM.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws ZipException If the file is not ZIP compatible.
     * @throws IOException On any other I/O related issue.
     */
    public ZipFile(File file, String charset)
    throws  NullPointerException,
            UnsupportedEncodingException,
            FileNotFoundException,
            ZipException,
            IOException {
        super(file, charset);
    }

    /**
     * Opens the given file for reading its ZIP contents,
     * assuming the specified charset for file names.
     * 
     * @param file The file.
     * @param charset The charset to use for comments and entry names.
     * @param preambled If this is <code>true</code>, then the ZIP compatible
     *        file may have a preamble.
     *        Otherwise, the ZIP compatible file must start with either a
     *        Local File Header (LFH) signature or an End Of Central Directory
     *        (EOCD) Header, causing this constructor to fail fast if the file
     *        is actually a false positive ZIP compatible file, i.e. not
     *        compatible to the ZIP File Format Specification.
     *        This may be used to read Self Extracting ZIP files (SFX), which
     *        usually contain the application code required for extraction in
     *        a preamble.
     *        This parameter is <code>true</code> by default.
     * @param postambled If this is <code>true</code>, then the ZIP compatible
     *        file may have a postamble of arbitrary length.
     *        Otherwise, the ZIP compatible file must not have a postamble
     *        which exceeds 64KB size, including the End Of Central Directory
     *        record (i.e. including the ZIP file comment), causing this
     *        constructor to fail fast if the file is actually a false positive
     *        ZIP compatible file, i.e. not compatible to the ZIP File Format
     *        Specification.
     *        This may be used to read Self Extracting ZIP files (SFX) with
     *        large postambles.
     *        This parameter is <code>false</code> by default.
     * @throws NullPointerException If <code>file</code> or <code>charset</code> is
     *         <code>null</code>.
     * @throws UnsupportedEncodingException If charset is not supported by
     *         this JVM.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws ZipException If the file is not ZIP compatible.
     * @throws IOException On any other I/O related issue.
     */
    public ZipFile(
            File file,
            String charset,
            boolean preambled,
            boolean postambled)
    throws  NullPointerException,
            UnsupportedEncodingException,
            FileNotFoundException,
            ZipException,
            IOException {
        super(file, charset, preambled, postambled);
    }

    /**
     * Opens the given read only file for reading its ZIP contents,
     * assuming UTF-8 charset for file names.
     * 
     * @param rof The read only file.
     *        Note that this constructor <em>never</em> closes this file.
     * @throws NullPointerException If <code>rof</code> is <code>null</code>.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws ZipException If the file is not ZIP compatible.
     * @throws IOException On any other I/O related issue.
     */
    public ZipFile(ReadOnlyFile rof)
    throws  NullPointerException,
            FileNotFoundException,
            ZipException,
            IOException {
        super(rof);
    }

    /**
     * Opens the given read only file for reading its ZIP contents,
     * assuming the specified charset for file names.
     * 
     * @param rof The read only file.
     *        Note that this constructor <em>never</em> closes this file.
     * @param charset The charset to use for comments and entry names.
     * @throws NullPointerException If <code>rof</code> or <code>charset</code>
     *         is <code>null</code>.
     * @throws UnsupportedEncodingException If charset is not supported by
     *         this JVM.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws ZipException If the file is not ZIP compatible.
     * @throws IOException On any other I/O related issue.
     */
    public ZipFile(ReadOnlyFile rof, String charset)
    throws  NullPointerException,
            UnsupportedEncodingException,
            FileNotFoundException,
            ZipException,
            IOException {
        super(rof, charset);
    }

    /**
     * Opens the given read only file for reading its ZIP contents,
     * assuming the specified charset for file names.
     * 
     * @param rof The read only file.
     *        Note that this constructor <em>never</em> closes this file.
     * @param charset The charset to use for comments and entry names.
     * @param preambled If this is <code>true</code>, then the ZIP compatible
     *        file may have a preamble.
     *        Otherwise, the ZIP compatible file must start with either a
     *        Local File Header (LFH) signature or an End Of Central Directory
     *        (EOCD) Header, causing this constructor to fail fast if the file
     *        is actually a false positive ZIP compatible file, i.e. not
     *        compatible to the ZIP File Format Specification.
     *        This may be used to read Self Extracting ZIP files (SFX), which
     *        usually contain the application code required for extraction in
     *        a preamble.
     *        This parameter is <code>true</code> by default.
     * @param postambled If this is <code>true</code>, then the ZIP compatible
     *        file may have a postamble of arbitrary length.
     *        Otherwise, the ZIP compatible file must not have a postamble
     *        which exceeds 64KB size, including the End Of Central Directory
     *        record (i.e. including the ZIP file comment), causing this
     *        constructor to fail fast if the file is actually a false positive
     *        ZIP compatible file, i.e. not compatible to the ZIP File Format
     *        Specification.
     *        This may be used to read Self Extracting ZIP files (SFX) with
     *        large postambles.
     *        This parameter is <code>false</code> by default.
     * @throws NullPointerException If <code>rof</code> or <code>charset</code>
     *         is <code>null</code>.
     * @throws UnsupportedEncodingException If charset is not supported by
     *         this JVM.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws ZipException If the file is not ZIP compatible.
     * @throws IOException On any other I/O related issue.
     */
    public ZipFile(
            ReadOnlyFile rof,
            String charset,
            boolean preambled,
            boolean postambled)
    throws  NullPointerException,
            UnsupportedEncodingException,
            FileNotFoundException,
            ZipException,
            IOException {
        super(rof, charset, preambled, postambled);
    }

    /** Enumerates clones of all entries in this ZIP file. */
    public synchronized Enumeration entries() {
  return new Enumeration() {
            Enumeration e = ZipFile.super.entries();

            public boolean hasMoreElements() {
    return e.hasMoreElements();
      }

      public Object nextElement() {
    return ((ZipEntry) e.nextElement()).clone();
      }
        };
    }

    /**
     * Returns a clone of the {@link ZipEntry} for the given name or
     * <code>null</code> if no entry with that name exists.
     *
     * @param name Name of the ZIP entry.
     */
    public synchronized ZipEntry getEntry(String name) {
        ZipEntry ze = super.getEntry(name);
        return ze != null ? (ZipEntry) ze.clone() : null;
    }

    public synchronized InputStream getPreambleInputStream() throws IOException {
        return new SynchronizedInputStream(
                super.getPreambleInputStream(),
                this);
    }

    public synchronized InputStream getPostambleInputStream() throws IOException {
        return new SynchronizedInputStream(
                super.getPostambleInputStream(),
                this);
    }

    public synchronized boolean busy() {
        return super.busy();
    }

    protected synchronized InputStream getInputStream(
            final String name,
            final boolean check,
            final boolean inflate)
    throws  IOException {
        return new SynchronizedInputStream(
                super.getInputStream(name, check, inflate),
                this);
    }

    public synchronized void close() throws IOException {
        super.close();
    }
}
