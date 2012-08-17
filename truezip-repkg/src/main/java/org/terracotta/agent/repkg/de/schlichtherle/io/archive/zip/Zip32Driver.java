/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * Zip32Driver.java
 *
 * Created on 24. Dezember 2005, 00:01
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
import java.util.zip.*;

import javax.swing.*;

import org.terracotta.agent.repkg.de.schlichtherle.io.archive.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.rof.*;

/**
 * An archive driver which builds ZIP files.
 * Note that this driver does not check the CRC value of any entries in
 * existing archives.
 * <p>
 * Instances of this class are immutable.
 *
 * @see CheckedZip32Driver
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 */
public class Zip32Driver extends AbstractArchiveDriver {
    private static final long serialVersionUID = -7061546656075796996L;

    /** Prefix for temporary files created by this driver. */
    static final String TEMP_FILE_PREFIX = "tzp-zip";

    /**
     * The default character set to use for entry names and comments,
     * which is {@value}.
     */
    public static final String DEFAULT_CHARSET = "IBM437";

    /**
     * The default compression level to use when writing a ZIP output stream,
     * which is {@value}.
     */
    public static final int DEFAULT_LEVEL = Deflater.BEST_COMPRESSION;

    private final boolean preambled, postambled;
    private final int level;

    /**
     * Equivalent to {@link #Zip32Driver(String, Icon, Icon, boolean, boolean, int)
     * this(DEFAULT_CHARSET, null, null, false, false, DEFAULT_LEVEL)}.
     */
    public Zip32Driver() {
        this(DEFAULT_CHARSET, null, null, false, false, DEFAULT_LEVEL);
    }

    /**
     * Equivalent to {@link #Zip32Driver(String, Icon, Icon, boolean, boolean, int)
     * this(charset, null, null, false, false, DEFAULT_LEVEL)}.
     */
    public Zip32Driver(String charset) {
        this(charset, null, null, false, false, DEFAULT_LEVEL);
    }

    /**
     * Equivalent to {@link #Zip32Driver(String, Icon, Icon, boolean, boolean, int)
     * this(DEFAULT_CHARSET, null, null, false, false, level)}.
     */
    public Zip32Driver(int level) {
        this(DEFAULT_CHARSET, null, null, false, false, level);
    }

    public Zip32Driver(
            final String charset,
            final boolean preambled,
            final boolean postambled,
            final Icon openIcon,
            final Icon closedIcon) {
        this(charset, openIcon, closedIcon, preambled, postambled, DEFAULT_LEVEL);
    }

    /**
     * Constructs a new ZIP driver.
     *
     * @param preambled <code>true</code> if and only if a prospective ZIP
     *        compatible file is allowed to contain preamble data before the
     *        actual ZIP file data.
     *        Self Extracting Archives typically use the preamble to store the
     *        application code that is required to extract the ZIP file contents.
     *        <p>
     *        Please note that any ZIP compatible file may actually have a
     *        preamble. However, for performance reasons this parameter should
     *        be set to <code>false</code>, unless required.
     * @param postambled <code>true</code> if and only if a prospective ZIP
     *        compatible file is allowed to have a postamble of arbitrary
     *        length.
     *        If set to <code>false</code>, the ZIP compatible file may still
     *        have a postamble. However, the postamble must not exceed 64KB
     *        size, including the End Of Central Directory record and thus
     *        the ZIP file comment. This causes the initial ZIP file
     *        compatibility test to fail fast if the file is not compatible
     *        to the ZIP File Format Specification.
     *        For performance reasons, this parameter should be set to
     *        <code>false</code> unless you need to support Self Extracting
     *        Archives with very large postambles.
     * @param level The compression level to use when writing a ZIP output
     *        stream.
     * @throws IllegalArgumentException If <code>level</code> is not in the
     *         range [{@value java.util.zip.Deflater#BEST_SPEED}..{@value java.util.zip.Deflater#BEST_COMPRESSION}]
     *         and is not {@value java.util.zip.Deflater#DEFAULT_COMPRESSION}.
     */
    public Zip32Driver(
            final String charset,
            final Icon openIcon,
            final Icon closedIcon,
            final boolean preambled,
            final boolean postambled,
            final int level) {
        super(charset, openIcon, closedIcon);
        if (    (   level < Deflater.BEST_SPEED
                 || level > Deflater.BEST_COMPRESSION)
                && level != Deflater.DEFAULT_COMPRESSION)
            throw new IllegalArgumentException();
        this.preambled = preambled;
        this.postambled = postambled;
        this.level = level;
    }

    //
    // Properties:
    //
    
    /**
     * Returns the value of the property <code>preambled</code> which was 
     * provided to the constructor.
     */
    public final boolean getPreambled() {
        return preambled;
    }

    /**
     * Returns the value of the property <code>postambled</code> which was 
     * provided to the constructor.
     */
    public final boolean getPostambled() {
        return postambled;
    }

    /**
     * Returns the value of the property <code>level</code> which was 
     * provided to the constructor.
     */
    public final int getLevel() {
        return level;
    }

    //
    // Factory methods:
    //

    public ArchiveEntry createArchiveEntry(
            final Archive archive,
            final String entryName,
            final ArchiveEntry template)
    throws CharConversionException {
        ensureEncodable(entryName);

        final Zip32Entry entry;
        if (template != null) {
            if (template instanceof Zip32Entry) {
                entry = new Zip32Entry((Zip32Entry) template);
                entry.setName(entryName);
            } else {
                entry = new Zip32Entry(entryName);
                entry.setTime(template.getTime());
                entry.setSize(template.getSize());
            }
        } else {
            entry = new Zip32Entry(entryName);
        }
        
        return entry;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation simply forwards the call to
     * {@link #createZip32InputArchive}.
     */
    public InputArchive createInputArchive(Archive archive, ReadOnlyFile rof)
    throws IOException {
        return createZip32InputArchive(archive, rof);
    }

    protected Zip32InputArchive createZip32InputArchive(
            Archive archive,
            ReadOnlyFile rof)
    throws IOException {
        return new Zip32InputArchive(
                rof, getCharset(), preambled, postambled);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation simply forwards the call to
     * {@link #createZip32OutputArchive} and wraps the result in a new
     * {@link MultiplexedOutputArchive}.
     */
    public OutputArchive createOutputArchive(
            Archive archive,
            OutputStream out,
            InputArchive source)
    throws IOException {
      throw new UnsupportedOperationException();
//        return new MultiplexedOutputArchive(createZip32OutputArchive(archive, out, (Zip32InputArchive) source));
    }

    protected Zip32OutputArchive createZip32OutputArchive(
            Archive archive,
            OutputStream out,
            Zip32InputArchive source)
    throws IOException {
        return new Zip32OutputArchive(out, getCharset(), level, source);
    }
}
