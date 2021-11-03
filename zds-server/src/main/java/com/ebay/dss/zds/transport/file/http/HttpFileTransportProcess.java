package com.ebay.dss.zds.transport.file.http;

import com.ebay.dss.zds.transport.TransportProcess;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpData;

/**
 * Created by tatian on 2019-10-30.
 */
public class HttpFileTransportProcess implements TransportProcess<HttpRequest, HttpData, HttpUploadServerHandler.FileUploadWrap> {

  public void beforeTransport(HttpRequest httpRequest) {}

  public void onTransport(HttpData fileUpload) {}

  public void afterTransport(HttpUploadServerHandler.FileUploadWrap fileUpload) {}
}
