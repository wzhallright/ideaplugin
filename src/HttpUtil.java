/*
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

import static com.intellij.psi.impl.source.resolve.reference.impl.providers.TypeOrElementOrAttributeReference.ReferenceType.TypeReference;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtil
{
    public static final Logger LOG = LoggerFactory.getLogger(HttpUtil.class);
    public static boolean isTest = false;

    public static Map<String, String> sendGetContent(String urlStr)
        throws IOException {
        try {
            return toJson(HttpProxy.sendGet(urlStr));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
    public static  Map<String, String> postJsonContent(String urlStr)
    {
      return postJsonContent(urlStr, isTest);
    }

    public static  Map<String, String> postJsonContent(String urlStr, boolean isProxy)
    {
        System.out.println("urlStr = " + urlStr);
        try {
            return toJson(HttpProxy.sendGet(urlStr, isProxy));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Map<String, String> toJson(String json)
            throws IOException
    {
        Map<String, String> listMap = JSON.parseObject(json, new TypeReference<Map<String, String>>(){});

        return listMap;
    }

    public static String get(String url) {
        //TODO：timeOut configurable
        int timeOut = 30000;
        String responeResult = get(url, timeOut);
        return responeResult;
    }

    public static String get(String url, int timeout) {
        HttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);
        httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, timeout);
        String responeResult = null;
        int responseCode = 0;
        try {
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader("Accept-Charset", "utf-8");
            HttpResponse response = httpClient.execute(httpGet);
            responseCode = response.getStatusLine().getStatusCode();
            if (responseCode == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                responeResult = EntityUtils.toString(entity, "utf8");
            }
        } catch (Exception e) {
            String msg = String.format("Http Request Exception. Code: %s, Url: %s", responseCode, url);
            LOG.error(msg);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        return responeResult;
    }

    public static String sendPost(String url, Map<String, String> param, Integer timeout) {
        Set<String> keys = param.keySet();
        NameValuePair[] data = new NameValuePair[keys.size()];
        int idx = 0;
        for (String key : keys) {
            String value = param.get(key);
            NameValuePair item = new NameValuePair(key, value);
            data[idx] = item;
            idx++;
        }
        StringBuilder contentBuffer = new StringBuilder();
        PostMethod postMethod = null;
        try {
            org.apache.commons.httpclient.HttpClient httpClient = new org.apache.commons.httpclient.HttpClient();
            if (null != timeout) {
                httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(timeout);
                httpClient.getHttpConnectionManager().getParams().setSoTimeout(timeout);
            }

            postMethod = new PostMethod(url);
            postMethod.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, "utf-8");
            postMethod.setRequestBody(data);
            int statusCode = httpClient.executeMethod(postMethod);
            if (statusCode == org.apache.commons.httpclient.HttpStatus.SC_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(postMethod.getResponseBodyAsStream(), "UTF-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    contentBuffer.append(line);
                }
                br.close();
            } else {
                LOG.error("HTTP Request Failure! stateCode:" + statusCode + ", Url: " + url);
            }
        } catch (Exception e) {
            LOG.error("HTTP POST Request Failure! Url: " + url, e);
        } finally {
            if (postMethod != null) {
                postMethod.releaseConnection();
            }
        }
        return contentBuffer.toString();
    }
}
