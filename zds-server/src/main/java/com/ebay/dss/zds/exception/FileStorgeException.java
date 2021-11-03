package com.ebay.dss.zds.exception;

public class FileStorgeException extends ApplicationBaseException {

  public FileStorgeException(String message) {
    super(ErrorCode.FILESTORAGE_EXCEPTIOM, message);
  }

  public FileStorgeException(String message, Exception ex) {
    super(ErrorCode.FILESTORAGE_EXCEPTIOM, message, ex);
  }

}
