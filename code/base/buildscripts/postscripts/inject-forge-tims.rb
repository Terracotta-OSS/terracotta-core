#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

require 'find'

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
    
    tims     = {}
    timnames = args[0]['tims']
    tim_getpath = tim_get.to_s
    tim_getpath.gsub!(/\\/, '/')
    timnames.each do |entry|
      data = %x[#{tim_getpath} list #{entry}].split("\n").last
      data.gsub!(/[()]/, '')
      data = data.split
      tims[data.first] = data.last
    end
    
    dirnames = args[1]['dest']
    dirnames.each do |entry|
      srcdir = FilePath.new(product_directory, *entry.split('/'))
      Find.find(srcdir.to_s) do |path|
        next unless FileTest.file?(path) && File.basename(path) =~ /^tc-config.xml$/
        config = File.read(path)
        tims.each do |k, v|
          value = "<module name=\"#{k}\" version=\"#{v}\"/>"
          regex = /<module *name *= *"#{Regexp.escape(k)}" *version *= *"@tim.version@" *\/>/
          File.open(path, 'w') {|f| f.write(config) } if config.gsub!(regex, value)
        end
      end
    end
  end
end
