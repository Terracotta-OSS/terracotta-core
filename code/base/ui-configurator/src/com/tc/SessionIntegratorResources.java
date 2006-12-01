/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import java.util.ListResourceBundle;

public class SessionIntegratorResources extends ListResourceBundle {
  public Object[][] getContents() {
    return contents;
  }

  static final Object[][] contents = {
    {"title", "Terracotta Sessions Configurator"},
    {"quit.action.label", "Quit"},
    {"file.menu.label", "File"},
    {"output.menu.label", "Output"},
    {"help.menu.label", "Help"},
    {"help.item.label", "Terracotta Sessions Configurator Help..."},
    {"about.action.label", "About Terracotta Sessions Configurator"},
    {"visit.forums.title", "Visit Terracotta Forums"},
    {"forums.url", "http://www.terracottatech.com/forums/"},
    {"contact.support.title", "Contact Terracotta Technical Support"},
    {"support.url", "http://www.terracottatech.com/support_services.shtml"},
    {"contact.field.eng.title", "Contact Terracotta Field Engineering"},
    {"field.eng.url", "http://www.terracottatech.com/contact/field/"},
    {"contact.sales.title", "Contact Terracotta Sales"},
    {"sales.url", "http://www.terracottatech.com/contact/"},
    {"clear.all.action.name", "Clear all"},
    {"help.action.name", "Help..."},
    {"show.splash.action.name", "Show Splash Screen..."},
    {"show.help.error", "Couldn't show help"},
    {"servers.action.name", "Servers..."},
    {"import.webapp.action.name", "Import webapp..."},
    {"export.configuration.action.name", "Export configuration..."},
    {"quit.action.name", "Exit Configurator"},
    {"quitting.dialog.msg", "Stopping servers. Please wait..."},
    {"not.war.msg", "Must be an archived or exploded .war file."},
    {"install.webapp.success.msg", "Successfully imported ''${0}''"},
    {"install.webapp.restart.msg", "The webapp will not be available until the system is restarted."},
    {"install.webapp.failure.msg", "Unable to import ''${0}''"},
    {"destination.not.directory.msg", "Destination is not a directory"},
    {"src.webapp.not.found.msg", "Unable to locate source for web application ''${0}'':\n\n${1}\n\nRemove this web application?"},
    {"refresh.success.msg", "Successfully refreshed ''${0}''"},
    {"refresh.failure.msg", "Unable to refresh ''${0}''"},
    {"cannot.remove.while.running.msg", "You cannot remove webapps while the system is running."},
    {"remove.success.msg", "Successfully removed ''${0}''"},
    {"remove.failure.msg", "Unable to remove ''${0}''"},
    {"query.save.config.msg", "The configuration is modified. Save?"},
    {"configuration.load.failure.msg", "Can't load configuration"},
    
    {"start.all.label", "Start all"},
    {"restart.all.label", "Restart all"},
    {"start.label", "Start"},
    {"stop.label", "Stop"},
  };
}
