#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#
class BaseCodeTerracottaBuilder <  TerracottaBuilder
  protected

  include BuildData

  def postscript(ant, build_environment, product_directory, *args)
    destdir = FilePath.new(product_directory, args.first).ensure_directory
    create_build_data(@config_source, destdir)
  end
end
