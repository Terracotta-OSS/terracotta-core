/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ArchiveEntry.java
 *
 * Created on 26. Februar 2006, 19:08
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


import javax.swing.Icon;

import org.terracotta.agent.repkg.de.schlichtherle.io.ArchiveEntryMetaData;
import org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi.ArchiveEntry;
import org.terracotta.agent.repkg.de.schlichtherle.util.zip.ZipEntry;

/**
 * An adapter class to make the {@link ZipEntry} class implement the
 * {@link ArchiveEntry} interface.
 *
 * @see Zip32Driver
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 */
public class Zip32Entry extends ZipEntry implements ArchiveEntry {

    static {
        assert (ZipEntry.UNKNOWN == ArchiveEntry.UNKNOWN);
    }

    /** The unknown value for numeric properties. */
    public static final byte UNKNOWN = ZipEntry.UNKNOWN;

    private ArchiveEntryMetaData metaData;

    public Zip32Entry(String entryName) {
        super(entryName);
    }

    public Zip32Entry(Zip32Entry blueprint) {
        super(blueprint);
    }

    protected void setName(String name) {
        super.setName(name);
    }

    public Icon getOpenIcon() {
        return null;
    }

    public Icon getClosedIcon() {
        return null;
    }

    //
    // Metadata implementation.
    //

    public ArchiveEntryMetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(ArchiveEntryMetaData metaData) {
        this.metaData = metaData;
    }
}
