package com.ebay.dss.zds.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.hibernate.validator.constraints.Length;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "zeta_attachment")
public class ZetaAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @Length(min = 1, max = 32)
    private String ContentType;
    @Column
    private int rawSize;
    @Column
    @Length(min = 1, max = 32)
    private String fileName;
    @Column
    @Length(min = 1, max = 16)
    private String fileExtension;
    @Column
    @Length(min = 1, max = 64)
    private String checksum;
    @Column
    @Length(min = 1, max = 128)
    private String originalFileName;
    @Column
    @Length(min = 1, max = 100)
    private String uploader;
    @Column
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private ZonedDateTime createTime;
    @Column
    @Length(max = 32)
    private String compressAlg;
    @Column
    @Length(max = 32)
    private String decompressAlg;
    @Column
    @Length(max = 32)
    private String checksumAlg;

    public String getCompressAlg() {
        return compressAlg;
    }

    public ZetaAttachment setCompressAlg(String compressAlg) {
        this.compressAlg = compressAlg;
        return this;
    }

    public String getDecompressAlg() {
        return decompressAlg;
    }

    public ZetaAttachment setDecompressAlg(String decompressAlg) {
        this.decompressAlg = decompressAlg;
        return this;
    }

    public String getChecksumAlg() {
        return checksumAlg;
    }

    public ZetaAttachment setChecksumAlg(String checksumAlg) {
        this.checksumAlg = checksumAlg;
        return this;
    }

    public int getRawSize() {
        return rawSize;
    }

    public ZetaAttachment setRawSize(int size) {
        this.rawSize = size;
        return this;
    }

    public String getChecksum() {
        return checksum;
    }

    public ZetaAttachment setChecksum(String checksum) {
        this.checksum = checksum;
        return this;
    }

    public Long getId() {
        return id;
    }

    public ZetaAttachment setId(Long id) {
        this.id = id;
        return this;
    }

    public String getContentType() {
        return ContentType;
    }

    public ZetaAttachment setContentType(String contentType) {
        ContentType = contentType;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public ZetaAttachment setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public ZetaAttachment setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
        return this;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public ZetaAttachment setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
        return this;
    }

    public String getUploader() {
        return uploader;
    }

    public ZetaAttachment setUploader(String uploader) {
        this.uploader = uploader;
        return this;
    }

    public ZonedDateTime getCreateTime() {
        return createTime;
    }

    public ZetaAttachment setCreateTime(ZonedDateTime createTime) {
        this.createTime = createTime;
        return this;
    }
}
