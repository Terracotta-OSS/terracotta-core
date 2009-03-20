#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  protected
  def postscript(ant, build_environment, product_directory, *args)
    FileUtils.rm_rf(File.join(product_directory.to_s, "lib"))
  end
end