#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  # - Remove samples directory
  protected
  def postscript(ant, build_environment, product_directory, *args)
    args.each do |arg|
      destdir = FilePath.new(product_directory, *arg.split('/'))
      destdir.delete
    end
  end
end
