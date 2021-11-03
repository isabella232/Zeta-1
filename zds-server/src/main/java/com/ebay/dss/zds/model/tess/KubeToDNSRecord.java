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
public class KubeToDNSRecord {

  public static final String A = "A";
  public static final String PTR = "PTR";

  public final String host;
  public final String ip;
  public final long ttl;
  public final String className;
  public final String dnsRecordsType;

  public KubeToDNSRecord(String host, String ip, long ttl, String className, String dnsRecordsType) {
    this.host = host;
    this.ip = ip;
    this.ttl = ttl;
    this.className = className;
    this.dnsRecordsType = dnsRecordsType;
  }

  public static KubeToDNSRecord fromRecord(String record) {
    String[] segments = record.split(" +");
    assert segments.length == 5;
    String dnsRecordsType = segments[3];
    long ttl = Long.valueOf(segments[1]);
    String className = segments[2];
    if (A.endsWith(dnsRecordsType)) {
      return new KubeToDNSRecord(trimDot(segments[0]), segments[4], ttl, className, dnsRecordsType);
    } else if (PTR.endsWith(dnsRecordsType)) {
      return new KubeToDNSRecord(trimDot(segments[4]), reverseArpaAddr(segments[0]), ttl, className, dnsRecordsType);
    } else {
      throw new IllegalArgumentException("Can not recognized DNS record type: " + dnsRecordsType);
    }
  }

  public static void main(String[] args) {
    System.out.println(fromRecord("livy-apollorno-0.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io. 3600    IN      A       10.174.70.15").host);
    System.out.println(fromRecord("15.70.174.10.in-addr.arpa.      3600    IN      PTR     livy-apollorno-0.livy-apollorno-hs.zeta-prod-ns.svc.57.tess.io.").host);
  }

  public static String trimDot(String str) {
   return str.charAt(str.length() -1) == '.' ? str.substring(0, str.length() - 1) : str;
  }

  public static String reverseArpaAddr(String address) {
    String trimmed = address.substring(0, address.length() - ".in-addr.arpa.".length());
    String[] split = trimmed.split("\\.");
    StringBuilder sb = new StringBuilder();
    for (int i = split.length -1 ; i >= 0; i--) {
      sb.append(split[i]);
      if (i != 0) {
        sb.append(".");
      }
    }
    return sb.toString();
  }
}
