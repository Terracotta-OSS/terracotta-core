#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# this packaging option does nothing but copy the content under destdir
# to srcdir, which is "build/dist"
class BaseCodeTerracottaBuilder <  TerracottaBuilder
  protected
  def make_package(srcdir, destdir, filename, install_name, internal_name)
    ant.copy(:todir => destdir.to_s) do
      ant.fileset(:dir => srcdir.to_s)
    end
  end
end
