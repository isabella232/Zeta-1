package com.ebay.dss.zds.common;

import com.ebay.dss.zds.model.EbayRealm;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Objects;

public class EbayRealmUtils {

    /**
     * Try to extract realm from principal
     * @param spn example: user/host@REALM.COM
     * @return REALM.COM related enum or null if not match
     */
    public static EbayRealm realmOf(String spn) {
        String[] serviceRealm = StringUtils.split(spn, '@');
        if (Objects.nonNull(serviceRealm) && serviceRealm.length == 2) {
            return Arrays.stream(EbayRealm.values())
                    .filter(r -> StringUtils.equals(r.value, serviceRealm[1]))
                    .findFirst()
                    .orElse(null);
        } else {
            return null;
        }
    }
}
