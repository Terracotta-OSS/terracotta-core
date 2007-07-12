#
# All content copyright (c) 2003-2007 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  protected
  def make_package(srcdir, destdir, filename, install_name, internal_name)
    ant.jar(:basedir => srcdir, :destfile => FilePath.new("#{destdir}", "#{filename}.jar").to_s)
  end
end