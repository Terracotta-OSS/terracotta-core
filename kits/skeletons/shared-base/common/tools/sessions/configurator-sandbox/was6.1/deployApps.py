import java.lang.System
import terracotta
import os

webappDir = java.lang.System.getProperty("webapp.dir")

if not webappDir:
    # For some reason on Windows the wsadmin.bat "-javaoption" option does not work correctly
    webappDir = os.path.join(os.environ["WAS_SANDBOX"], os.environ["PORT"], 'webapps')

appUtil = terracotta.AppUtil(AdminApp, webappDir)
appUtil.installAll()

if AdminConfig.hasChanges():
    AdminConfig.save()
