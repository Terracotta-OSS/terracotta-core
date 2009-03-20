#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  # - set the execute permission of all the script files in the kit
  protected
  def postscript(ant, build_environment, product_directory, *args)
    Dir.chdir(File.join(product_directory.to_s)) do
      # DEV-2567
      FileUtils.rm_rf("api-jars")
    end
  end
end
