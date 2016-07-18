/* Copyright (C) 2016 Adam Gleave */
package uk.ac.cam.cl.arg58.mphil.compression;

import uk.ac.cam.eng.ml.tcs27.compression.SimpleMass;

import java.util.Random;

public class UniformIntegerCDF extends SimpleMass<Integer>
                               implements IntegerMass {
  int n;

  public UniformIntegerCDF(int n) {
    this.n = n;
  }

  @Override
  public double massBetween(Integer start, Integer end) {
    if (end < start) {
      return 0;
    } else {
      end = Math.min(end, n - 1);
      start = Math.max(start, 0);
      return (end - start + 1) / n;
    }
  }

  @Override
  public long discreteMassBetween(Integer start, Integer end) {
    if (end < start) {
      return 0;
    } else {
      end = Math.min(end, n - 1);
      start = Math.max(start, 0);
      return end - start + 1;
    }
  }

  @Override
  public double mass(Integer x) {
    if (x >= 0 && x < n) {
      return 1 / n;
    } else {
      return 0;
    }
  }

  @Override
  public double logMass(Integer x) {
    if (x >= 0 && x < n) {
      return -Math.log(n);
    } else {
      return Double.NEGATIVE_INFINITY;
    }
  }

  @Override
  public Integer sample(Random rnd) {
    return rnd.nextInt(n);
  }
}
