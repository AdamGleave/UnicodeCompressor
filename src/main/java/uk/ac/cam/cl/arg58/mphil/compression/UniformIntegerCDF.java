/* Copyright (C) 2016 Adam Gleave */
package uk.ac.cam.cl.arg58.mphil.compression;

import uk.ac.cam.eng.ml.tcs27.compression.SimpleMass;

import java.util.Random;

public class UniformIntegerCDF extends SimpleMass<Integer>
                               implements IntegerMass {
  int start, end, n;

  /** Constructs a uniform distribution over integers
   *  between <var>start</var> (inclusive) and <var>end</var> (inclusive). */
  public UniformIntegerCDF(int start, int end) {
    this.start = start;
    this.end = end;
    this.n = end - start + 1;
  }

  @Override
  public double massBetween(Integer from, Integer to) {
    if (to < from) {
      return 0;
    } else {
      to = Math.min(to, end);
      from = Math.max(from, start);
      return (to - from + 1) / n;
    }
  }

  @Override
  public long discreteMassBetween(Integer from, Integer to) {
    if (to < from) {
      return 0;
    } else {
      to = Math.min(to, end);
      from = Math.max(from, start);
      return to - from + 1;
    }
  }

  @Override
  public double mass(Integer x) {
    if (x >= 0 && x <= end) {
      return 1 / n;
    } else {
      return 0;
    }
  }

  @Override
  public double logMass(Integer x) {
    if (x >= 0 && x <= end) {
      return -Math.log(n);
    } else {
      return Double.NEGATIVE_INFINITY;
    }
  }

  @Override
  public Integer sample(Random rnd) {
    return rnd.nextInt(n) + start;
  }
}