package com.ruoran.houyi.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
public class HttpClientUtil {

    private static PoolingHttpClientConnectionManager cm;
    private static String EMPTY_STR = "";
    private static String UTF_8 = "UTF-8";

    private static void init() {
        if (cm == null) {
            cm = new PoolingHttpClientConnectionManager();
            // 整个连接池最大连接数
            cm.setMaxTotal(100);
            // 每路由最大连接数，默认值是2
            cm.setDefaultMaxPerRoute(5);
        }
    }

    /**
     * 通过连接池获取HttpClient
     *
     * @return
     */
    private static CloseableHttpClient getHttpClient() {
        init();
        return HttpClients.custom().setConnectionManager(cm).build();
    }

    public static String doPost(String url, String body) {
        HttpClient httpClient = getHttpClient();
        HttpPost httpPost = null;
        String result = null;
        try {
            httpPost = new HttpPost(url);
            //设置参数
            StringEntity entity = new StringEntity(body, ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            HttpResponse response = httpClient.execute(httpPost);
            if (response != null) {
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    result = EntityUtils.toString(resEntity);
                }
            }
        } catch (Exception ex) {
            log.error("HTTP POST请求失败, url:{}", url, ex);
        }
        return result;
    }

    public static String doPost(String url, Map<String, String> map, String charset) {
        //HttpClient httpClient = null;
        CloseableHttpClient httpclient = getHttpClient();
        CloseableHttpResponse response = null;
        HttpPost httpPost = null;
        String result = null;
        try {
            //httpClient = new SSLClient();
            httpPost = new HttpPost(url);
            //设置参数
            List<NameValuePair> list = new ArrayList<NameValuePair>();
            Iterator iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> elem = (Map.Entry<String, String>) iterator.next();
                list.add(new BasicNameValuePair(elem.getKey(), elem.getValue()));
            }
            if (list.size() > 0) {
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list, charset);
                httpPost.setEntity(entity);
            }
            response = httpclient.execute(httpPost);
            if (response != null) {
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    result = EntityUtils.toString(resEntity, charset);
                }
            }
        } catch (Exception ex) {
            log.error("HTTP POST请求失败, url:{}", url, ex);
        }finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                log.error("关闭HTTP响应失败", e);
            }
        }
        return result;
    }

    public static String doGet(String url) {
        return doGet(url, null);
    }

    public static String doGet(String url, String charset) {
        if (null == charset) {
            charset = "utf-8";
        }
        CloseableHttpClient httpclient = getHttpClient();
        CloseableHttpResponse response = null;
        HttpGet httpGet = null;
        String result = null;

        try {
            //httpClient = new SSLClient();
            httpGet = new HttpGet(url);

            response = httpclient.execute(httpGet);
            if (response != null) {
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    result = EntityUtils.toString(resEntity, charset);
                }
            }
        } catch (Exception e) {
            log.error("HTTP GET请求失败, url:{}", url, e);
        }finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                log.error("关闭HTTP响应失败", e);
            }
        }
        return result;
    }
}
