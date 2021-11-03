package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.model.ZetaAttachment;
import com.ebay.dss.zds.model.ZetaAttachmentContent;
import com.ebay.dss.zds.model.ZetaAttachmentPage;
import com.ebay.dss.zds.rest.annotation.AuthenticationNT;
import com.ebay.dss.zds.service.ZetaAttachmentService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/attachments")
public class ZetaAttachmentController {

    private static final Logger LOGGER = LogManager.getLogger();
    private ZetaAttachmentService attachmentService;

    public ZetaAttachmentController(ZetaAttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping
    public ZetaAttachmentPage upload(@AuthenticationNT String nt,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return attachmentService.list(nt, page, size);
    }

    @PostMapping
    public Object upload(@AuthenticationNT String nt,
                         @RequestParam("file") MultipartFile file) throws IOException, NoSuchAlgorithmException {
        return attachmentService.upload(nt, file);
    }


    @GetMapping("/{file:.+}")
    public void download(@PathVariable String file, HttpServletResponse response) throws IOException {
        LOGGER.info("Download file {}", file);
        ZetaAttachment attachment = attachmentService.getAttachment(file);
        ZetaAttachmentContent content = attachmentService.getContent(attachment);
        response.setContentLength(attachment.getRawSize());
        response.setContentType(attachment.getContentType());
        String fullFileName = attachment.getFileName() + "." + attachment.getFileExtension();
        response.setHeader("Content-disposition", "attachment; filename=" + fullFileName);

        OutputStream out = response.getOutputStream();
        FileCopyUtils.copy(content.getInputStream(), out);
        LOGGER.info("Downloaded file {}", file);
    }

    @DeleteMapping("/{file:.+}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationNT String nt,
                       @PathVariable String file) {
        LOGGER.info("Delete file {}", file);
        attachmentService.delete(nt, file);
        LOGGER.info("Deleted file {}", file);
    }

}
