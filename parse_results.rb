#!/usr/bin/env ruby
# frozen_string_literal: true

require 'csv'

PROCESSOR_COLS = %w[exec idle].freeze
PROCESSOR_OFFSETS = [4, 22].freeze
ALL_COLS = %w[bustraffic invalidationupdate exec].freeze
ALL_OFFSETS = [34, 37, 2].freeze

CSV.open('result.csv', 'w') do |csv|
  csv << %w[protocol input cachesize associativity blocksize] +
         %w[exec0 exec1 exec2 exec3 idle0 idle1 idle2 idle3 bustraffic] +
         %w[invalidationupdate exec load0 load1 load2 load3 store0 store1 store2] +
         %w[store3 miss0 miss1 miss2 miss3 private0 private1 private2] +
         %w[private3 shared0 shared1 shared2 shared3]
  Dir['log/*'].each do |filename|
    p filename
    data = File.read(filename).split("\n")
    cols_data =
      filename[4..-1].split(' ') +
      PROCESSOR_OFFSETS.flat_map do |offset|
        (0..3).flat_map do |id|
          [
            /- Processor #{id} = ([0-9]+)/.match(data[offset + id])[1]
          ]
        end
      end +
      ALL_COLS.zip(ALL_OFFSETS).flat_map do |_col, offset|
        [/= ([0-9]+)/.match(data[offset])[1]]
      end +
      (0..3).flat_map do |id|
        [
          /Load = ([0-9]+),/.match(data[16 + id])[1]
        ]
      end +
      (0..3).flat_map do |id|
        [
          /Store = ([0-9]+)/.match(data[16 + id])[1]
        ]
      end +
      (0..3).flat_map do |id|
        [
          /- Processor #{id} = ([0-9.]+)/.match(data[28 + id])[1]
        ]
      end +
      (0..3).flat_map do |id|
        [
          /numPrivateAccess = ([0-9.]+)/.match(data[40 + id])[1]
        ]
      end +
      (0..3).flat_map do |id|
        [
          /numSharedAccess = ([0-9.]+)/.match(data[40 + id])[1]
        ]
      end
    csv << cols_data
  end
end
