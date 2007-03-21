/*
 * Copyright 2001-2007 Geert Bevin <gbevin[remove] at uwyn dot com>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 * $Id$
 */
import com.uwyn.rife.engine.Site;
import com.uwyn.rife.engine.SiteBuilder;
import com.uwyn.rife.rep.SingleObjectParticipant;

public class ParticipantSite extends SingleObjectParticipant {
    private Site site;
    
    public ParticipantSite() {
        SiteBuilder builder = new SiteBuilder("main");
        builder
            .setArrival("Order")
            
            .enterElement()
                .setImplementation(Order.class)
            .leaveElement();
        
        site = builder.getSite();
    }
    
    public Object getObject() {
        return site;
    }
}
