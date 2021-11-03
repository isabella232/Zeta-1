package com.ebay.dss.zds.message;

import com.ebay.dss.zds.dao.ZetaEventRepository;

import javax.validation.constraints.NotNull;

/**
 * Created by tatian on 2019-06-21.
 */
public abstract class TaggedEvent {

    public interface MysqlStorable {

        public long store(ZetaEventRepository zetaEventRepository);

    }

    public interface EmailStorable {

        public String getReceiver();

        public String getSender();

        public String getSubject();

        public String getContent();

    }

    public interface NTTagged {

        @NotNull
        public String getNt();
    }

    public interface InfluxStorable {

        String measurement();

    }
}
