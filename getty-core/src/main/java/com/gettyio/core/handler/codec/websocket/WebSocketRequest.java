/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gettyio.core.handler.codec.websocket;


/**
 * WebSocketRequest.java
 *
 * @description:请求信息bean
 * @author:gogym
 * @date:2020/4/9
 * @copyright: Copyright by gettyio.com
 */
class WebSocketRequest {
    /**
     * 请求地址
     */
    private String requestUri;
    /**
     * 机器地址
     */
    private String host;
    /**
     * 源地址
     */
    private String origin;
    /**
     * cookie
     */
    private String cookie;
    /**
     * 是否更新
     */
    private Boolean upgrade = false;
    /**
     * 是否保存链接
     */
    private Boolean connection = false;
    private Long key1;
    private Long key2;
    /**
     * 签名
     */
    private String digest;
    /**
     * 版本，默认为0
     */
    private Integer secVersion = 0;

    public Boolean getConnection() {
        return connection;
    }

    public void setConnection(Boolean connection) {
        this.connection = connection;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Long getKey1() {
        return key1;
    }

    public void setKey1(Long key1) {
        this.key1 = key1;
    }

    public Long getKey2() {
        return key2;
    }

    public void setKey2(Long key2) {
        this.key2 = key2;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public Boolean getUpgrade() {
        return upgrade;
    }

    public void setUpgrade(Boolean upgrade) {
        this.upgrade = upgrade;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public Integer getSecVersion() {
        return secVersion;
    }

    public void setSecVersion(Integer secVersion) {
        this.secVersion = secVersion;
    }

}
