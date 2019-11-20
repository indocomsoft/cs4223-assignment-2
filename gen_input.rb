#!/usr/bin/env ruby
# frozen_string_literal: true

%w[MESI MOESI Dragon].each do |protocol|
  %w[blackscholes_four/blackscholes bodytrack_four/bodytrack fluidanimate_four/fluidanimate].each do |input|
    (0..15).each do |cache_size_log2|
      (0..cache_size_log2).each do |associativity_log2|
        (0..cache_size_log2).each do |block_size_log2|
          cache_size = 2**cache_size_log2
          associativity = 2**associativity_log2
          block_size = 2**block_size_log2
          num_blocks = cache_size / block_size
          num_sets = num_blocks / associativity
          if !num_sets.zero? && (num_sets & (num_sets - 1)).zero?
            filename = "log/#{protocol} #{input.gsub("/","-")} #{cache_size} #{associativity} #{block_size}"
            if !File.exist?(filename) || File.zero?(filename)
              puts "#{protocol} #{input} #{cache_size} #{associativity} #{block_size}"
            end
          end
        end
      end
    end
  end
end
