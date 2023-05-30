package org.folio.harvesteradmin.test;

public class SampleId {
  public static final int SAMPLES_ID_PREFIX = 9631;
  public static final int ZEROES = 100000;
  int identifyingPart;

  public SampleId(int smallId) {
    this.identifyingPart = smallId;
  }

  public int fullId() {
    return (SAMPLES_ID_PREFIX * ZEROES) + identifyingPart;
  }

  public String toString() {
    return Integer.toString(fullId());
  }
}
