package demo.continuations;

import com.uwyn.rife.engine.Site;
import com.uwyn.rife.rep.BlockingRepository;
import com.uwyn.rife.rep.Rep;
import com.uwyn.rife.resources.ResourceFinderClasspath;
import com.uwyn.rife.servlet.RifeLifecycle;

/**
 *  This is a custom implementation of the entire lifecycle of your RIFE
 *  application. RIFE's standard web.xml file has been modified and instead of
 *  having a rep.path init-param, it uses a lifecycle.classname init-param
 *  that provides the classname of the custom lifecycle implementation.
 *
 *@author    Terracotta, Inc.
 */
public class LifeCycle extends RifeLifecycle {
   public LifeCycle() {
      BlockingRepository rep = new BlockingRepository();
      rep.addParticipant(ParticipantSite.class, Site.DEFAULT_PARTICIPANT_NAME, false, null);
      rep.runParticipants();
      // don't forget to set the default repository
      Rep.setDefaultRepository(rep);
   }
}