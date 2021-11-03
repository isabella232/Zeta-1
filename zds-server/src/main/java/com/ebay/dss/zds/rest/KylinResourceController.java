package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.model.KylinReadableList;
import com.ebay.dss.zds.model.KylinReadableReq;
import com.ebay.dss.zds.model.ZetaResponse;
import org.codehaus.plexus.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.Charset;

@RestController
@RequestMapping("/kylin")
public class KylinResourceController {

    @Autowired
    @Qualifier("kylin-template")
    private RestTemplate rest;

    @PostMapping("/readable_projects")
    public ZetaResponse<KylinReadableList> fetchKylinReadable(
            @RequestBody KylinReadableReq req) {
        String auth = req.getUser() + ":" + req.getPassword();
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(Charset.forName("US-ASCII")));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headers.set("Authorization", "Basic " + new String(encodedAuth));

        String url = (req.isSsl()?"https":"http") + "://" + req.getHost() + "/kylin/api/projects/readable";
        RequestEntity<Void> kylinReq = new RequestEntity<>(null, headers, HttpMethod.GET, URI.create(url));
        ResponseEntity<KylinReadableList> kylinResp = rest.exchange(kylinReq, KylinReadableList.class);

        return new ZetaResponse<>(kylinResp.getBody(), kylinResp.getStatusCode());
    }
}
