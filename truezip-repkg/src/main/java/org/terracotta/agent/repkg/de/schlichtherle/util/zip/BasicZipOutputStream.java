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
import java.util.*;
import java.util.zip.*;

import org.terracotta.agent.repkg.de.schlichtherle.io.util.*;

/**
 * <em>This class is <b>not</b> intended for public use!</em>
 * The methods in this class are unsynchronized and
 * {@link #entries}/{@link #getEntry} enumerate/return {@link ZipEntry}
 * instances which are shared with this class rather than clones of them.
 * The class {@link org.terracotta.agent.repkg.de.schlichtherle.io.archive.zip.Zip32OutputArchive}
 * extends from this class in order to benefit from the slightly better
 * performance.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.4
 * @see ZipOutputStream
 */
public class BasicZipOutputStream extends FilterOutputStream {

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

    /** CRC instance to avoid parsing DEFLATED data twice. */
    private final CRC32 crc = new CRC32();

    /** This instance is used for deflated output. */
    private final ZipDeflater def = new ZipDeflater();

    /** This buffer holds deflated data for output. */
    private final byte[] dbuf = new byte[ZIP.FLATER_BUF_LENGTH];

    private final byte[] sbuf = new byte[1];

    /** The file comment. */
    private String comment = "";

    /** Default compression method for next entry. */
    private short method = ZIP.DEFLATED;

    /**
     * The list of ZIP entries started to be written so far.
     * Maps entry names to zip entries.
     */
    private final Map entries = new LinkedHashMap();

    /** Start of entry data. */
    private long dataStart;

    /** Start of central directory. */
    private long cdOffset;

    /** Length of central directory. */
    private long cdLength;

    private boolean finished;

    private boolean closed;

    /** Current entry. */
    private ZipEntry entry;
    
    /**
     * Whether or not we need to deflate the current entry.
     * This can be used together with the <code>DEFLATED</code> method to
     * write already compressed entry data into the ZIP file.
     */
    private boolean deflate;

    /**
     * Creates a new ZIP output stream decorating the given output stream,
     * using the {@value #DEFAULT_CHARSET} charset.
     *
     * @throws NullPointerException If <code>out</code> is <code>null</code>.
     */
    public BasicZipOutputStream(final OutputStream out)
    throws NullPointerException {
        super(toLEDataOutputStream(out));

        // Check parameters (fail fast).
        if (out == null)
            throw new NullPointerException();

        this.charset = DEFAULT_CHARSET;
    }

    /**
     * Creates a new ZIP output stream decorating the given output stream.
     *
     * @throws NullPointerException If <code>out</code> or <code>charset</code> is
     *         <code>null</code>.
     * @throws UnsupportedEncodingException If charset is not supported by
     *         this JVM.
     */
    public BasicZipOutputStream(
            final OutputStream out,
            final String charset)
    throws  NullPointerException,
            UnsupportedEncodingException {
        super(toLEDataOutputStream(out));

        // Check parameters (fail fast).
        if (out == null || charset == null)
            throw new NullPointerException();
        "".getBytes(charset); // may throw UnsupportedEncodingException!

        this.charset = charset;
    }

    private static LEDataOutputStream toLEDataOutputStream(OutputStream out) {
        return out instanceof LEDataOutputStream
                ? (LEDataOutputStream) out
                : new LEDataOutputStream(out);
    }

    /**
     * Returns the charset to use for filenames and the file comment.
     */
    public String getEncoding() {
        return charset;
    }

    /**
     * Returns the number of ZIP entries written so far.
     */
    public int size() {
        return entries.size();
    }

    /**
     * Returns an enumeration of the ZIP entries written so far.
     * Note that the enumerated entries are shared with this class.
     * It is illegal to put more entries into this ZIP output stream
     * concurrently or modify the state of the enumerated entries.
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

    public String getComment() {
        return comment;
    }

    /**
     * Set the file comment.
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Returns the compression level currently used.
     */
    public int getLevel() {
        return def.getLevel();
    }
    
    /**
     * Sets the compression level for subsequent entries.
     */
    public void setLevel(int level) {
  def.setLevel(level);
    }

    public int getMethod() {
        return method;
    }

    /**
     * Sets the default compression method for subsequent entries.
     * 
     * <p>Default is DEFLATED.</p>
     */
    public void setMethod(int method) {
  if (method != ZIP.STORED && method != ZIP.DEFLATED)
      throw new IllegalArgumentException("invalid compression method");
        this.method = (short) method;
    }

    /**
     * Returns the total number of (compressed) bytes this stream has written
     * to the underlying stream.
     */
    public long length() {
        return ((LEDataOutputStream) out).size();
    }

    /**
     * Returns <code>true</code> if and only if this
     * <code>BasicZipOutputStream</code> is currently writing a ZIP entry.
     */
    public boolean isBusy() {
        return entry != null;
    }

    /**
     * Equivalent to
     * {@link #putNextEntry(ZipEntry, boolean) putNextEntry(entry, true)}.
     */
    public final void putNextEntry(final ZipEntry entry)
    throws IOException {
        putNextEntry(entry, true);
    }
    
    /**
     * Starts writing the next ZIP entry to the underlying stream.
     * Note that if two or more entries with the same name are written
     * consecutively to this stream, the last entry written will shadow
     * all other entries, i.e. all of them are written to the ZIP compatible
     * file (and hence require space), but only the last will be accessible
     * from the central directory.
     * This is unlike the genuine {@link java.util.zip.ZipOutputStream
     * java.util.zip.ZipOutputStream} which would throw a {@link ZipException}
     * in this method when the second entry with the same name is to be written.
     * 
     * @param entry The ZIP entry to write.
     * @param deflate Whether or not the entry data should be deflated.
     *        Use this to directly write already deflated data only!
     * @throws ZipException If and only if writing the entry is impossible
     *         because the resulting file would not comply to the ZIP file
     *         format specification.
     * @throws IOException On any I/O related issue.
     */
    public void putNextEntry(final ZipEntry entry, final boolean deflate)
    throws IOException {
        closeEntry();

        final String name = entry.getName();
        /*if (entries.get(name) != null)
            throw new ZipException(name + " (duplicate entry)");*/

        {
            final long size = entry.getNameLength(charset)
                            + entry.getExtraLength()
                            + entry.getCommentLength(charset);
            if (size > 0xFFFF)
                throw new ZipException(entry.getName()
                + ": sum of name, extra fields and comment too long: " + size);
        }

        int method = entry.getMethod();
        if (method == ZipEntry.UNKNOWN)
            method = getMethod();
        switch (method) {
            case ZIP.STORED:
                checkLocalFileHeaderData(entry);
                this.deflate = false;
                break;
                
            case ZIP.DEFLATED:
                if (!deflate)
                    checkLocalFileHeaderData(entry);
                this.deflate = deflate;
                break;
                
            default:
                throw new ZipException(entry.getName()
                + ": unsupported compression method: " + method);
        }

        if (entry.getPlatform() == ZipEntry.UNKNOWN)
            entry.setPlatform(ZIP.PLATFORM_FAT);
        if (entry.getMethod()   == ZipEntry.UNKNOWN)
            entry.setMethod(method);
        if (entry.getTime()     == ZipEntry.UNKNOWN)
            entry.setTime(System.currentTimeMillis());

        // Write LFH BEFORE putting the entry in the map.
        this.entry = entry;
        writeLocalFileHeader();

        // Store entry now so that an immediate subsequent call to getEntry(...)
        // returns it.
        final ZipEntry old = (ZipEntry) entries.put(name, entry);
        assert old == null;
    }

    private static void checkLocalFileHeaderData(final ZipEntry entry)
    throws ZipException {
        if (entry.getCrc()            == ZipEntry.UNKNOWN)
            throw new ZipException("unknown CRC checksum");
        if (entry.getCompressedSize() == ZipEntry.UNKNOWN)
            throw new ZipException("unknown compressed size");
        if (entry.getSize()           == ZipEntry.UNKNOWN)
            throw new ZipException("unknown size");
    }

    /** @throws IOException On any I/O related issue. */
    private void writeLocalFileHeader() throws IOException {
        assert entry != null;

        // Start changes.
        final ZipEntry entry = this.entry;
        final LEDataOutputStream dos = (LEDataOutputStream) out;
        final long offset = dos.size();
        finished = false;

        // Local File Header signature.
        dos.writeInt(ZIP.LFH_SIG);

        final boolean dataDescriptor
                =  entry.getCrc()            == ZipEntry.UNKNOWN
                || entry.getCompressedSize() == ZipEntry.UNKNOWN
                || entry.getSize()           == ZipEntry.UNKNOWN;

        // Compose General Purpose Bit Flag.
        // See appendix D of PKWARE's ZIP File Format Specification.
        final boolean utf8 = ZIP.UTF8.equalsIgnoreCase(charset);
        final int general = (dataDescriptor ? (1 <<  3) : 0)
                          | (utf8           ? (1 << 11) : 0);

        // Version needed to extract.
        dos.writeShort(dataDescriptor ? 20 : 10);

        // General purpose bit flag.
        dos.writeShort(general);

        // Compression method.
        dos.writeShort(entry.getMethod());

        // Last modification time and date in DOS format.
        dos.writeInt((int) entry.getDosTime());

        // CRC32.
        // Compressed length.
        // Uncompressed length.
        if (dataDescriptor) {
            dos.writeInt(0);
            dos.writeInt(0);
            dos.writeInt(0);
        } else {
            dos.writeInt((int) entry.getCrc());
            dos.writeInt((int) entry.getCompressedSize());
            dos.writeInt((int) entry.getSize());
        }

        // File name length.
        final byte[] name = entry.getName().getBytes(charset);
        dos.writeShort(name.length);

        // Extra field length.
        byte[] extra = entry.getExtra();
        if (extra == null)
            extra = new byte[0];
        dos.writeShort(extra.length);

        // File name.
        dos.write(name);

        // Extra field.
        dos.write(extra);

        // Commit changes.
        entry.setGeneral(general);
        entry.offset = offset;
        dataStart = dos.size();
    }

    /**
     * @throws IOException On any I/O related issue.
     */
    public void write(int b) throws IOException {
        byte[] buf = sbuf;
        buf[0] = (byte) b;
        write(buf, 0, 1);
    }

    /**
     * @throws IOException On any I/O related issue.
     */
    public void write(final byte[] b, final int off, final int len)
    throws IOException {
        if (entry != null) {
            if (len == 0) // let negative values pass for an exception
                return;
            if (deflate) {
                // Fast implementation.
                assert !def.finished();
                def.setInput(b, off, len);
                while (!def.needsInput())
                    deflate();
                crc.update(b, off, len);
            } else {
                out.write(b, off, len);
                if (entry.getMethod() != ZIP.DEFLATED)
                    crc.update(b, off, len);
            }
        } else {
            out.write(b, off, len);
        }
    }

    private final void deflate() throws IOException {
        final int dlen = def.deflate(dbuf, 0, dbuf.length);
        if (dlen > 0)
            out.write(dbuf, 0, dlen);
    }

    /**
     * Writes all necessary data for this entry to the underlying stream.
     *
     * @throws ZipException If and only if writing the entry is impossible
     *         because the resulting file would not comply to the ZIP file
     *         format specification.
     * @throws IOException On any I/O related issue.
     */
    public void closeEntry() throws IOException {
        if (entry == null)
            return;

        switch (entry.getMethod()) {
            case ZIP.STORED:
                final long expectedCrc = crc.getValue();
                if (expectedCrc != entry.getCrc()) {
                    throw new ZipException(entry.getName()
                    + ": bad entry CRC-32: "
                    + Long.toHexString(entry.getCrc())
                    + " expected: "
                    + Long.toHexString(expectedCrc));
                }
                final long written = ((LEDataOutputStream) out).size();
                if (entry.getSize() != written - dataStart) {
                    throw new ZipException(entry.getName()
                    + ": bad entry size: "
                    + entry.getSize()
                    + " expected: "
                    + (written - dataStart));
                }
                break;
                
            case ZIP.DEFLATED:
                if (deflate) {
                    assert !def.finished();
                    def.finish();
                    while (!def.finished())
                        deflate();

                    entry.setCrc(crc.getValue());
                    entry.setCompressedSize(def.getTotalOut() & 0xFFFFFFFFl);
                    entry.setSize(def.getTotalIn() & 0xFFFFFFFFl);

                    def.reset();
                } else {
                    // Note: There is no way to check whether the written
                    // data matches the crc, the compressed size and the
                    // uncompressed size!
                }
                break;
                
            default:
                throw new ZipException(entry.getName()
                + ": unsupported compression method: "
                + entry.getMethod());
        }

        writeDataDescriptor();
        flush();
        crc.reset();
        entry = null;
    }

    /**
     * @throws IOException On any I/O related issue.
     */
    private void writeDataDescriptor() throws IOException {
        final ZipEntry entry = this.entry;
        assert entry != null;

        if (!entry.getGeneralBit(3))
            return;

        final LEDataOutputStream dos = (LEDataOutputStream) out;

        dos.writeInt(ZIP.DD_SIG);
        dos.writeInt((int) entry.getCrc());
        dos.writeInt((int) entry.getCompressedSize());
        dos.writeInt((int) entry.getSize());
    }

    /**
     * Closes the current entry and writes the central directory to the
     * underlying output stream.
     * <p>
     * <b>Notes:</b>
     * <ul>
     * <li>The underlying stream is not closed.</li>
     * <li>Unlike Sun's implementation in J2SE 1.4.2, you may continue to use
     *     this ZIP output stream with putNextEntry(...) and the like.
     *     When you finally close the stream, the central directory will
     *     contain <em>all</em> entries written.</li>
     * </ul>
     *
     * @throws ZipException If and only if writing the entry is impossible
     *         because the resulting file would not comply to the ZIP file
     *         format specification.
     * @throws IOException On any I/O related issue.
     */
    public void finish() throws IOException {
        if (finished)
            return;

        // Order is important here!
        finished = true;
        closeEntry();
        final LEDataOutputStream dos = (LEDataOutputStream) out;
        cdOffset = dos.size();
        for (final Iterator i = entries.values().iterator(); i.hasNext(); )
            writeCentralFileHeader((ZipEntry) i.next());
        cdLength = dos.size() - cdOffset;
        writeEndOfCentralDirectory();
    }

    /**
     * Writes the central file header entry
     *
     * @throws IOException On any I/O related issue.
     */
    private void writeCentralFileHeader(final ZipEntry ze) throws IOException {
        assert ze != null;

        final LEDataOutputStream dos = (LEDataOutputStream) out;

        dos.writeInt(ZIP.CFH_SIG);

        // Version made by.
        dos.writeShort((ze.getPlatform() << 8) | 63);

        // Version needed to extract.
        dos.writeShort(ze.getGeneralBit(3) ? 20 : 10);

        // General purpose bit flag.
        dos.writeShort(ze.getGeneral());

        // Compression method.
        dos.writeShort(ze.getMethod());

        // Last mod. time and date.
        dos.writeInt((int) ze.getDosTime());

        // CRC.
        // Compressed length.
        // Uncompressed length.
        dos.writeInt((int) ze.getCrc());
        dos.writeInt((int) ze.getCompressedSize());
        dos.writeInt((int) ze.getSize());

        // File name length.
        final byte[] name = ze.getName().getBytes(charset);
        dos.writeShort(name.length);

        // Extra field length.
        byte[] extra = ze.getExtra();
        if (extra == null)
            extra = new byte[0];
        dos.writeShort(extra.length);

        // File comment length.
        String comment = ze.getComment();
        if (comment == null)
            comment = "";
        final byte[] data = comment.getBytes(charset);
        dos.writeShort(data.length);

        // Disk number start.
        dos.writeShort(0);

        // Internal file attributes.
        dos.writeShort(0);

        // External file attributes.
        dos.writeInt(0);

        // Offset of local file header.
        dos.writeInt((int) ze.offset);

        // File name.
        dos.write(name);

        // Extra field.
        dos.write(extra);

        // File comment.
        dos.write(data);
    }

    /**
     * Writes the &quot;End of central dir record&quot;
     *
     * @throws IOException On any I/O related issue.
     */
    private void writeEndOfCentralDirectory() throws IOException {
        final LEDataOutputStream dos = (LEDataOutputStream) out;

        dos.writeInt(ZIP.EOCD_SIG);

        // Disk numbers.
        dos.writeShort(0);
        dos.writeShort(0);

        // Number of entries.
        dos.writeShort(entries.size());
        dos.writeShort(entries.size());

        // Length and offset of Central Directory.
        dos.writeInt((int) cdLength);
        dos.writeInt((int) cdOffset);

        // ZIP file comment.
        String comment = getComment();
        if (comment == null)
            comment = "";
        byte[] data = comment.getBytes(charset);
        dos.writeShort(data.length);
        dos.write(data);
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with the stream.
     * This closes the open output stream writing to this ZIP file,
     * if any.
     *
     * @throws IOException On any I/O related issue.
     */
    public void close() throws IOException {
        if (closed)
            return;

        // Order is important here!
        closed = true;
        try {
            finish();
        } finally {
            entries.clear();
            super.close();
        }
    }

    /**
     * A Deflater which can be asked for its current deflation level.
     */
    private static class ZipDeflater extends Deflater {
        private int level = Deflater.DEFAULT_COMPRESSION;
        
        public ZipDeflater() {
            super(Deflater.DEFAULT_COMPRESSION, true);
        }
        
        public int getLevel() {
            return level;
        }
        
        public void setLevel(int level) {
            super.setLevel(level);
            this.level = level;
        }
    }
}
