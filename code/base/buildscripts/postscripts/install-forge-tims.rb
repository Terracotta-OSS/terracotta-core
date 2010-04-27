#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  protected

  # - Install TIMs from the Forge into the kit using the tim-get tool.
  def postscript(ant, build_environment, product_directory, *args)
    return if @no_extra
    return if @no_tims

    args.each do |tim|
      next if tim.nil? || tim.strip.empty?
      puts("***** Installing #{tim}")
      tim_get(:install, tim)
    end
  end
end
