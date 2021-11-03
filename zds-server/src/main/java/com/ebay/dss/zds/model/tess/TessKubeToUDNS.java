package com.ebay.dss.zds.model.tess;

/**
 Created by tatian on 2020-11-19.



 DNS Records Type:
 A:   A Record points your hostname to an IP address
 PTR: PTR records are used for the Reverse DNS (Domain Name System) lookup.
 Using the IP address you can get the associated domain/hostname.
 An A record should exist for every PTR record.
 The usage of a reverse DNS setup for a mail server is a good solution.
 3600: TTL
 IN: CLASS fields appear in resource records. The following CLASS mnemonics and values are defined:

 IN            1 the Internet

 CS            2 the CSNET class (Obsolete - used only for examples in some obsolete RFCs)

 CH            3 the CHAOS class

 HS            4 Hesiod [Dyer 87]
 */

import java.util.ArrayList;
import java.util.List;

/**
 livy-apollorno-0.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io. 3600    IN      A       10.174.70.15
 livy-apollorno-1.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io. 3600    IN      A       10.174.70.39
 livy-apollorno-2.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io. 3600    IN      A       10.174.70.44
 livy-apollorno-3.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io. 3600    IN      A       10.174.70.61
 livy-apollorno-4.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io. 3600    IN      A       10.174.70.72
 livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.  3600    IN      A       10.174.70.15
 livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.  3600    IN      A       10.174.70.39
 livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.  3600    IN      A       10.174.70.44
 livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.  3600    IN      A       10.174.70.61
 livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.  3600    IN      A       10.174.70.72
 15.70.174.10.in-addr.arpa.      3600    IN      PTR     livy-apollorno-0.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.
 39.70.174.10.in-addr.arpa.      3600    IN      PTR     livy-apollorno-1.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.
 44.70.174.10.in-addr.arpa.      3600    IN      PTR     livy-apollorno-2.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.
 61.70.174.10.in-addr.arpa.      3600    IN      PTR     livy-apollorno-3.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.
 72.70.174.10.in-addr.arpa.      3600    IN      PTR     livy-apollorno-4.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.
 * */
public class TessKubeToUDNS {

  private List<KubeToDNSRecord> records;

  public TessKubeToUDNS(List<KubeToDNSRecord> records) {
    this.records = records;
  }

  public List<KubeToDNSRecord> getRecords() {
    return records;
  }

  public static TessKubeToUDNS fromAnnotationStr(String str) {
    String[] records = str.split("\n");
    assert records.length % 3 == 0;
    int last = records.length / 3;
    List<KubeToDNSRecord> list = new ArrayList<>();
    for(int i = 0; i < last; i++) {
      list.add(KubeToDNSRecord.fromRecord(records[i].trim()));
    }
    return new TessKubeToUDNS(list);
  }

  public static void main(String[] args) {
    String str = "livy-apollorno-0.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io. 3600    IN      A       10.174.70.15\n" +
            " livy-apollorno-1.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io. 3600    IN      A       10.174.70.39\n" +
            " livy-apollorno-2.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io. 3600    IN      A       10.174.70.44\n" +
            " livy-apollorno-3.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io. 3600    IN      A       10.174.70.61\n" +
            " livy-apollorno-4.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io. 3600    IN      A       10.174.70.72\n" +
            " livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.  3600    IN      A       10.174.70.15\n" +
            " livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.  3600    IN      A       10.174.70.39\n" +
            " livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.  3600    IN      A       10.174.70.44\n" +
            " livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.  3600    IN      A       10.174.70.61\n" +
            " livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.  3600    IN      A       10.174.70.72\n" +
            " 15.70.174.10.in-addr.arpa.      3600    IN      PTR     livy-apollorno-0.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.\n" +
            " 39.70.174.10.in-addr.arpa.      3600    IN      PTR     livy-apollorno-1.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.\n" +
            " 44.70.174.10.in-addr.arpa.      3600    IN      PTR     livy-apollorno-2.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.\n" +
            " 61.70.174.10.in-addr.arpa.      3600    IN      PTR     livy-apollorno-3.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.\n" +
            " 72.70.174.10.in-addr.arpa.      3600    IN      PTR     livy-apollorno-4.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.";
    TessKubeToUDNS udns = fromAnnotationStr(str);
    udns.getRecords().forEach(r -> System.out.println(r.host));
  }

}
