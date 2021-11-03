package com.ebay.dss.zds.service;

import com.ebay.dss.zds.dao.ZetaNotebookRepository;
import com.ebay.dss.zds.dao.notebook.meta.NotebookMetaRepository;
import com.ebay.dss.zds.exception.NotebookException;
import com.ebay.dss.zds.model.ZetaNotebook;
import com.ebay.dss.zds.model.ZetaNotebookPreference;
import com.ebay.dss.zds.model.notebook.meta.NotebookMeta;
import com.ebay.dss.zds.model.notebook.ZetaNotebookSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ZetaNotebookMetaService {
    @Autowired
    NotebookMetaRepository repository;

    @Autowired
    ZetaNotebookRepository notebookRepository;


    @Resource(name = "error-handle-rest-template")
    private RestTemplate restTemplate;

    @Value("${zds.doeservice.url}")
    private String url;

    public List<ZetaNotebookSummary> getNotebookSummaryByIds(List<String> ids) {
        return repository.findSummaryByIds(ids);
    }
    public NotebookMeta getNotebookMetaById(String id) {
        NotebookMeta notebookMeta = repository.findOneById(id);
        return notebookMeta;
    }

    public boolean hasMeta(String id) {
        Optional<NotebookMeta> meta = repository.findById(id);
        return meta.isPresent();
    }
    @Transactional
    public void batchDeleteNotebookMeta(List<String> ids) {
        List<NotebookMeta> metas = repository.findAllById(ids);
        repository.deleteInBatch(metas);
//        batchDeleteDOEMeta(ids);
    }

    public void deleteNotebookMeta(String id) {
        if (hasMeta(id)) {
            repository.deleteById(id);
//            deleteDOEMeta(id);
        }
    }

    public NotebookMeta createOrUpdateNotebookMeta(NotebookMeta meta) {
        NotebookMeta oldMeta = repository.findOneById(meta.getId());
        mergeNotebookMeta(meta, oldMeta);
        repository.save(meta);
        return meta;
    }

    public String parseNotebookReference(ZetaNotebook notebook) {
        ZetaNotebookPreference preference = ZetaNotebookPreference.fromJson(notebook.getPreference());
        String platform = getPlatform(preference);
        String refs = "";
        try {
            refs = fetchDependencies(notebook.getContent(), platform);
        } catch (NotebookException e) {

        }
        return refs;
    }

    private String getPlatform(ZetaNotebookPreference preference) {
        if (preference != null && preference.notebookConnection != null) {
            Map<String, Object> cnn = preference.notebookConnection;
            if (cnn.get("codeType").equals("TERADATA")) {
                return "td";
            }
        }
        return "spark";
    }
    private String fetchDependencies(String query, String platform) {
        String dependencies;
        ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url + "dependency/parseSourceTransform?type=" + platform, query, Map.class);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new NotebookException("Failed to reach DOE service");
        }
        dependencies = (String) responseEntity.getBody().get("value");
        return dependencies;
    }

    private void mergeNotebookMeta(NotebookMeta target, NotebookMeta val) {
        if (val == null) {
            return;
        }
        if (target.getReference() == null) {
            target.setReference(val.getReference());
        }
    }
}
