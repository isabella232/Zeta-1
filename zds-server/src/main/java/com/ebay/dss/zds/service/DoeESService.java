package com.ebay.dss.zds.service;

import com.ebay.dss.zds.dao.notebook.meta.NotebookMetaRepository;
import com.ebay.dss.zds.exception.NotebookException;
import com.ebay.dss.zds.model.notebook.meta.DoeNotebookMeta;
import com.ebay.dss.zds.model.notebook.meta.DoeNotebookMetaProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class DoeESService {
    @Autowired
    NotebookMetaRepository repository;

    @Resource(name = "error-handle-rest-template")
    private RestTemplate restTemplate;

    @Value("${zds.notebookmeta.url}")
    private String url;

    public void deleteDOEMeta(String id) {
        ResponseEntity<Map> responseEntity = restTemplate.getForEntity(url + "delete?obj=query&id=" + id, Map.class);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new NotebookException("Failed to reach DOE service");
        }
    }
    public void batchDeleteDOEMeta(List<String> ids) {
        ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url + "deleteList?obj=query", ids.toArray(), Map.class);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new NotebookException("Failed to reach DOE service");
        }
    }
    public void updateDOEMeta(String id) {
        DoeNotebookMetaProjection projection = repository.findDoeMetaById(id);
        DoeNotebookMeta entity = DoeNotebookMeta.fromProjection(projection);
        ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url + "push?obj=query&id=" + id, entity, Map.class);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new NotebookException("Failed to reach DOE service");
        }
    }

}
