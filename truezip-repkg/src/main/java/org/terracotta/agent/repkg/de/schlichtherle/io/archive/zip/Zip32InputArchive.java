/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * Zip32InputArchive.java
 *
 * Created on 27. Februar 2006, 09:12
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

package org.terracotta.agent.repkg.de.schlichtherle.io.archive.zip;


import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.terracotta.agent.repkg.de.schlichtherle.io.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.rof.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.util.*;
import org.terracotta.agent.repkg.de.schlichtherle.util.zip.*;
import org.terracotta.agent.repkg.de.schlichtherle.util.zip.ZipEntry;

/**
 * An implementation of {@link InputArchive} to read ZIP32 archives.
 *
 * @see Zip32Driver
 * 
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 */
public class Zip32InputArchive
        extends BasicZipFile
        implements InputArchive {

    private InputArchiveMetaData metaData;

    public Zip32InputArchive(
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

    protected ZipEntry createZipEntry(String entryName) {
        return new Zip32Entry(Paths.normalize(entryName, '/'));
    }

    public int getNumArchiveEntries() {
        return super.size();
    }

    public Enumeration getArchiveEntries() {
        return super.entries();
    }

    public ArchiveEntry getArchiveEntry(final String entryName) {
        return (Zip32Entry) super.getEntry(entryName);
    }

    public InputStream getInputStream(
            final ArchiveEntry entry,
            final ArchiveEntry dstEntry)
    throws IOException {
        return super.getInputStream(
                entry.getName(), false, !(dstEntry instanceof Zip32Entry));
    }

    //
    // Metadata implementation.
    //

    public InputArchiveMetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(InputArchiveMetaData metaData) {
        this.metaData = metaData;
    }
}
