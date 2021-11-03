package com.ebay.dss.zds.service.ace;

import com.ebay.dss.zds.dao.ace.AceEnotifyReadRepository;
import com.ebay.dss.zds.model.ace.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AceENotifySimpleService implements AceENotifyService {

    private static final Logger LOGGER = LogManager.getLogger();
    private final AceEnotifyRead fallbackRead = new AceEnotifyRead();
    private final URI url;
    private final RestTemplate doeTemplate;
    private final AceEnotifyReadRepository enotifyReadRepository;

    public AceENotifySimpleService(@Qualifier("resttemplate") RestTemplate doeTemplate,
                                   AceEnotifyReadRepository enotifyReadRepository,
                                   @Value("${zds.enotify.service.url}") String url) {
        this.doeTemplate = doeTemplate;
        this.enotifyReadRepository = enotifyReadRepository;
        LOGGER.info("Use enotify source url: " + url);
        this.url = URI.create(url);
    }

    @Override
    public AceEnotifyOptions.Product[] getAllEnotifyProducts() {
        return AceEnotifyOptions.Product.values();
    }

    @Override
    public AceEnotifies findEnotifies(AceEnotifyOptions options) {
        AceEnotifies enotifies = new AceEnotifies();
        try {
            List<AceEnotify> enotifiesTmp = findEnotifiesFromDoe(options);
            enotifies.setEnotifies(enotifiesTmp);
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return enotifies;
    }

    private List<AceEnotify> findEnotifiesFromDoe(AceEnotifyOptions options) {
        List<DoeEnotify> doeEnotifies = findDoeEnotifyFromDoe(options);
        if (doeEnotifies.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> eIds = doeEnotifies.stream()
                .map(DoeEnotify::getId)
                .collect(Collectors.toList());
        List<AceEnotifyRead> reads = enotifyReadRepository.findAllByEnotifyIdInAndNt(eIds, options.getUser());
        Map<Long, AceEnotifyRead> readMap = reads.stream()
                .collect(Collectors.toMap(AceEnotifyRead::getEnotifyId, Function.identity()));
        return doeEnotifies.stream()
                .map(e -> new AceEnotify()
                        .setDoeEnotify(e)
                        .setEnotifyRead(readMap.getOrDefault(e.getId(), getFallbackRead())))
                .collect(Collectors.toList());
    }

    private List<DoeEnotify> findDoeEnotifyFromDoe(AceEnotifyOptions options) {

        Object request = options2Request(options);
        DoeEnotify.DoeEnotifies respBody = doeTemplate.postForObject(url, request, DoeEnotify.DoeEnotifies.class);
        if (respBody != null &&
                respBody.getData() != null &&
                respBody.getData().getValue() != null) {
            List<DoeEnotify> doeEnotifies = respBody.getData().getValue();
            return doeEnotifies;
        }
        return Collections.emptyList();
    }

    private AceEnotifyRead getFallbackRead() {
        return fallbackRead;
    }

    private Object options2Request(AceEnotifyOptions options) {
        Map<String, Object> objectMap = new HashMap<>();
        Set<Integer> productIds = options.getProducts().stream()
                .flatMap(p -> p.getIds().stream())
                .collect(Collectors.toSet());
        objectMap.put("prodList", productIds);
        if (options.getAfter() != null) {
            objectMap.put("after", options.getAfter().toString());
        }
        if (options.getType() != null) {
            objectMap.put("type", options.getType());
        }
        return objectMap;
    }

    @Override
    public boolean setReadEnotifies(AceEnotifyReadOptions options) {
        try {
            return setReadEnotifies2Db(options);
        } catch (Exception e) {
            LOGGER.error("Set read of Enotifies error", e);
            return false;
        }
    }

    @Override
    public AceEnotifyOptions.Type[] getAllEnotifyTypes() {
        return AceEnotifyOptions.Type.values();
    }

    private boolean setReadEnotifies2Db(AceEnotifyReadOptions options) {

        Set<Long> eIds = options.getEnotifyIds();
        ZonedDateTime readTime = ZonedDateTime.now(ZoneId.of("UTC"));
        if (eIds == null || eIds.isEmpty()) {
            return true;
        }
        if (StringUtils.isBlank(options.getUser())) {
            return false;
        }
        List<AceEnotifyRead> readList = eIds.stream()
                .map(id -> new AceEnotifyRead()
                        .setEnotifyId(id)
                        .setNt(options.getUser())
                        .setRead(options.isRead())
                        .setReadTime(readTime))
                .collect(Collectors.toList());
        readList = enotifyReadRepository.saveAll(readList);
        return readList.size() == eIds.size();
    }
}
