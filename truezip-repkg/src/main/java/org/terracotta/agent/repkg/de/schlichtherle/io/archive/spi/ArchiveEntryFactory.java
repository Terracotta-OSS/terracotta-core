/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ArchiveEntryFactory.java
 *
 * Created on 3. Dezember 2006, 20:29
 */

package org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi;

import java.io.CharConversionException;

/**
 * A factory for archive entries.
 *
 * @deprecated Thie interface will vanish in TrueZIP 7.
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.4
 */
public interface ArchiveEntryFactory {

    ArchiveEntry createArchiveEntry(
            String entryName,
            ArchiveEntry blueprint)
    throws CharConversionException;
}
