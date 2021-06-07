package com.github.marschall.sqlid.jmh;

import java.io.IOException;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class Main {

  public static void main(String[] args) throws RunnerException, IOException {
    int threads = args.length > 0 ? Integer.parseInt(args[0]) : 1;
    String fileName = "jmh-result-threads-" + threads  + ".txt";
    Options options = new OptionsBuilder()
            .include("com\\.github\\.marschall\\.sqlid\\.jmh\\..*Benchmarks")
            .forks(1)
            .warmupIterations(3)
            .measurementIterations(5)
            .threads(threads)
            .resultFormat(ResultFormatType.TEXT)
            .output(fileName)
            .addProfiler("gc")
            .build();
    new Runner(options).run();
  }

}
