#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  protected

  # - Inject TIM configurations into sample tc-configs.
  def postscript(ant, build_environment, product_directory, *args)
    return if @no_demo

    tim_get = FilePath.new(product_directory, 'bin', 'tim-get').script_extension
    unless File.exist?(tim_get.to_s)
      raise("Cannot find tim-get executable at expected location: #{tim_get}")
    end
    unless File.executable?(tim_get.to_s)
      raise("tim-get script exists, but is not executable")
    end
    
    timnames = args[0]['tims']
    dirnames = args[1]['dest']
    dirnames.each do |entry|
      srcdir = FilePath.new(product_directory, *entry.split('/'))
#Find.find(srcdir) do |path|
#  if FileTest.file? path
#    puts "x: #{path}"
#  end
#end
    end

#puts "xxxxxxxxxxxxxxxxxx"
#puts "xx"
#puts args[0]['tims']
#puts "--"
#puts args[1]['dest']
#puts "xx"
#puts "xxxxxxxxxxxxxxxxxx"
#   args.each do |tim|
#     puts("***** Retrieving info #{tim} :: #{tim_get.to_s}")
#     unless system(tim_get.to_s, 'info', tim)
#       raise("Failed to retrieve info #{tim}.  Exit status: #{$?.exitstatus}")
#     end
#   end
  end
end
