/*
 * Copyright 2001-2007 Geert Bevin <gbevin[remove] at uwyn dot com>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 * $Id$
 */
import com.uwyn.rife.rep.BlockingRepository;
import com.uwyn.rife.rep.Rep;
import com.uwyn.rife.resources.ResourceFinderClasspath;
import com.uwyn.rife.servlet.RifeLifecycle;

/**
 * This is a custom implementation of the entire lifecycle of your RIFE
 * application. RIFE's standard web.xml file has been modified and instead of having
 * a rep.path init-param, it uses a lifecycle.classname init-param that provides
 * the classname of the custom lifecycle implementation.
 */
public class LifeCycle extends RifeLifecycle {
    public LifeCycle() {
        BlockingRepository rep = new BlockingRepository();
        rep.addParticipant(ParticipantSite.class);
        rep.runParticipants();
        // don't forget to set the default repository
        Rep.setDefaultRepository(rep);
    }
}
