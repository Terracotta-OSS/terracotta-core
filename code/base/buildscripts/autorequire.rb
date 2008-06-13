#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# A nifty little class that lets you make Ruby automagically load ('require')
# all files in a given directroy.
class AutoRequire
  # Loads (via 'require') all '.rb' files in the given directory, in alphabetical
  # order. (You really should make it so that order doesn't matter, but in order
  # to prevent the kind of huge pain-in-the-ass bugs that result from, e.g., you
  # accidentally relying on an undefined order and then watching it break only when
  # someone runs your code on Linux on a ReiserFS volume, we just sort the list
  # in the first place.)
  def self.all_in_directory(directory)
    unless FileTest.directory?(directory)
      raise RuntimeError, "Directory '%s' doesn't exist; we can't import any files from it." % directory
    end

    Dir.open(directory) do |dir|
      entries = [ ]
      dir.each { |entry| entries << entry }
      entries.sort.each do |entry|
        path = File.join(directory, entry)
        require path if path =~ /\.rb$/ && path !~ /_tests\.rb$/
      end
    end
  end
end
