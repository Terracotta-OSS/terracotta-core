#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  protected

  # - Install TIMs from the Forge into the kit using the tim-get tool.
  def postscript(ant, build_environment, product_directory, *args)
    return if @no_demo
    tim_get = FilePath.new(product_directory, 'bin', 'tim-get').script_extension
    args.each do |tim|
      puts("***** Installing #{tim}")
      unless system(tim_get.to_s, 'install', tim)
        raise("Failed to install #{tim}")
      end
    end
  end
end
