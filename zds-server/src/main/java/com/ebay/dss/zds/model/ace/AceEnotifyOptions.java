package com.ebay.dss.zds.model.ace;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


public class AceEnotifyOptions {

    private String user;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate after;
    private Set<Product> products;
    private Type type;

    public AceEnotifyOptions() {
    }

    public String getUser() {
        return user;
    }

    public AceEnotifyOptions setUser(String user) {
        this.user = user;
        return this;
    }

    public LocalDate getAfter() {
        return after;
    }

    public AceEnotifyOptions setAfter(LocalDate after) {
        this.after = after;
        return this;
    }

    public Set<Product> getProducts() {
        return products;
    }

    public AceEnotifyOptions setProducts(Set<Product> products) {
        this.products = products;
        return this;
    }

    public Type getType() {
        return type;
    }

    public AceEnotifyOptions setType(Type type) {
        this.type = type;
        return this;
    }

    public enum Product {
        Zeta(2410),
        Hermes_RNO(2428),
        Apollo_RNO(2376),
        Hercules(2133),
        Ares(74),
        Kylin(321),
        Hopper(2307),
        Mozart(2308),
        Unknown(-1)
        ;

        private List<Integer> ids;

        Product(Integer... ids) {
            this.ids = Arrays.asList(ids);
        }

        public List<Integer> getIds() {
            return ids;
        }

        @JsonCreator
        public static Product fromInt(int v) {
            for (Product p : Product.values()) {
                if (p.getIds().contains(v)) {
                    return p;
                }
            }
            return Unknown;
        }
    }

    public enum Type {
        GENERAL,
        PRODUCT,
        MAINTENANCE,
        ;
    }
}
