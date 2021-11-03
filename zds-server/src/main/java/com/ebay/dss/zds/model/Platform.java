package com.ebay.dss.zds.model;

public enum Platform {
  hercules(10), ares(2), apollo(3), apollorno(14), hermes(16), hermesrno(16);

  private int id;

  Platform(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }
}
