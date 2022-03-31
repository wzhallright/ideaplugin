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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by wangzhehan on 16/3/9.
 */
public class HttpProxy
{
    public static final Logger LOG = LoggerFactory.getLogger(HttpProxy.class);
    static String proxyHost = "0.0.0.0";
    static int proxyPort = 80;

    public static String sendGet(String url, boolean isProxy) {
        try {
            return sendGet(url, isProxy, false);
        } catch (Exception e) {
            LOG.error("发送GET请求出现异常！", e);
        }
        return "";
    }

    public static String sendGet(String url) throws IOException {
        return sendGet(url, false, true);
    }

    /**
     * 向指定URL发送GET方法的请求
     *
     * @param url 发送请求的URL
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendGet(String url, boolean isProxy, boolean isShowUseTime) throws IOException {
        String result = "";
        BufferedReader in = null;
        long start = System.currentTimeMillis();
        try {
            String urlNameString = url;
            URL realUrl = new URL(urlNameString);
            HttpURLConnection connection = null;
            if (isProxy) {
                // 打开和URL之间的连接
                @SuppressWarnings("static-access")
                Proxy proxy = new Proxy(Proxy.Type.DIRECT.HTTP,
                    new InetSocketAddress(proxyHost, proxyPort));
                connection = (HttpURLConnection) realUrl.openConnection(proxy);
            } else {
                connection = (HttpURLConnection) realUrl.openConnection();
            }

            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            // 遍历所有的响应头字段
            /*for (String key : map.keySet()) {
                System.out.println(key + "--->" + map.get(key));
            }*/
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        }
        // 使用finally块来关闭输入流
        finally {
            if (isShowUseTime) {
                LOG.info("http get take time " + (System.currentTimeMillis() - start) + " ms");
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
                LOG.error("BufferedReader close error", e);
            }
        }
        return result;
    }

    public static String sendPost(String url, Map<String, String> param, int timeout) {
        // 将Map类型的参数转换成NameValuePair数组
        Set<String> keys = param.keySet();
        NameValuePair[] data = new NameValuePair[keys.size()];
        int idx = 0;
        for (String key : keys) {
            String value = param.get(key);
            NameValuePair item = new NameValuePair(key, value);
            data[idx] = item;
            idx++;
        }

        PostMethod postMethod = null;
        try {
            HttpClient httpClient = new HttpClient();
            // 设置超时时间
            httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(timeout);
            httpClient.getHttpConnectionManager().getParams().setSoTimeout(timeout);

            postMethod = new PostMethod(url);
            // 设置编码
            postMethod.getParams().setParameter(
              HttpMethodParams.HTTP_CONTENT_CHARSET,
              StandardCharsets.UTF_8.name());

            // 将值放入postMethod中
            postMethod.setRequestBody(data);

            // 执行postMethod
            int statusCode = httpClient.executeMethod(postMethod);
            if (statusCode == HttpStatus.SC_OK) {
                BufferedReader br = new BufferedReader(
                  new InputStreamReader(postMethod.getResponseBodyAsStream(), "UTF-8"));
                String line;
                StringBuilder contentBuffer = new StringBuilder();
                try {
                    while ((line = br.readLine()) != null) {
                        contentBuffer.append(line);
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("JD RWMonitor 调用 HTTP POST 接口成功: " + contentBuffer.toString());
                    }
                } finally {
                    if (br != null) {
                        br.close();
                    }
                    return contentBuffer.toString();
                }
            } else {
                LOG.warn("HTTP POST 请求[" + url + "]失败!返回状态为:" + statusCode);
            }
        } catch (Exception e) {
            LOG.warn("HTTP POST 请求[" + url + "]异常: " + e.getMessage());
        } finally {
            if (postMethod != null) {
                postMethod.releaseConnection();
            }
        }
        return null;
    }
}
