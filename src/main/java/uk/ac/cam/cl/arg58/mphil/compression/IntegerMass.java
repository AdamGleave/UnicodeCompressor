/* Copyright (C) 2016 Adam Gleave and Christian Steinruecken */
package uk.ac.cam.cl.arg58.mphil.compression;

import uk.ac.cam.eng.ml.tcs27.compression.Mass;

public interface IntegerMass extends Mass<Integer> {
  /* start and end are inclusive */
  public double massBetween(Integer start, Integer end);
  public long discreteMassBetween(Integer start, Integer end);
}
