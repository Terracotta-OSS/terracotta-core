/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * Zip32OutputArchive.java
 *
 * Created on 27. Februar 2006, 01:10
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

package org.terracotta.agent.repkg.de.schlichtherle.io.archive.zip;


import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.terracotta.agent.repkg.de.schlichtherle.io.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.util.*;
import org.terracotta.agent.repkg.de.schlichtherle.util.*;
import org.terracotta.agent.repkg.de.schlichtherle.util.zip.*;

/**
 * An implementation of {@link OutputArchive} to write ZIP archives.
 * <p>
 * This output archive can only write one entry at a time.
 * Archive drivers may wrap this class in a {@link MultiplexedOutputArchive}
 * to overcome this limitation.
 * 
 * @author Christian Schlichtherle
 * @version @version@
 * @see Zip32Driver
 * @since TrueZIP 6.0
 */
public class Zip32OutputArchive
        extends BasicZipOutputStream
        implements OutputArchive {

    /** Prefix for temporary files created by the multiplexer. */
    private static final String TEMP_FILE_PREFIX = Zip32Driver.TEMP_FILE_PREFIX;

    private final Zip32InputArchive source;
    private OutputArchiveMetaData metaData;
    private Zip32Entry tempEntry;

    /** @deprecated */
    public Zip32OutputArchive(
            final OutputStream out,
            final String charset,
            final Zip32InputArchive source)
    throws  NullPointerException,
            UnsupportedEncodingException,
            IOException {
        this(out, charset, Zip32Driver.DEFAULT_LEVEL, source);
    }

    /**
     * Creates a new instance which uses the output stream, character set and
     * compression level.
     *
     * @param level The compression level to use.
     * @throws IllegalArgumentException If <code>level</code> is not in the
     *         range [{@value java.util.zip.Deflater#BEST_SPEED}..{@value java.util.zip.Deflater#BEST_COMPRESSION}]
     *         and is not {@value java.util.zip.Deflater#DEFAULT_COMPRESSION}.
     */
    public Zip32OutputArchive(
            final OutputStream out,
            final String charset,
            final int level,
            final Zip32InputArchive source)
    throws  NullPointerException,
            UnsupportedEncodingException,
            IOException {
        super(out, charset);
        super.setLevel(level);

        this.source = source;
        if (source != null) {
            // Retain comment and preamble of input ZIP archive.
            super.setComment(source.getComment());
            if (source.getPreambleLength() > 0) {
                final InputStream in = source.getPreambleInputStream();
                try {
                    org.terracotta.agent.repkg.de.schlichtherle.io.File.cat(
                            in, source.offsetsConsiderPreamble() ? this : out);
                } finally {
                    in.close();
                }
            }
        }
    }

    public int getNumArchiveEntries() {
        return size() + (tempEntry != null ? 1 : 0);
    }

    public Enumeration getArchiveEntries() {
        if (tempEntry == null)
            return super.entries();
        return new JointEnumeration(
                super.entries(),
                Collections.enumeration(
                    Collections.singletonList(tempEntry)));
    }

    public ArchiveEntry getArchiveEntry(final String entryName) {
        Zip32Entry e = (Zip32Entry) getEntry(entryName);
        if (e != null)
            return e;
        e = tempEntry;
        return e != null && entryName.equals(e.getName()) ? e : null;
    }

    public OutputStream getOutputStream(
            final ArchiveEntry dstEntry,
            final ArchiveEntry srcEntry)
    throws IOException {
        final Zip32Entry entry = (Zip32Entry) dstEntry;

        if (isBusy())
            throw new OutputArchiveBusyException(entry);

        if (entry.isDirectory()) {
            entry.setMethod(Zip32Entry.STORED);
            entry.setCrc(0);
            entry.setCompressedSize(0);
            entry.setSize(0);
            return new EntryOutputStream(entry);
        }

        if (srcEntry instanceof Zip32Entry) {
            // Set up entry attributes for Direct Data Copying (DDC).
            // A preset method in the entry takes priority.
            // The ZIP.RAES drivers use this feature to enforce deflation
            // for enhanced authentication security.
            final Zip32Entry srcZipEntry = (Zip32Entry) srcEntry;
            if (entry.getMethod() == Zip32Entry.UNKNOWN)
                entry.setMethod(srcZipEntry.getMethod());
            if (entry.getMethod() == srcZipEntry.getMethod())
                entry.setCompressedSize(srcZipEntry.getCompressedSize());
            entry.setCrc(srcZipEntry.getCrc());
            entry.setSize(srcZipEntry.getSize());
            return new EntryOutputStream(
                    entry, srcZipEntry.getMethod() != Zip32Entry.DEFLATED);
        }

        if (srcEntry != null)
            entry.setSize(srcEntry.getSize());

        switch (entry.getMethod()) {
            case Zip32Entry.UNKNOWN:
                entry.setMethod(Zip32Entry.DEFLATED);
                break;

            case Zip32Entry.STORED:
                if (entry.getCrc() == Zip32Entry.UNKNOWN
                        || entry.getCompressedSize() == Zip32Entry.UNKNOWN
                        || entry.getSize() == Zip32Entry.UNKNOWN) {
                    if (!(srcEntry instanceof RfsEntry)) {
                        final java.io.File temp = Temps.createTempFile(
                                TEMP_FILE_PREFIX);
                        return new TempEntryOutputStream(entry, temp);
                    }
                    final java.io.File file = ((RfsEntry) srcEntry).getFile();
                    final long length = file.length();
                    if (length > Integer.MAX_VALUE)
                        throw new IOException("file too large");
                    final InputStream in = new java.io.FileInputStream(file);
                    final Crc32OutputStream out = new Crc32OutputStream();
                    org.terracotta.agent.repkg.de.schlichtherle.io.File.cp(in, out);
                    entry.setCrc(out.crc.getValue());
                    entry.setCompressedSize(length);
                    entry.setSize(length);
                }
                break;

            case Zip32Entry.DEFLATED:
                break;

            default:
                assert false : "unsupported method";
        }
        return new EntryOutputStream(entry);
    }

    /**
     * Returns whether this output archive is busy writing an archive entry
     * or not.
     */
    public final boolean isBusy() {
        return super.isBusy() || tempEntry != null;
    }

    /**
     * This entry output stream writes directly to our subclass.
     * It can only be used if this output stream is not currently busy
     * writing another entry and the entry holds enough information to
     * write the entry header.
     * These preconditions are checked by {@link #getOutputStream}.
     */
    private class EntryOutputStream extends FilterOutputStream {
        private EntryOutputStream(Zip32Entry entry) throws IOException {
            this(entry, true);
        }

        private EntryOutputStream(Zip32Entry entry, boolean deflate)
        throws IOException {
            super(Zip32OutputArchive.this);
            putNextEntry(entry, deflate);
        }

        public void write(byte[] b) throws IOException {
            out.write(b, 0, b.length);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
        }

        public void close() throws IOException {
            closeEntry();
        }
    } // class EntryOutputStream

    /**
     * This entry output stream writes the entry to a temporary file.
     * When the stream is closed, the temporary file is then copied to this
     * output stream and finally deleted.
     */
    private class TempEntryOutputStream extends CheckedOutputStream {
        private final java.io.File temp;
        private boolean closed;

        public TempEntryOutputStream(
                final Zip32Entry entry,
                final java.io.File temp)
        throws IOException {
            super(new java.io.FileOutputStream(temp), new CRC32());
            assert entry.getMethod() == Zip32Entry.STORED;
            this.temp = temp;
            tempEntry = entry;
        }

        public void close() throws IOException {
            if (closed)
                return;

            // Order is important here!
            closed = true;
            try {
                try {
                    super.close();
                } finally {
                    final long length = temp.length();
                    if (length > Integer.MAX_VALUE)
                        throw new IOException("file too large");
                    tempEntry.setCrc(getChecksum().getValue());
                    tempEntry.setCompressedSize(length);
                    tempEntry.setSize(length);
                    storeTempEntry(tempEntry, temp);
                }
            } finally {
                tempEntry = null;
            }
        }
    } // class TempEntryOutputStream

    private void storeTempEntry(
            final Zip32Entry entry,
            final java.io.File temp)
    throws IOException {
        assert entry.getMethod() == Zip32Entry.STORED;
        assert entry.getCrc() != Zip32Entry.UNKNOWN;
        assert entry.getCompressedSize() != Zip32Entry.UNKNOWN;
        assert entry.getSize() != Zip32Entry.UNKNOWN;

        try {
            final InputStream in = new java.io.FileInputStream(temp);
            try {
                putNextEntry(entry);
                try {
                    org.terracotta.agent.repkg.de.schlichtherle.io.File.cat(in, this);
                } finally {
                    closeEntry();
                }
            } finally {
                in.close();
            }
        } finally {
            if (!temp.delete()) // may fail on Windoze if in.close() failed!
                temp.deleteOnExit(); // we're bullish never to leavy any temps!
        }
    }

    private static class Crc32OutputStream extends OutputStream {
        private final CRC32 crc = new CRC32();

        public void write(int b) {
            crc.update(b);
        }

        public void write(byte[] b, int off, int len) {
            crc.update(b, off, len);
        }
    } // class Crc32OutputStream

    /**
     * @deprecated This method will be removed in the next major version number
     *             release and should be implemented as
     *             <code>getOutputStream(entry, null).close()</code>.
     */
    public final void storeDirectory(ArchiveEntry entry)
    throws IOException {
        assert false : "Since TrueZIP 6.5, this is not used anymore!";
        if (!entry.isDirectory())
            throw new IllegalArgumentException();
        getOutputStream(entry, null).close();
    }

    /**
     * Retain the postamble of the source ZIP archive, if any.
     */
    public void finish() throws IOException {
        super.finish();

        if (source == null)
            return;

        final long ipl = source.getPostambleLength();
        if (ipl <= 0)
            return;

        final long il = source.length();
        final long ol = length();

        final InputStream in = source.getPostambleInputStream();
        try {
            // Second, if the output ZIP compatible file differs in length from
            // the input ZIP compatible file pad the output to the next four byte
            // boundary before appending the postamble.
            // This might be required for self extracting files on some platforms
            // (e.g. Wintel).
            if (ol + ipl != il)
                write(new byte[(int) (ol % 4)]);

            // Finally, write the postamble.
            org.terracotta.agent.repkg.de.schlichtherle.io.File.cat(in, this);
        } finally {
            in.close();
        }
    }

    //
    // Metadata implementation.
    //

    public OutputArchiveMetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(final OutputArchiveMetaData metaData) {
        this.metaData = metaData;
    }
}
