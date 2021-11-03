package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.dao.ace.AlationArticleRepository;
import com.ebay.dss.zds.dao.ace.AlationQueryRepository;
import com.ebay.dss.zds.model.ace.AlationArticle;
import com.ebay.dss.zds.model.ace.AlationQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@RestController
@RequestMapping("/ace/alation")
public class AceAlationController {

    private final AlationQueryRepository alationQueryRepository;
    private final AlationArticleRepository alationArticleRepository;

    public AceAlationController(AlationQueryRepository alationQueryRepository,
                                AlationArticleRepository alationArticleRepository) {
        this.alationQueryRepository = alationQueryRepository;
        this.alationArticleRepository = alationArticleRepository;
    }

    private String getAlationURL(HttpServletRequest request) {
        return request.getServletPath()
                .replaceAll("/ace/alation", "");
    }

    @GetMapping("/query/**")
    public Optional<AlationQuery> query(HttpServletRequest request) {
        String url = getAlationURL(request);
        return alationQueryRepository.findOneByUrl(url);
    }

    @GetMapping("/article/**")
    public Optional<AlationArticle> article(HttpServletRequest request) {
        String url = getAlationURL(request);
        return alationArticleRepository.findOneByUrl(url);
    }

}
