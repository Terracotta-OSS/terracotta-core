require 'spec_helper'
require 'paths'

describe FilePath do
  describe "#initialize" do
    HOME_PATH = ENV['HOME'].sub(/^([A-Z])/) { $1.downcase }
    NETWORK_PATH = "//pebble/monkeyshare/setup/jdk/windows/hotspot1.6.0_24"

    it "creates a FilePath object" do
      path = FilePath.new(HOME_PATH)
      path.to_s.should == ENV['HOME']
    end

    it "recognizes absolute paths" do
      path = FilePath.new(HOME_PATH)
      path.should be_absolute
    end

    it "treats network paths as absolute paths" do
      path = FilePath.new(NETWORK_PATH)
      path.should be_absolute
    end

    it "converts absolute paths to canonical form correctly" do
      path = FilePath.new(HOME_PATH)
      path.to_s.should == HOME_PATH
      path.canonicalize.to_s.should == HOME_PATH
    end

    it "converts relative paths to canonical form correctly" do
      path = FilePath.new('.')
      path.to_s.should == '.'
      path.canonicalize.to_s.should == File.expand_path('.')
    end

    it "converts network paths to canonical form correctly" do
      path = FilePath.new(NETWORK_PATH)
      path.canonicalize.to_s.should == NETWORK_PATH[1..-1]
    end
  end
end
