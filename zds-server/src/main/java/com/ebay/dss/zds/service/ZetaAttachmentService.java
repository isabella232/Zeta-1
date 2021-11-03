package com.ebay.dss.zds.service;

import com.ebay.dss.zds.dao.ZetaAttachmentContentRepository;
import com.ebay.dss.zds.dao.ZetaAttachmentRepository;
import com.ebay.dss.zds.model.ZetaAttachment;
import com.ebay.dss.zds.model.ZetaAttachmentContent;
import com.ebay.dss.zds.model.ZetaAttachmentPage;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.zip.DeflaterInputStream;
import java.util.zip.InflaterInputStream;

@Service
public class ZetaAttachmentService {


    public static final String DECOMPRESS_INFLATER = "inflater";
    public static final String COMPRESS_DEFLATER = "deflater";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Long MAX_RAW_ATTACHMENT_SIZE = 2L << 21;
    private static final String MAX_RAW_ATTACHMENT_SIZE_STRING = "4MB";
    private static final String DEFAULT_CHECKSUM_ALG = "SHA-256";
    private static final Set<String> supportedImageTypes = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    MediaType.IMAGE_JPEG_VALUE,
                    MediaType.IMAGE_PNG_VALUE
            )));
    private static final Set<String> supportedTextTypes = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    MediaType.TEXT_HTML_VALUE,
                    MediaType.TEXT_MARKDOWN_VALUE,
                    MediaType.TEXT_PLAIN_VALUE,
                    "text/csv"
            )));
    private ZetaAttachmentRepository repository;
    private ZetaAttachmentContentRepository contentRepository;

    public ZetaAttachmentService(ZetaAttachmentRepository repository,
                                 ZetaAttachmentContentRepository contentRepository) {
        this.repository = repository;
        this.contentRepository = contentRepository;
    }

    private static void checkSupportedContentType(String contentType) {
        if (supportedImageTypes.contains(contentType) ||
                supportedTextTypes.contains(contentType)) {
            return;
        }
        throw new RuntimeException("Invalid Content Type " + contentType);
    }

    private static void checkRawFileSize(long size) {
        if (size > MAX_RAW_ATTACHMENT_SIZE) {
            throw new RuntimeException("attachment is over " + MAX_RAW_ATTACHMENT_SIZE_STRING);
        }
    }

    private static String getContentType(MultipartFile file, ZetaAttachment attachment) {
        String contentType = file.getContentType();
        String typeByName = URLConnection.guessContentTypeFromName(file.getOriginalFilename());
        String typeByBytes = null;
        try (InputStream is = file.getInputStream()) {
            typeByBytes = URLConnection.guessContentTypeFromStream(is);
        } catch (IOException e) {
            LOGGER.warn("Read Content from input stream error, {}", e.getMessage());
        }
        if (typeByBytes != null) {
            return typeByBytes;
        } else if (typeByName != null) {
            return typeByName;
        } else {
            return contentType;
        }
    }

    private static String genFileName() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String checksum(MultipartFile file, ZetaAttachment attachment) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance(attachment.getChecksumAlg());
        byte[] hash;
        try (InputStream is = file.getInputStream();
             DigestInputStream dis = new DigestInputStream(is, md)) {
            byte[] buffer = new byte[64 * 1024];
            while (dis.read(buffer) > -1) {
                // do nothing
            }
            hash = md.digest();
        }
        if (hash == null) {
            return null;
        }
        StringBuilder hexBuilder = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexBuilder.append(0);
            }
            hexBuilder.append(hex);
        }
        return hexBuilder.toString();
    }

    public ZetaAttachmentPage list(String nt, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<ZetaAttachment> tmp = repository.findAllByUploader(nt, pageable);
        return new ZetaAttachmentPage()
                .setAttachments(tmp.getContent())
                .setSize(tmp.getNumberOfElements())
                .setSorted(tmp.getSort().isSorted())
                .setTotalPage(tmp.getTotalPages())
                .setTotalSize(tmp.getTotalElements());
    }

    public Object upload(String nt, MultipartFile file) throws IOException, NoSuchAlgorithmException {
        ZetaAttachment attachment = new ZetaAttachment()
                .setFileName(genFileName())
                .setCreateTime(ZonedDateTime.now(ZoneId.of("UTC")))
                .setUploader(nt)
                .setOriginalFileName(file.getOriginalFilename())
                .setCompressAlg(COMPRESS_DEFLATER)
                .setDecompressAlg(DECOMPRESS_INFLATER)
                .setChecksumAlg(DEFAULT_CHECKSUM_ALG);
        checkRawFileSize(file.getSize());
        attachment.setRawSize((int) file.getSize());
        String contentType = getContentType(file, attachment);
        checkSupportedContentType(file.getContentType());
        attachment.setContentType(contentType);
        attachment.setFileExtension(FilenameUtils.getExtension(file.getOriginalFilename()));
        String checksum = checksum(file, attachment);
        attachment.setChecksum(checksum);
        attachment = repository.save(attachment);
        try (InputStream is = file.getInputStream()) {
            ZetaAttachmentContent content = new ZetaAttachmentContent()
                    .setId(attachment.getId())
                    .setInputStream(is);
            content = compressInputStream(attachment.getCompressAlg(), content);
            int res = contentRepository.save(content);
            if (res != 1) {
                throw new RuntimeException("Attachment persist failed");
            }
        }
        return attachment;
    }

    private Optional<ZetaAttachment> from(String file) {
        String name = FilenameUtils.getBaseName(file);
        return repository.findByFileName(name);
    }

    public ZetaAttachment getAttachment(String file) {
        ZetaAttachment attachment = from(file).orElse(null);
        if (attachment == null) {
            throw new RuntimeException("File doesnt's exist");
        }
        return attachment;
    }

    public ZetaAttachmentContent getContent(ZetaAttachment attachment) throws IOException {
        ZetaAttachmentContent content = contentRepository.findContent(attachment.getId()).orElse(null);
        if (content == null || content.getInputStream() == null) {
            throw new RuntimeException("File doesnt's exist");
        }
        return decompressInputStream(attachment.getDecompressAlg(), content);
    }

    public void delete(String nt, String file) {
        String name = FilenameUtils.getBaseName(file);
        repository.deleteByFileNameAndUploader(name, nt);
    }

    public ZetaAttachmentContent compressInputStream(String alg, ZetaAttachmentContent content) throws IOException {
        if (alg == null) {
            return content;
        }
        switch (alg) {
            case COMPRESS_DEFLATER:
                return content.setInputStream(new DeflaterInputStream(content.getInputStream()));
            default:
                return content;
        }
    }

    public ZetaAttachmentContent decompressInputStream(String alg, ZetaAttachmentContent content) {
        if (alg == null) {
            return content;
        }
        switch (alg) {
            case DECOMPRESS_INFLATER:
                return content.setInputStream(new InflaterInputStream(content.getInputStream()));
            default:
                return content;
        }
    }
}
