package demo.continuations;

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
