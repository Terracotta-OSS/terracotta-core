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

    tims     = {}
    timnames = args[0]['tims']
    timnames.each do |entry|
      output = tim_get_output(:list, entry).split("\n")
      output.delete_if { |line| 
        puts "[xxx] #{line}"
        !line.index(entry) 
      }
      data = output.last
      unless data.nil?
        data.gsub!(/[()]/, '')
        data = data.split
        data.shift
        tims[data.first] = data.last
      end
    end

    if !tims.empty? && timnames.size != tims.size
      msg  = "Unable to locate all of the TIMs listed in the distribution config - only some TIM info will be injected.\n" 
      msg << "What got installed: #{tims.keys.join(', ')}\n"
      msg << "What was specified: #{timnames.join(', ')}"
      fail msg
    end
  
    if tims.empty? 
      msg = "Unable to locate any TIMs - no TIM info will be injected.\n"
      msg << "What was specified: #{timnames.join(', ')}"
      fail msg
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
