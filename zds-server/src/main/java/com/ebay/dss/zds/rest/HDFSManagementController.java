package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.model.FileStorageResponse;
import com.ebay.dss.zds.rest.annotation.AuthenticationNT;
import com.ebay.dss.zds.service.hdfs.HDFSManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Created by tatian on 2019-12-09.
 */
@RestController
@RequestMapping("/hdfs")
public class HDFSManagementController {

  @Value("${zds.dump.response.buffer-size: #{64*1024}}")
  private int dumpBufferSize = 64 * 1024;

  @Autowired
  private HDFSManagementService hdfsManagementService;

  @GetMapping("/read")
  public void read(@AuthenticationNT String nt,
                   @RequestParam("filePath") String filePath,
                   @RequestParam("cluster") int cluster,
                   HttpServletResponse response) throws IOException {
    FileStorageResponse fileStorageResponse = hdfsManagementService.read(nt, filePath, cluster);
    byte[] fileBytes = (byte[]) fileStorageResponse.getContent();
    try (OutputStream out = response.getOutputStream();
         ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes)) {
      int readed = 0;
      byte[] buffer = new byte[dumpBufferSize];
      while (readed >= 0) {
        readed = bis.read(buffer, 0, buffer.length);
        if (readed > 0) {
          out.write(buffer, 0, readed);
        }
      }
    }
  }

  @PutMapping("/create")
  public FileStorageResponse create(@AuthenticationNT String nt,
                                    @RequestParam("filePath") String filePath,
                                    @RequestParam("cluster") int cluster) {
    return hdfsManagementService.create(nt, filePath, cluster);
  }

  @PostMapping("/upload")
  public FileStorageResponse uploadFile(@AuthenticationNT String nt,
                                        @RequestParam("filePath") String filePath,
                                        @RequestParam("fileName") String fileName,
                                        @RequestParam("cluster") int cluster,
                                        @RequestParam(value = "isOverWrite", required = false, defaultValue = "false") boolean isOverWrite,
                                        @RequestParam("file") MultipartFile file) {
    return hdfsManagementService.upload(nt, filePath, fileName, cluster, isOverWrite, file);
  }

  @PutMapping("/rename")
  public FileStorageResponse rename(@AuthenticationNT String nt,
                                    @RequestParam("srcPath") String srcPath,
                                    @RequestParam("dstPath") String dstPath,
                                    @RequestParam("cluster") int cluster) {
    return hdfsManagementService.rename(nt, srcPath, dstPath, cluster);
  }

  @DeleteMapping("/delete")
  public FileStorageResponse delete(@AuthenticationNT String nt,
                                    @RequestParam("filePath") String filePath,
                                    @RequestParam("cluster") int cluster) {
    return hdfsManagementService.delete(nt, filePath, cluster);
  }

  @GetMapping("/list")
  public Map<String, Object> list(@AuthenticationNT String nt, int cluster, String path) {
    return hdfsManagementService.listFolder(nt, path, cluster);
  }
}
