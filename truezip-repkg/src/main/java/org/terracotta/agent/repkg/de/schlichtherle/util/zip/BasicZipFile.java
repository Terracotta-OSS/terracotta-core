/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * Copyright 2006-2007 Schlichtherle IT Services
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
import java.lang.ref.*;
import java.util.*;
import java.util.zip.*;

import org.terracotta.agent.repkg.de.schlichtherle.io.rof.*;

/**
 * <em>This class is <b>not</b> intended for public use!</em>
 * The methods in this class are unsynchronized and
 * {@link #entries}/{@link #getEntry} enumerate/return {@link ZipEntry}
 * instances which are shared with this class rather than clones
 * of them.
 * The class {@link org.terracotta.agent.repkg.de.schlichtherle.io.archive.zip.Zip32InputArchive}
 * extends from this class in order to benefit from the slightly better
 * performance.
 * 
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.4
 * @see ZipFile
 */
public class BasicZipFile {

    private static final long LONG_MSB = 0x8000000000000000L;

    private static final int LFH_FILE_NAME_LENGTH_OFFSET =
        /* local file header signature     */ 4 +
        /* version needed to extract       */ 2 +
        /* general purpose bit flag        */ 2 +
        /* compression method              */ 2 +
        /* last mod file time              */ 2 +
        /* last mod file date              */ 2 +
        /* crc-32                          */ 4 +
        /* compressed size                 */ 4 +
        /* uncompressed size               */ 4;

    private static final int EOCD_NUM_ENTRIES_OFFSET =
        /* end of central dir signature    */ 4 +
        /* number of this disk             */ 2 +
        /* number of the disk with the     */   +
        /* start of the central directory  */ 2 +
        /* total number of entries in      */   +
        /* the central dir on this disk    */ 2;

    private static final int EOCD_CD_SIZE_OFFSET =
            ZIP.EOCD_MIN_LEN - 10;

    private static final int EOCD_CD_LOCATION_OFFSET =
        /* end of central dir signature    */ 4 +
        /* number of this disk             */ 2 +
        /* number of the disk with the     */   +
        /* start of the central directory  */ 2 +
        /* total number of entries in      */   +
        /* the central dir on this disk    */ 2 +
        /* total number of entries in      */   +
        /* the central dir                 */ 2 +
        /* size of the central directory   */ 4;
    
    private static final int EOCD_COMMENT_OFFSET =
            ZIP.EOCD_MIN_LEN - 2;

    private static final Set allocatedInflaters = new HashSet();
    private static final List releasedInflaters = new LinkedList();

    /**
     * The default character set used for entry names and comments in ZIP
     * compatible files.
     * This is {@value} for compatibility with Sun's JDK implementation.
     * Note that you should use &quot;IBM437&quot; for ordinary ZIP files
     * instead.
     */
    public static final String DEFAULT_CHARSET = ZIP.DEFAULT_CHARSET;

    /** The charset to use for entry names and comments. */
    private final String charset;

    /** The comment of this ZIP compatible file. */
    private String comment;

    /** Maps entry names to zip entries. */
    private final Map entries = new LinkedHashMap();

    /** The actual data source. */
    private ReadOnlyFile archive;

    /** The number of open streams reading from this ZIP compatible file. */
    private int openStreams;

    /** The number of bytes in the preamble of this ZIP compatible file. */
    private long preamble;

    /** The number of bytes in the postamble of this ZIP compatible file. */
    private long postamble;

    /** Maps offsets specified in the ZIP file to real offsets in the file. */
    private OffsetMapper mapper;

    /**
     * Opens the given file for reading its ZIP contents,
     * assuming {@value #DEFAULT_CHARSET} charset for file names.
     * 
     * @param name name of the file.
     * @throws NullPointerException If <code>name</code> is <code>null</code>.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws ZipException If the file is not ZIP compatible.
     * @throws IOException On any other I/O related issue.
     */
    public BasicZipFile(String name)
    throws  NullPointerException,
            FileNotFoundException,
            ZipException,
            IOException {
        this.charset = DEFAULT_CHARSET;
        try {
            init(null, new File(name), true, false);
        } catch (UnsupportedEncodingException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
    }

    /**
     * Opens the given file for reading its ZIP contents,
     * assuming the specified charset for file names.
     * 
     * @param name name of the file.
     * @param charset the charset to use for file names
     * @throws NullPointerException If <code>name</code> or <code>charset</code> is
     *         <code>null</code>.
     * @throws UnsupportedEncodingException If charset is not supported by
     *         this JVM.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws ZipException If the file is not ZIP compatible.
     * @throws IOException On any other I/O related issue.
     */
    public BasicZipFile(String name, String charset)
    throws  NullPointerException,
            UnsupportedEncodingException,
            FileNotFoundException,
            ZipException,
            IOException {
        this.charset = charset;
        init(null, new File(name), true, false);
    }

    /**
     * Opens the given file for reading its ZIP contents,
     * assuming the specified charset for file names.
     * 
     * @param name name of the file.
     * @param charset the charset to use for file names
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
    public BasicZipFile(
            String name,
            String charset,
            boolean preambled,
            boolean postambled)
    throws  NullPointerException,
            UnsupportedEncodingException,
            FileNotFoundException,
            ZipException,
            IOException {
        this.charset = charset;
        init(null, new File(name), preambled, postambled);
    }

    /**
     * Opens the given file for reading its ZIP contents,
     * assuming {@value #DEFAULT_CHARSET} charset for file names.
     * 
     * @param file The file.
     * @throws NullPointerException If <code>file</code> is <code>null</code>.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws ZipException If the file is not ZIP compatible.
     * @throws IOException On any other I/O related issue.
     */
    public BasicZipFile(File file)
    throws  NullPointerException,
            FileNotFoundException,
            ZipException,
            IOException {
        this.charset = DEFAULT_CHARSET;
        try {
            init(null, file, true, false);
        } catch (UnsupportedEncodingException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
    }

    /**
     * Opens the given file for reading its ZIP contents,
     * assuming the specified charset for file names.
     * 
     * @param file The file.
     * @param charset The charset to use for entry names and comments
     *        - must <em>not</em> be <code>null</code>!
     * @throws NullPointerException If <code>file</code> or <code>charset</code>
     *         is <code>null</code>.
     * @throws UnsupportedEncodingException If charset is not supported by
     *         this JVM.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws ZipException If the file is not ZIP compatible.
     * @throws IOException On any other I/O related issue.
     */
    public BasicZipFile(File file, String charset)
    throws  NullPointerException,
            UnsupportedEncodingException,
            FileNotFoundException,
            ZipException,
            IOException {
        this.charset = charset;
        init(null, file, true, false);
    }

    /**
     * Opens the given file for reading its ZIP contents,
     * assuming the specified charset for file names.
     * 
     * @param file The file.
     * @param charset The charset to use for entry names and comments
     *        - must <em>not</em> be <code>null</code>!
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
    public BasicZipFile(
            File file,
            String charset,
            boolean preambled,
            boolean postambled)
    throws  NullPointerException,
            UnsupportedEncodingException,
            FileNotFoundException,
            ZipException,
            IOException {
        this.charset = charset;
        init(null, file, preambled, postambled);
    }

    /**
     * Opens the given read only file for reading its ZIP contents,
     * assuming {@value #DEFAULT_CHARSET} charset for file names.
     * 
     * @param rof The read only file.
     *        Note that this constructor <em>never</em> closes this file.
     * @throws NullPointerException If <code>rof</code> is <code>null</code>.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws ZipException If the file is not ZIP compatible.
     * @throws IOException On any other I/O related issue.
     */
    public BasicZipFile(ReadOnlyFile rof)
    throws  NullPointerException,
            FileNotFoundException,
            ZipException,
            IOException {
        this.charset = DEFAULT_CHARSET;
        try {
            init(rof, null, true, false);
        } catch (UnsupportedEncodingException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
    }

    /**
     * Opens the given read only file for reading its ZIP contents,
     * assuming the specified charset for file names.
     * 
     * @param rof The read only file.
     *        Note that this constructor <em>never</em> closes this file.
     * @param charset The charset to use for entry names and comments
     *        - must <em>not</em> be <code>null</code>!
     * @throws NullPointerException If <code>rof</code> or <code>charset</code>
     *         is <code>null</code>.
     * @throws UnsupportedEncodingException If charset is not supported by
     *         this JVM.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws ZipException If the file is not ZIP compatible.
     * @throws IOException On any other I/O related issue.
     */
    public BasicZipFile(ReadOnlyFile rof, String charset)
    throws  NullPointerException,
            UnsupportedEncodingException,
            FileNotFoundException,
            ZipException,
            IOException {
        this.charset = charset;
        init(rof, null, true, false);
    }

    /**
     * Opens the given read only file for reading its ZIP contents,
     * assuming the specified charset for file names.
     * 
     * @param rof The read only file.
     *        Note that this constructor <em>never</em> closes this file.
     * @param charset The charset to use for entry names and comments
     *        - must <em>not</em> be <code>null</code>!
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
    public BasicZipFile(
            ReadOnlyFile rof,
            String charset,
            boolean preambled,
            boolean postambled)
    throws  NullPointerException,
            UnsupportedEncodingException,
            FileNotFoundException,
            ZipException,
            IOException {
        this.charset = charset;
        init(rof, null, preambled, postambled);
    }

    private void init(
            ReadOnlyFile rof,
            final File file,
            final boolean preambled,
            final boolean postambled)
    throws  NullPointerException,
            UnsupportedEncodingException,
            FileNotFoundException,
            ZipException,
            IOException {
        // Check parameters (fail fast).
        if (charset == null)
            throw new NullPointerException("charset");
        new String(new byte[0], charset); // may throw UnsupportedEncodingException!
        if (rof == null) {
            if (file == null)
                throw new NullPointerException();
            rof = createReadOnlyFile(file);
        } else { // rof != null
            assert file == null;
        }
        archive = rof;

        try {
            final BufferedReadOnlyFile brof;
            if (archive instanceof BufferedReadOnlyFile)
                brof = (BufferedReadOnlyFile) archive;
            else
                brof = new BufferedReadOnlyFile(archive);
            mountCentralDirectory(brof, preambled, postambled);
            // Do NOT close brof - would close rof as well!
        } catch (IOException failure) {
            if (file != null)
                rof.close();
            throw failure;
        }
        
        assert mapper != null;
    }

    /**
     * A factory method called by the constructor to get a read only file
     * to access the contents of the ZIP file.
     * This method is only used if the constructor isn't called with a read
     * only file as its parameter.
     * 
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws IOException On any other I/O related issue.
     */
    protected ReadOnlyFile createReadOnlyFile(File file)
    throws FileNotFoundException, IOException {
        return new SimpleReadOnlyFile(file);
    }

    /**
     * Reads the central directory of the given file and populates
     * the internal tables with ZipEntry instances.
     * <p>
     * The ZipEntrys will know all data that can be obtained from
     * the central directory alone, but not the data that requires the
     * local file header or additional data to be read.
     * 
     * @throws ZipException If the file is not ZIP compatible.
     * @throws IOException On any other I/O related issue.
     */
    private void mountCentralDirectory(
            final ReadOnlyFile rof,
            final boolean preambled,
            final boolean postambled)
    throws ZipException, IOException {
        int numEntries = findCentralDirectory(rof, preambled, postambled);
        assert mapper != null;

        preamble = Long.MAX_VALUE;

        final byte[] sig = new byte[4];
        final byte[] cfh = new byte[ZIP.CFH_MIN_LEN - sig.length];
        for (; ; numEntries--) {
            rof.readFully(sig);
            if (readUInt(sig, 0) != ZIP.CFH_SIG)
                break;

            rof.readFully(cfh);
            final int general = readUShort(cfh, 4);
            final int nameLen = readUShort(cfh, 24);
            final byte[] name = new byte[nameLen];
            rof.readFully(name);

            // See appendix D of PKWARE's ZIP File Format Specification.
            final boolean utf8 = (general & (1 << 11)) != 0;
            final String charset = utf8 ? ZIP.UTF8 : this.charset;
            final ZipEntry entry = createZipEntry(new String(name, charset));
            try {
                int off = 0;

                final int versionMadeBy = readUShort(cfh, off);
                off += 2;
                entry.setPlatform((short) (versionMadeBy >> 8));

                off += 2; // version needed to extract

                entry.setGeneral(general);
                off += 2; // general purpose bit flag
                assert entry.getGeneralBit(11) == utf8;

                final int method = readUShort(cfh, off);
                off += 2;
                if (method != ZIP.STORED && method != ZIP.DEFLATED)
                    throw new ZipException(entry.getName()
                    + ": unsupported compression method: " + method);
                entry.setMethod(method);

                entry.setDosTime(readUInt(cfh, off));
                off += 4;

                entry.setCrc(readUInt(cfh, off));
                off += 4;

                entry.setCompressedSize(readUInt(cfh, off));
                off += 4;

                entry.setSize(readUInt(cfh, off));
                off += 4;

                off += 2;   // file name length

                final int extraLen = readUShort(cfh, off);
                off += 2;

                final int commentLen = readUShort(cfh, off);
                off += 2;

                off += 2;   // disk number

                //ze.setInternalAttributes(readUShort(cfh, off));
                off += 2;

                //ze.setExternalAttributes(readUInt(cfh, off));
                off += 4;

                // Local file header offset.
                final long lfhOff = mapper.location(readUInt(cfh, off));

                // Set MSB in entry offset in order to indicate that
                // getInputStream(*) should resolve this.
                // Note that the result can never be -1 as a long value.
                entry.offset = lfhOff | LONG_MSB;
                
                // Update preamble size conditionally.
                if (lfhOff < preamble)
                    preamble = lfhOff;

                if (extraLen > 0) {
                    final byte[] extra = new byte[extraLen];
                    rof.readFully(extra);
                    entry.setExtra(extra);
                }

                if (commentLen > 0) {
                    final byte[] comment = new byte[commentLen];
                    rof.readFully(comment);
                    entry.setComment(new String(comment, charset));
                }
            } catch (IllegalArgumentException incompatibleZipFile) {
                final ZipException exc = new ZipException(entry.getName());
                exc.initCause(incompatibleZipFile);
                throw exc;
            }

            // Map the entry using the name that has been determined
            // by createZipEntry().
            // Note that this name may differ from what has been found
            // in the ZIP file!
            entries.put(entry.getName(), entry);
        }

        // Check if the number of entries found matches the number of entries
        // declared in the End Of Central Directory header.
        // If this is a (possibly negative) multiple of 65536, then the
        // number of entries stored in the ZIP file exceeds the maximum
        // number of 65535 entries supported by the ZIP File Format
        // Specification (a two byte unsigned integer).
        // Although beyond the spec, we silently tolerate this.
        // Thanks to Jean-Francois Thamie for submitting this issue!
        if (numEntries % 65536 != 0)
            throw new ZipException(
                    "expected " +
                    Math.abs(numEntries) +
                    (numEntries > 0 ? " more" : " less") +
                    " entries in the Central Directory");

        if (preamble == Long.MAX_VALUE)
            preamble = 0;
    }

    /**
     * Searches for the &quot;End of central dir record&quot;, parses
     * it and positions the file pointer at the first central directory
     * record.
     * Performs some means to check that this is really a ZIP compatible
     * file.
     * <p>
     * As a side effect, both <code>mapper</code> and </code>postamble</code>
     * will be set.
     * 
     * @throws ZipException If the file is not ZIP compatible.
     * @throws IOException On any other I/O related issue.
     */
    private int findCentralDirectory(
            final ReadOnlyFile rof,
            boolean preambled,
            final boolean postambled)
    throws ZipException, IOException {
        final byte[] sig = new byte[4];
        if (!preambled) {
            rof.seek(0);
            rof.readFully(sig);
            final long signature = readUInt(sig, 0);
            // Constraint: A ZIP file must start with a Local File Header (LFH)
            // or an End Of Central Directory (EOCD) record in case it's emtpy.
            preambled = signature == ZIP.LFH_SIG || signature == ZIP.EOCD_SIG;
        }
        if (preambled) {
            final long length = rof.length();
            final long max = length - ZIP.EOCD_MIN_LEN;
            final long min = !postambled && max >= 0xffff ? max - 0xffff : 0;
            for (long eocdOff = max; eocdOff >= min; eocdOff--) {
                rof.seek(eocdOff);
                rof.readFully(sig);
                if (readUInt(sig, 0) != ZIP.EOCD_SIG)
                    continue;
                
                // Process EOCD.
                final byte[] eocd = new byte[ZIP.EOCD_MIN_LEN - sig.length];
                rof.readFully(eocd);
                final int numEntries = readUShort(eocd, EOCD_NUM_ENTRIES_OFFSET - sig.length);
                final long cdSize = readUInt(eocd, EOCD_CD_SIZE_OFFSET - sig.length);
                final long cdLoc = readUInt(eocd, EOCD_CD_LOCATION_OFFSET - sig.length);
                final int commentLen = readUShort(eocd, EOCD_COMMENT_OFFSET - sig.length);
                if (commentLen > 0) {
                    final byte[] comment = new byte[commentLen];
                    rof.readFully(comment);
                    setComment(new String(comment, charset));
                }
                postamble = length - rof.getFilePointer();
                
                // Seek and check first CFH, probably using an offset mapper.
                long start = eocdOff - cdSize;
                rof.seek(start);
                start -= cdLoc;
                if (start != 0) {
                    mapper = new IrregularOffsetMapper(start);
                } else {
                    mapper = new OffsetMapper();
                }
                
                return numEntries;
            }
        }
        throw new ZipException(
                "expected End Of Central Directory signature");
    }

    /**
     * A factory method returning a newly created ZipEntry for the given name.
     */
    protected ZipEntry createZipEntry(String name) {
        return new ZipEntry(name);
    }

    /**
     * Returns the comment of this ZIP compatible file or <code>null</code>
     * if no comment exists.
     */
    public String getComment() {
        return comment;
    }
    
    private void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Returns <code>true</code> if and only if some input streams are open to
     * read from this ZIP compatible file.
     */
    public boolean busy() {
        return openStreams > 0;
    }

    /** Returns the charset to use for entry names and comments. */
    public String getCharset() {
        return charset;
    }

    /** @deprecated Use {@link #getCharset} instead. */
    public String getEncoding() {
        return getCharset();
    }

    /**
     * Returns an enumeration of the ZIP entries in this ZIP file.
     * Note that the enumerated entries are shared with this class.
     * It is illegal to change their state!
     */
    public Enumeration entries() {
        return Collections.enumeration(entries.values());
    }

    /**
     * Returns the {@link ZipEntry} for the given name or
     * <code>null</code> if no entry with that name exists.
     * Note that the returned entry is shared with this class.
     * It is illegal to change its state!
     *
     * @param name Name of the ZIP entry.
     */
    public ZipEntry getEntry(String name) {
        return (ZipEntry) entries.get(name);
    }

    /**
     * Returns the number of entries in this ZIP compatible file.
     */
    public int size() {
  return entries.size();
    }

    /**
     * Returns the file length of this ZIP compatible file in bytes.
     */
    public long length() throws IOException {
        ensureOpen();
        return archive.length();
    }

    /**
     * Returns the length of the preamble of this ZIP compatible file in bytes.
     *
     * @return A positive value or zero to indicate that this ZIP compatible
     *         file does not have a preamble.
     *
     * @since TrueZIP 5.1
     */
    public long getPreambleLength() {
        return preamble;
    }
    
    /**
     * Returns an {@link InputStream} to read the preamble of this ZIP
     * compatible file.
     * <p>
     * Note that the returned stream is a <i>lightweight</i> stream,
     * i.e. there is no external resource such as a {@link ReadOnlyFile}
     * allocated for it. Instead, all streams returned by this method share
     * the underlying <code>ReadOnlyFile</code> of this <code>ZipFile</code>.
     * This allows to close this object (and hence the underlying
     * <code>ReadOnlyFile</code>) without cooperation of the returned
     * streams, which is important if the application wants to work on the
     * underlying file again (e.g. update or delete it).
     *
     * @since TrueZIP 5.1
     * @throws ZipException If this ZIP file has been closed.
     */
    public InputStream getPreambleInputStream() throws IOException {
        ensureOpen();
        return new IntervalInputStream(0, preamble);
    }

    /**
     * Returns the length of the postamble of this ZIP compatible file in bytes.
     *
     * @return A positive value or zero to indicate that this ZIP compatible
     *         file does not have an postamble.
     *
     * @since TrueZIP 5.1
     */
    public long getPostambleLength() {
        return postamble;
    }
    
    /**
     * Returns an {@link InputStream} to read the postamble of this ZIP
     * compatible file.
     * <p>
     * Note that the returned stream is a <i>lightweight</i> stream,
     * i.e. there is no external resource such as a {@link ReadOnlyFile}
     * allocated for it. Instead, all streams returned by this method share
     * the underlying <code>ReadOnlyFile</code> of this <code>ZipFile</code>.
     * This allows to close this object (and hence the underlying
     * <code>ReadOnlyFile</code>) without cooperation of the returned
     * streams, which is important if the application wants to work on the
     * underlying file again (e.g. update or delete it).
     *
     * @since TrueZIP 5.1
     * @throws ZipException If this ZIP file has been closed.
     */
    public InputStream getPostambleInputStream() throws IOException {
        ensureOpen();
        return new IntervalInputStream(archive.length() - postamble, postamble);
    }

    /**
     * Returns <code>true</code> if and only if the offsets in this ZIP file
     * are relative to the start of the file, rather than the first Local
     * File Header.
     * <p>
     * This method is intended for very special purposes only.
     */
    public boolean offsetsConsiderPreamble() {
        assert mapper != null;
        return mapper.location(0) == 0;
    }
    
    /**
     * Equivalent to {@link #getInputStream(String, boolean, boolean)
     * getInputStream(name, false, true)}.
     */
    public final InputStream getInputStream(String name)
    throws IOException {
        return getInputStream(name, false, true);
    }

    /**
     * Equivalent to {@link #getInputStream(String, boolean, boolean)
     * getInputStream(entry.getName(), false, true)} instead.
     */
    public final InputStream getInputStream(ZipEntry entry)
    throws IOException {
        return getInputStream(entry.getName(), false, true);
    }

    /**
     * Equivalent to {@link #getInputStream(String, boolean, boolean)
     * getInputStream(name, true, true)}.
     */
    public final InputStream getCheckedInputStream(String name)
    throws IOException {
        return getInputStream(name, true, true);
    }

    /**
     * Equivalent to {@link #getInputStream(String, boolean, boolean)
     * getInputStream(entry.getName(), true, true)} instead.
     */
    public final InputStream getCheckedInputStream(ZipEntry entry)
    throws IOException {
        return getInputStream(entry.getName(), true, true);
    }

    /** @deprecated */
    public InputStream getInputStream(String name, boolean inflate)
    throws  IOException {
        return getInputStream(name, false, inflate);
    }

    /** @deprecated */
    public final InputStream getInputStream(ZipEntry entry, boolean inflate)
    throws IOException {
        return getInputStream(entry.getName(), false, inflate);
    }

    /**
     * Returns an <code>InputStream</code> for reading the inflated or
     * deflated data of the given entry.
     * <p>
     * If the {@link #close} method is called on this instance, all input
     * streams returned by this method are closed, too.
     *
     * @param name The name of the entry to get the stream for
     *        - may <em>not</em> be <code>null</code>!
     * @param check Whether or not the entry's CRC-32 value is checked.
     *        If and only if this parameter is true, two additional checks are
     *        performed for the ZIP entry:
     *        <ol>
     *        <li>All entry headers are checked to have consistent declarations
     *            of the CRC-32 value for the inflated entry data.
     *        <li>When calling {@link InputStream#close} on the returned entry
     *            stream, the CRC-32 value computed from the inflated entry
     *            data is checked against the declared CRC-32 values.
     *            This is independent from the <code>inflate</code> parameter.
     *        </ol>
     *        If any of these checks fail, a {@link CRC32Exception} is thrown.
     *        <p>
     *        This parameter should be <code>false</code> for most
     *        applications, and is the default for the sibling of this class
     *        in {@link java.util.zip.ZipFile java.util.zip.ZipFile}.
     * @param inflate Whether or not the entry data should be inflated.
     *        If <code>false</code>, the entry data is not inflated,
     *        even if the entry data is deflated.
     *        This parameter should be <code>true</code> for most applications.
     * @return A stream to read the entry data from or <code>null</code> if the
     *         entry does not exist.
     * @throws NullPointerException If <code>name</code> is <code>null</code>.
     * @throws CRC32Exception If the declared CRC-32 values of the inflated
     *         entry data are inconsistent across the entry headers.
     * @throws ZipException If this file is not compatible to the ZIP File
     *         Format Specification.
     * @throws IOException If the entry cannot get read from this ZipFile.
     * @since TrueZIP 6.4
     */
    protected InputStream getInputStream(
            final String name,
            final boolean check,
            final boolean inflate)
    throws IOException {
        ensureOpen();
        if (name == null)
            throw new NullPointerException();

        final ZipEntry entry = (ZipEntry) entries.get(name);
        if (entry == null)
            return null;

        long offset = entry.offset;
        assert offset != -1;
        if (offset < 0) {
            // This offset has been set by mountCentralDirectory()
            // and needs to be resolved first.
            offset &= ~LONG_MSB; // Switch off MSB.
            archive.seek(offset);
            final byte[] lfh = new byte[ZIP.LFH_MIN_LEN];
            archive.readFully(lfh);
            final long lfhSig = readUInt(lfh, 0);
            if (lfhSig != ZIP.LFH_SIG)
                throw new ZipException(name
                + ": expected Local File Header signature");
            offset += ZIP.LFH_MIN_LEN
                    + readUShort(lfh, LFH_FILE_NAME_LENGTH_OFFSET) // file name length
                    + readUShort(lfh, LFH_FILE_NAME_LENGTH_OFFSET + 2); // extra field length

            if (check) {
                // Check CRC-32 in the Local File Header or Data Descriptor.
                final long localCrc;
                if (entry.getGeneralBit(3)) {
                    // The CRC-32 is in the Data Descriptor after the compressed
                    // size.
                    // Note the Data Descriptor's Signature is optional:
                    // All newer apps should write it (and so does ZIP.RAES),
                    // but older apps might not.
                    final byte[] dd = new byte[8];
                    archive.seek(offset + entry.getCompressedSize());
                    archive.readFully(dd);
                    final long ddSig = readUInt(dd, 0);
                    localCrc = ddSig == ZIP.DD_SIG ? readUInt(dd, 4) : ddSig;
                } else {
                    // The CRC-32 is in the Local File Header.
                    localCrc = readUInt(lfh, 14);
                }
                if (entry.getCrc() != localCrc)
                    throw new CRC32Exception(name, entry.getCrc(), localCrc);
            }

            entry.offset = offset;
        }

        final IntervalInputStream iis
                = new IntervalInputStream(offset, entry.getCompressedSize());
        final int bufSize = getBufferSize(entry);
        InputStream in = iis;
        switch (entry.getMethod()) {
            case ZIP.DEFLATED:
                if (inflate) {
                    iis.addDummy();
                    in = new PooledInflaterInputStream(in, bufSize);
                    if (check)
                        in = new CheckedInputStream(in, entry, bufSize);
                    break;
                } else {
                    if (check)
                        in = new RawCheckedInputStream(in, entry, bufSize);
                }
                break;

            case ZIP.STORED:
                if (check)
                    in = new CheckedInputStream(in, entry, bufSize);
                break;

            default:
                assert false : "this should already have been checked by mountCentralDirectory()";
        }

        return in;
    }

    private static final int getBufferSize(final ZipEntry entry) {
        long size = entry.getSize();
        if (size > ZIP.FLATER_BUF_LENGTH)
            size = ZIP.FLATER_BUF_LENGTH;
        else if (size < ZIP.FLATER_BUF_LENGTH / 8)
            size = ZIP.FLATER_BUF_LENGTH / 8;
        return (int) size;
    }

    /**
     * Ensures that this archive is still open.
     */
    private final void ensureOpen() throws ZipException {
        if (archive == null)
            throw new ZipException("ZIP file has been closed");
    }

    private static final class PooledInflaterInputStream
            extends InflaterInputStream {
        private boolean closed;

        public PooledInflaterInputStream(InputStream in, int size) {
            super(in, allocateInflater(), size);
        }

        public void close() throws IOException {
            if (closed)
                return;

            closed = true;
            try {
                super.close();
            } finally {
                releaseInflater(inf);
            }
        }
    } // class PooledInflaterInputStream

    private static Inflater allocateInflater() {
        Inflater inflater = null;

        synchronized (releasedInflaters) {
            for (Iterator i = releasedInflaters.iterator(); i.hasNext(); ) {
                inflater = (Inflater) ((Reference) i.next()).get();
                i.remove();
                if (inflater != null) {
                    //inflater.reset();
                    break;
                }
            }
            if (inflater == null)
                inflater = new Inflater(true);

            // We MUST make sure that we keep a strong reference to the
            // inflater in order to retain it from being released again and
            // then finalized when the close() method of the InputStream
            // returned by getInputStream(...) is called from within another
            // finalizer.
            // The finalizer of the inflater calls end() and leaves the object
            // in a state so that the subsequent call to reset() throws an NPE.
            // The ZipFile class in Sun's J2SE 1.4.2 shows this bug.
            allocatedInflaters.add(inflater);
        }
        
        return inflater;
    }

    private static void releaseInflater(Inflater inflater) {
        inflater.reset();
        synchronized (releasedInflaters) {
            releasedInflaters.add(new SoftReference(inflater));
            allocatedInflaters.remove(inflater);
        }
    }

    private static final class CheckedInputStream
            extends java.util.zip.CheckedInputStream {
        private final ZipEntry entry;
        private final int size;

        public CheckedInputStream(
                final InputStream in,
                final ZipEntry entry,
                final int size) {
            super(in, new CRC32());
            this.entry = entry;
            this.size = size;
        }

        public long skip(long toSkip) throws IOException {
            return skipWithBuffer(this, toSkip, new byte[size]);
        }

        public void close() throws IOException {
            try {
                while (skip(Long.MAX_VALUE) > 0) // process CRC-32 until EOF - this version makes FindBugs happy!
                    ; 
            } finally {
                super.close();
            }
            final long expectedCrc = entry.getCrc();
            final long actualCrc = getChecksum().getValue();
            if (expectedCrc != actualCrc)
                throw new CRC32Exception(
                        entry.getName(), expectedCrc, actualCrc);
        }
    } // class CheckedInputStream

    /**
     * This method skips <code>toSkip</code> bytes in the given input stream
     * using the given buffer unless EOF or IOException.
     */
    private static long skipWithBuffer(
            final InputStream in,
            final long toSkip,
            final byte[] buf)
    throws IOException {
        long total = 0;
        for (long len; (len = toSkip - total) > 0; total += len) {
            len = in.read(buf, 0, len < buf.length ? (int) len : buf.length);
            if (len < 0)
                break;
        }
        return total;
    }

    /**
     * An stream which reads and returns deflated data from its input
     * while a CRC-32 checksum is computed over the inflated data and
     * checked in the close() method.
     */
    private static final class RawCheckedInputStream extends FilterInputStream {

        private final Checksum crc = new CRC32();
        private final byte[] singleByteBuf = new byte[1];
        private final Inflater inf;
        private final byte[] infBuf; // contains inflated data!
        private final ZipEntry entry;
        private boolean closed;

        public RawCheckedInputStream(
                final InputStream in,
                final ZipEntry entry,
                final int size) {
            super(in);
            this.inf = allocateInflater();
            this.infBuf = new byte[size];
            this.entry = entry;
        }

        private void ensureOpen()
        throws IOException {
            if (closed)
                throw new IOException("input stream has been closed");
        }

        public int read()
        throws IOException {
            int read;
            while ((read = read(singleByteBuf, 0, 1)) == 0) // reading nothing is not acceptible!
                ;
            return read > 0 ? singleByteBuf[0] & 0xff : -1;
        }

        public int read(final byte[] buf, final int off, final int len)
        throws IOException {
            if (len == 0)
                return 0; // be fault-tolerant and compatible to FileInputStream

            // Check state.
            ensureOpen();

            // Check parameters.
            if (buf == null)
                throw new NullPointerException();
            final int offPlusLen = off + len;
            if ((off | len | offPlusLen | buf.length - offPlusLen) < 0)
                throw new IndexOutOfBoundsException();

            // Read data.
            final int read = in.read(buf, off, len);
            
            // Feed inflater.
            if (read >= 0) {
                inf.setInput(buf, off, read);
            } else {
                buf[off] = 0;
                inf.setInput(buf, off, 1); // provide dummy byte
            }

            // Inflate and update checksum.
            try {
                int inflated;
                while ((inflated = inf.inflate(infBuf, 0, infBuf.length)) > 0)
                    crc.update(infBuf, 0, inflated);
            } catch (DataFormatException dfe) {
                IOException ioe = new IOException(dfe.toString());
                ioe.initCause(dfe);
                throw ioe;
            }

            // Check inflater invariants.
            assert read >= 0 || inf.finished();
            assert read <  0 || inf.needsInput();
            assert !inf.needsDictionary();

            return read;
        }

        public long skip(long toSkip) throws IOException {
            return skipWithBuffer(this, toSkip, new byte[infBuf.length]);
        }

        public void close() throws IOException {
            if (closed)
                return;

            // Order is important!
            try {
                while (skip(Long.MAX_VALUE) > 0) // process CRC-32 until EOF - this version makes FindBugs happy!
                    ; 
            } finally {
                closed = true;
                releaseInflater(inf);
                super.close();
            }

            long expectedCrc = entry.getCrc();
            long actualCrc = crc.getValue();
            if (expectedCrc != actualCrc)
                throw new CRC32Exception(
                        entry.getName(), expectedCrc, actualCrc);
        }

        public void mark(int readlimit) {
        }

        public void reset()
        throws IOException {
            throw new IOException("mark()/reset() not supported");
        }

        public boolean markSupported() {
            return false;
        }
    } // class RawCheckedInputStream

    /**
     * Closes the file.
     * This closes any open input streams reading from this ZIP file.
     * 
     * @throws IOException if an error occurs closing the file.
     */
    public void close() throws IOException {
        // Order is important here!
        if (archive != null) {
            final ReadOnlyFile oldArchive = archive;
            archive = null;
            oldArchive.close();
        }
    }

    private static final int readUShort(final byte[] b, final int off) {
        return ((b[off + 1] & 0xff) << 8) | (b[off] & 0xff);
    }
    
    private static final long readUInt(final byte[] b, int off) {
        off += 3;
        long v = b[off--] & 0xffL;
        v <<= 8;
        v |= b[off--] & 0xffL;
        v <<= 8;
        v |= b[off--] & 0xffL;
        v <<= 8;
        v |= b[off] & 0xffL;
        return v;
    }

    /**
     * InputStream that delegates requests to the underlying
     * RandomAccessFile, making sure that only bytes from a certain
     * range can be read.
     * This design of this class makes the ZipFile class thread safe,
     * i.e. multiple threads may safely retrieve individual InputStreams.
     * It also allows to call close() on the ZipFile, thereby closing all
     * input streams reading from it, which is important in the context of
     * TrueZIP's high level API.
     */
    private class IntervalInputStream extends AccountedInputStream {
        private long remaining;
        private long fp;
        private boolean addDummyByte;

        /**
         * @param start The start address (not offset) in <code>archive</code>.
         * @param remaining The remaining bytes allowed to be read in
         *        <code>archive</code>.
         */
        IntervalInputStream(long start, long remaining) {
            assert start >= 0;
            assert remaining >= 0;
            this.remaining = remaining;
            fp = start;
        }

        public int read()
        throws IOException {
            ensureOpen();

            if (remaining <= 0) {
                if (addDummyByte) {
                    addDummyByte = false;
                    return 0;
                }

                return -1;
            }

            archive.seek(fp);
            final int ret = archive.read();
            if (ret >= 0) {
                fp++;
                remaining--;
            }

            return ret;
        }

        public int read(final byte[] b, final int off, int len)
        throws IOException {
            if (len <= 0) {
                if (len < 0)
                    throw new IndexOutOfBoundsException();
                return 0;
            }

            ensureOpen();
            
            if (remaining <= 0) {
                if (addDummyByte) {
                    addDummyByte = false;
                    b[off] = 0;
                    return 1;
                }

                return -1;
            }

            if (len > remaining)
                len = (int) remaining;

            archive.seek(fp);
            final int ret = archive.read(b, off, len);
            if (ret > 0) {
                fp += ret;
                remaining -= ret;
            }

            return ret;
        }

        /**
         * Inflater needs an extra dummy byte for nowrap - see
         * Inflater's javadocs.
         */
        void addDummy() {
            addDummyByte = true;
        }

        /**
         * @return The number of bytes remaining in this entry, yet maximum
         *         <code>Integer.MAX_VALUE</code>.
         *         Note that this is only relevant for entries which have been
         *         stored with the <code>STORED</code> method.
         *         For entries stored according to the <code>DEFLATED</code>
         *         method, the value returned by this method on the
         *         <code>InputStream</code> returned by {@link #getInputStream}
         *         is actually determined by an {@link InflaterInputStream}.
         */
        public int available()
        throws IOException {
            ensureOpen();

            long available = remaining;
            if (addDummyByte)
                available++;
            return available > Integer.MAX_VALUE
                    ? Integer.MAX_VALUE
                    : (int) available;
        }
    } // class BoundedInputStream

    private abstract class AccountedInputStream extends InputStream {
        private boolean closed;

        public AccountedInputStream() {
            openStreams++;
        }

        public void close() throws IOException {
            // Order is important here!
            if (!closed) {
                closed = true;
                openStreams--;
                super.close();
            }
        }

        protected void finalize() throws Throwable {
            try {
                close();
            } finally {
                super.finalize();
            }
        }
    };

    private static class OffsetMapper {
        long location(long offset) {
            return offset;
        }
    }
    
    private static class IrregularOffsetMapper extends OffsetMapper {
        final long start;

        IrregularOffsetMapper(long start) {
            this.start = start;
        }

        long location(long offset) {
            return start + offset;
        }
    }
}
