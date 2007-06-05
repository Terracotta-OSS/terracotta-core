import java.lang.Boolean
import terracotta

dso = terracotta.DSO(AdminTask)
dso.enable()

if AdminConfig.hasChanges():
    AdminConfig.save()
