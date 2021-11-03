package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.dao.ZetaJobRequestRepository;
import com.ebay.dss.zds.model.ZetaJobRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/requests")
public class ZetaJobRequestController {

    @Autowired
    private ZetaJobRequestRepository zetaRequestRepository;

    @RequestMapping(value="/{id}", method = RequestMethod.GET)
    public ZetaResponse<ZetaJobRequest> getZetaJobRequest(@PathVariable("id") int id){
        return new ZetaResponse(zetaRequestRepository.getZetaJobRequest(id), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ZetaResponse<List<ZetaJobRequest>> getAllZetaJobRequests(){
        return new ZetaResponse(zetaRequestRepository.getAllZetaJobRequests(), HttpStatus.OK);
    }

    @RequestMapping(value="/notebook/{id}", method = RequestMethod.GET)
    public ZetaResponse<List<ZetaJobRequest>> getZetaJobRequestByNotebook(@PathVariable("id") String notebookId){
        return new ZetaResponse<>(zetaRequestRepository.getZetaJobRequestsByNotebook(notebookId), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST)
    public ZetaResponse<ZetaJobRequest> addZetaJobRequest(@RequestBody ZetaJobRequest request){
        zetaRequestRepository.addZetaJobRequest(request);
        return new ZetaResponse(request, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT)
    public ZetaResponse<ZetaJobRequest> updateZetaJobRequest(@RequestBody ZetaJobRequest request){
        zetaRequestRepository.updateZetaJobRequest(request);
        return new ZetaResponse(request, HttpStatus.OK);
    }

    @RequestMapping(value="/{id}/state/{state}", method = RequestMethod.PUT)
    public ZetaResponse updateZetaJobRequestStateById(@PathVariable("id") int id, @PathVariable("state") String state){
        zetaRequestRepository.updateZetaJobRequestStateById(id, state);
        return new ZetaResponse(id, HttpStatus.OK);
    }

    @RequestMapping(value="/{id}", method = RequestMethod.DELETE)
    public ZetaResponse deleteZetaJobRequest(@PathVariable("id") int id) {
        zetaRequestRepository.deleteZetaJobRequest(id);
        return new ZetaResponse(id, HttpStatus.OK);
    }

    @RequestMapping(value="/notebook/{id}", method = RequestMethod.DELETE)
    public ZetaResponse deleteZetaRequestByNotebook(@PathVariable("id") String notebookId){
        zetaRequestRepository.deleteZetaJobRequestByNotebook(notebookId);
        return new ZetaResponse(notebookId, HttpStatus.OK);
    }
}
