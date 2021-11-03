package com.ebay.dss.zds.rest;


import com.ebay.dss.zds.model.FileStorageResponse;
import com.ebay.dss.zds.rest.annotation.AuthenticationNT;
import com.ebay.dss.zds.service.ZetaPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/package")
public class ZetaPackageController {

  @Autowired
  private ZetaPackageService zetaPackageService;

  @PostMapping("/upload")
  public FileStorageResponse uploadFile(@AuthenticationNT String nt,
                                        @RequestParam("fileType") String fileType,
                                        @RequestParam("filePath") String filePath,
                                        @RequestParam("fileName") String fileName,
                                        @RequestParam("cluster") int cluster,
                                        @RequestParam(value = "isOverWrite", required = false, defaultValue = "false") boolean isOverWrite,
                                        @RequestParam("file") MultipartFile file) {
    /*
    if (!FileType.isFileSupport(fileType)) {
      return new FileStorageResponse(FileStorageResponse.FAIL_CODE, String.format("File Type %s is not support", fileType));
    }
    */
    return zetaPackageService.uploadPackage(nt, fileType, filePath, fileName, cluster, isOverWrite, file);

  }

  @PutMapping("/{id}/rename")
  public FileStorageResponse renamePackage(@AuthenticationNT String nt,
                                           @PathVariable("id") Long id,
                                           @RequestParam("newName") String newName) {
    return zetaPackageService.renamePackage(nt, id, newName);
  }

  @DeleteMapping("/{id}")
  public FileStorageResponse deletePackage(@AuthenticationNT String nt,
                                           @PathVariable("id") Long id) {
    return zetaPackageService.deletePackage(nt, id);
  }

  @GetMapping("/list")
  public Map<String, Object> getPackages(@AuthenticationNT String nt, int cluster, String path) {
    return zetaPackageService.getHDFSFolderList(nt, cluster, path);
  }
}
