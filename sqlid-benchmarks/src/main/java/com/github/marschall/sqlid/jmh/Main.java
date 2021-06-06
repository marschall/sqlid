package com.github.marschall.sqlid.jmh;

import java.io.IOException;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class Main {

  public static void main(String[] args) throws RunnerException, IOException {
    String fileName = "jmh-result.txt";
    Options options = new OptionsBuilder()
            .include("com\\.github\\.marschall\\.sqlid\\.jmh\\..*Benchmarks")
            .forks(1)
            .warmupIterations(3)
            .measurementIterations(5)
            .resultFormat(ResultFormatType.TEXT)
            .output(fileName)
            .addProfiler("gc")
            .build();
    new Runner(options).run();
  }

}
