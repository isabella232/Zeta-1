/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.ebay.dss.zds.transport.file.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Handler that just dumps the contents of the response from the server
 */
public class HttpUploadClientHandler extends SimpleChannelInboundHandler<HttpObject> {

    private final static Logger logger = LoggerFactory.getLogger(HttpUploadClient.class);

    private boolean readingChunks;

    private HttpResponse lastResponse;

    private ExceptionObserver exceptionObserver;

    public HttpUploadClientHandler() {
        this.exceptionObserver = new ExceptionObserver.SimpleExceptionObserver();
    }

    public HttpUploadClientHandler(ExceptionObserver exceptionObserver) {
        this.exceptionObserver = exceptionObserver;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception{
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;

            lastResponse = response;

            if (logger.isDebugEnabled()) {
                if (!response.headers().isEmpty()) {
                    for (CharSequence name : response.headers().names()) {
                        for (CharSequence value : response.headers().getAll(name)) {
                            logger.debug("HEADER: " + name + " = " + value);
                        }
                    }
                }
            }

            if (response.status().code() == 200 && HttpUtil.isTransferEncodingChunked(response)) {
                readingChunks = true;
                logger.debug("CHUNKED CONTENT {");
            } else {
                logger.debug("CONTENT {");
            }
        }
        if (msg instanceof HttpContent) {
            HttpContent chunk = (HttpContent) msg;
            logger.debug(chunk.content().toString(CharsetUtil.UTF_8));

            if (chunk instanceof LastHttpContent) {
                if (readingChunks) {
                    logger.debug("} END OF CHUNKED CONTENT");
                } else {
                    logger.debug("} END OF CONTENT");
                }
                readingChunks = false;
            } else {
                // logger.info(chunk.content().toString(CharsetUtil.UTF_8));
                if (lastResponse != null && lastResponse.status().code() >= 400) {
                    throw new Exception(chunk.content().toString(CharsetUtil.UTF_8));
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // cause.printStackTrace();
        ctx.channel().close();
        exceptionObserver.onException(ctx, cause);
    }
}
