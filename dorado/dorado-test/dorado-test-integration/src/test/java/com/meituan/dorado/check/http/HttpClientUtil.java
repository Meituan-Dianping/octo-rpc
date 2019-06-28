/*
 * Copyright 2018 Meituan Dianping. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.meituan.dorado.check.http;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpClientUtil {
    private static Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

    private static CloseableHttpClient httpclient;

    static {
        HttpClientBuilder builder = HttpClients.custom();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(100);
        builder.setConnectionManager(cm);
        httpclient = builder.build();
    }

    public static String doGet(String url, Map<String, String> params) {
        return doGet(url, params, null);
    }

    public static String doGet(String url, Map<String, String> params, Map<String, String> headers) {
        try {
            HttpGet httpget = null;
            if (params != null && params.size() > 0) {
                URIBuilder uri = new URIBuilder(url);
                for (Map.Entry<String, String> param : params.entrySet()) {
                    uri.addParameter(param.getKey(), param.getValue());
                }
                httpget = new HttpGet(uri.build());
            } else {
                httpget = new HttpGet(url);
            }
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    httpget.setHeader(header.getKey(), header.getValue());
                }
            }
            return httpclient.execute(httpget, getResponseHandler());
        } catch (URISyntaxException | IOException e) {
            logger.error("Http GET Fail", e);
        }
        return "";
    }

    public static String doPost(String url, Map<String, String> pairsMap) {
        try {
            HttpPost httppost = new HttpPost(url);
            if (pairsMap != null && pairsMap.size() > 0) {
                List<BasicNameValuePair> pairs = new ArrayList<BasicNameValuePair>();
                for (String key : pairsMap.keySet()) {
                    pairs.add(new BasicNameValuePair(key, pairsMap.get(key)));
                }
                httppost.setEntity(new UrlEncodedFormEntity(pairs));
            }
            return httpclient.execute(httppost, getResponseHandler());
        } catch (ClientProtocolException cpe) {
            logger.error("Http POST Fail", cpe);
        } catch (IOException ioe) {
            logger.error("Http POST Fail", ioe);
        }
        return "";
    }

    public static String doPost(String url, String paramStr) {
        return doPost(url, paramStr, null);
    }

    public static String doPost(String url, String paramStr, Map<String, String> headers) {
        try {
            HttpPost httppost = new HttpPost(url);
            StringEntity str = new StringEntity(paramStr, ContentType.APPLICATION_JSON);
            httppost.setEntity(str);
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    httppost.setHeader(header.getKey(), header.getValue());
                }
            }
            return httpclient.execute(httppost, getResponseHandler());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return "";
    }

    public static String doPostWithAppkey(String url, String paramStr, String remoteAppkey) {
        try {
            HttpPost httppost = new HttpPost(url);
            StringEntity str = new StringEntity(paramStr, ContentType.APPLICATION_JSON);
            httppost.setEntity(str);
            return httpclient.execute(httppost, getResponseHandler());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return "";
    }

    public static String doPut(String url, String params, Map<String, String> headers) {
        try {
            HttpPut httpput = new HttpPut(url);
            StringEntity str = new StringEntity(params);
            httpput.setEntity(str);
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    httpput.addHeader(header.getKey(), header.getValue());
                }
            }
            return httpclient.execute(httpput, getResponseHandler());
        } catch (ClientProtocolException cpe) {
            logger.error("Http PUT Fail", cpe);
        } catch (IOException ioe) {
            logger.error("Http PUT Fail", ioe);
        }
        return "";
    }

    private static ResponseHandler<String> getResponseHandler() {
        return new ResponseHandler<String>() {
            @Override
            public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity, "UTF-8") : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }
        };
    }

    public static void shutDown() {
        try {
            httpclient.close();
        } catch (Exception e) {
            logger.error("Http client close fail", e);
        }
    }
}
