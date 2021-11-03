package com.ebay.dss.zds.service.ace;

import com.ebay.dss.zds.model.ace.AceSearchEntry;
import com.ebay.dss.zds.model.ace.AceSearchOptions;

import java.util.Collection;
import java.util.Optional;

public interface AceSearchService {

    Optional<AceSearchEntry> search(AceSearchOptions options);

    Collection<String> scopes();
}
