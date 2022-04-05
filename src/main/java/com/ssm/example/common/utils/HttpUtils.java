package com.ssm.example.common.utils;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * http/https 请求工具(支持get,post,put,delete)
 *
 * @author ext.wenzhongming1
 * @version 1.0.0
 * @since 2022/3/9 16:58
 **/

public class HttpUtils {

    private static final RequestConfig REQUEST_CONFIG;
    private static final int THREAD_POOL_MAX_TIMEOUT = 70000;
    public static final String GET = "get";
    public static final String PUT = "put";
    public static final String POST = "post";
    public static final String DELETE = "delete";
    public static final String OPTIONS = "options";
    public static final String TRACE = "trace";
    public static final String HEAD = "head";

    private final Map<String, String> headers = new LinkedHashMap<>();
    private final List<Parameter> params = new ArrayList<>();

    @Getter
    @Setter
    private Object objectParam;

    private final String url;
    private String charset;
    private static final String DEFAULT_CHARSET = StandardCharsets.UTF_8.name();
    private String method;

    @Getter
    @Setter
    private int timeout;
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);

    static {
        // 设置连接池
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        // 设置连接池大小
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(connectionManager.getMaxTotal());
        RequestConfig.Builder configBuilder = RequestConfig.custom();
        // 设置连接超时
        configBuilder.setConnectTimeout(THREAD_POOL_MAX_TIMEOUT);
        // 设置读取超时
        configBuilder.setSocketTimeout(THREAD_POOL_MAX_TIMEOUT);
        // 设置从连接池获取连接实例的超时
        configBuilder.setConnectionRequestTimeout(THREAD_POOL_MAX_TIMEOUT);
        REQUEST_CONFIG = configBuilder.build();
    }

    public HttpUtils(String url, String method) {
        this.url = url;
        this.method = method;
    }

    // default get request
    public HttpUtils(String url) {
        this.url = url;
    }

    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public String getUrl() {
        return url;
    }

    public String getCharset() {
        if (null == this.charset || "".equals(this.charset)) {
            this.charset = DEFAULT_CHARSET;
        }
        return this.charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getMethod() {
        if (this.method == null || "".equals(this.method)) {
            this.method = GET;
        }

        return this.method;
    }

    public void addParameter(String key, Object value) {
        if (key != null && value != null) {
            this.params.add(new Parameter(key, value));
        }
    }

    public String doRequest() throws Exception {
        // 通用的请求头
        this.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.120 Safari/537.36");
        this.addHeader("Connection", "Keep-Alive");
        this.addHeader("Accept-Charset", this.getCharset());

        HttpRequestBase request;
        HttpEntityEnclosingRequestBase requestBase;
        String res;
        if (POST.equals(this.getMethod())) {
            requestBase = new HttpPost(url);
            buildBody(requestBase);
            res = execute(requestBase);
        } else if (PUT.equals(this.getMethod())) {
            requestBase = new HttpPut(url);
            buildBody(requestBase);
            res = execute(requestBase);
        } else if (DELETE.equals(this.getMethod())) {
            request = new HttpDelete(buildUrl(url));
            res = execute(request);
        } else if (HEAD.equals(this.getMethod())) {
            request = new HttpHead(buildUrl(url));
            res = execute(request);
        } else if (OPTIONS.equals(this.getMethod())) {
            request = new HttpOptions(buildUrl(url));
            res = execute(request);
        } else if (TRACE.equals(this.getMethod())) {
            request = new HttpTrace(buildUrl(url));
            res = execute(request);
        } else {
            request = new HttpGet(buildUrl(url));
            res = execute(request);
        }
        return res;
    }

    private String execute(HttpRequestBase request) throws IllegalAccessException, IOException {
        request.setConfig(REQUEST_CONFIG);
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }
        HttpClient httpClient = wrapClient();
        HttpResponse response = httpClient.execute(request);
        int code = response.getStatusLine().getStatusCode();
        if (code != 200) {
            String err;
            try {
                err = EntityUtils.toString(response.getEntity(), this.getCharset());
            } catch (Exception var9) {
                err = "无法读取响应消息";
            }
            throw new IllegalAccessException(err);
        }
        // 直接返回响应结果，给外部处理
        return EntityUtils.toString(response.getEntity(), this.getCharset());
    }

    private void buildBody(HttpEntityEnclosingRequestBase requestBase) throws UnsupportedEncodingException {
        if ("application/x-www-form-urlencoded".equals(this.headers.get("Content-Type"))) {
            UrlEncodedFormEntity formEntity = buildFormEntity();
            formEntity.setContentType("application/x-www-form-urlencoded; charset=" + this.getCharset());
            requestBase.setEntity(formEntity);
        } else if ("multipart/form-data".equals(this.headers.get("Content-Type"))) {
            //还有一种是multipart/form-data
            UrlEncodedFormEntity formEntity = buildFormEntity();
            formEntity.setContentType("multipart/form-data; charset=" + this.getCharset());
            requestBase.setEntity(formEntity);
        } else {
            //还有种是json
            requestBase.setEntity(new StringEntity(buildJson(), this.getCharset()));
        }
    }

    private UrlEncodedFormEntity buildFormEntity() throws UnsupportedEncodingException {
        List<NameValuePair> nameValuePairList;
        if (CollectionUtil.isEmpty(this.params) && !ObjectUtils.isEmpty(this.getObjectParam())) {
            Map<String, Object> maps = JSON.parseObject(JSONObject.toJSONString(this.getObjectParam()));
            nameValuePairList = new ArrayList<>();
            for (Map.Entry<String, Object> item : maps.entrySet()) {
                nameValuePairList.add(new BasicNameValuePair(item.getKey(), JSONObject.toJSONString(item.getValue())));
            }
        } else {
            nameValuePairList = this.params.stream().map(item -> new BasicNameValuePair(item.getKey(), JSONObject.toJSONString(item.getValue()))).collect(Collectors.toList());
        }
        return new UrlEncodedFormEntity(nameValuePairList, this.getCharset());
    }

    public String buildJson() {
        if (CollectionUtil.isEmpty(this.params) && !ObjectUtils.isEmpty(this.getObjectParam())) {
            return JSONObject.toJSONString(this.getObjectParam());
        } else {
            Map<String, Object> map = this.params.stream().collect(Collectors.toMap(Parameter::getKey, Parameter::getValue));
            return JSONObject.toJSONString(map);
        }
    }

    private String buildQueryParams() {
        List<String> data = new ArrayList<>();

        for (Parameter entry : this.params) {
            if (entry.getKey() != null && entry.getValue() != null) {
                data.add(entry.getKey() + "=" + encodeURI(String.valueOf(entry.getValue())));
            }
        }
        return StringUtils.join(data, "&");
    }

    public static String encodeURI(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (Exception var2) {
            return str;
        }
    }

    private String buildUrl(String uri) {
        StringBuilder sbUrl = new StringBuilder(uri);
        String params = buildQueryParams();
        if (params.length() > 0) {
            sbUrl.append("?").append(params);
        }
        return sbUrl.toString();
    }

    private HttpClient wrapClient() {
        if (this.getUrl().startsWith("https://")) {
            return sslClient();
        }
        return HttpClients.createDefault();
    }

    private static HttpClient sslClient() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] xcs, String str) {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] xcs, String str) {

                }
            };
            ctx.init(null, new TrustManager[]{tm}, null);

            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(ctx, NoopHostnameVerifier.INSTANCE);
            // 创建Registry
            RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD_STRICT)
                    .setExpectContinueEnabled(Boolean.TRUE).setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))
                    .setProxyPreferredAuthSchemes(Collections.singletonList(AuthSchemes.BASIC)).build();
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .register("https", socketFactory).build();
            // 创建ConnectionManager，添加Connection配置信息
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            return HttpClients.custom().setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(requestConfig).build();
        } catch (KeyManagementException | NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static class Parameter {
        private final String key;
        private final Object value;

        public Parameter(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return this.key;
        }

        public Object getValue() {
            return this.value;
        }
    }

    public <T> T parseResultJson(String json, Class<T> tClass, List<T> tList) {
        // 这里通常会是一个对象，一般都会对返回格式进行统一处理{"msg":xxx,"data":xxx,"code":xxx}
        if (json.startsWith("{") && json.endsWith("}")) {
            // 标准对象返回
            return JSONObject.parseObject(json, tClass);
        } else if (json.startsWith("[") && json.endsWith("]")) {
            // 数组返回
            tList = JSON.parseArray(json,tClass);
            return (T) tList;
        } else {
            // 直接返回的是一个普通字符串
            return (T) json;
        }
    }

    private static final String DONG_DONG_OPEN_API = "http://open.timline.jd.com";
    /**
     * 获取访问凭证
     */
    public static final String API_GET_ACCESS_TOKEN = DONG_DONG_OPEN_API + "/open-apis/v1/auth/get_access_token";
    /**
     * 发送机器人消息
     */
    public static final String API_MESSAGE_ROBOT = DONG_DONG_OPEN_API + "/open-apis/v1/messages/robot";

    public static void main(String[] args) throws Exception {
        LOGGER.debug("进入发送机器人消息测试接口");

        HttpUtils httpUtils = new HttpUtils(API_GET_ACCESS_TOKEN, HttpUtils.POST);
        httpUtils.addHeader("Content-Type", "application/json");
        httpUtils.addHeader("X-Requested-Id", "2e6eb795e542f78328455024db4090");

        httpUtils.setCharset(StandardCharsets.UTF_8.name());
        httpUtils.addParameter("app_key", "00_657dba9fbdb34dcf");
        httpUtils.addParameter("app_secret", "5728bcc5e94e4cb6891cac7351ba96e2");

        String body = httpUtils.doRequest();
//        if (body.startsWith("{")) {
//            // 标准返回的话一般是一个统一格式的响应对象{“msg”:xxx,"data":xxx,"code":xxx}
//            Object obj = JSON.parse(body);
//        } else {
//            List<Object> list = JSON.parseArray(body);
//        }

        LOGGER.info("获取到的数据：" + body);
        JSONObject jsonObject = JSONObject.parseObject(body);
        String data = JSONObject.toJSONString(jsonObject.get("data"));
        JSONObject dataObj = JSONObject.parseObject(data);
        String authorization = dataObj.getString("access_token");
        LOGGER.info("获取到的访问凭证：" + authorization);

        httpUtils = new HttpUtils(API_MESSAGE_ROBOT, HttpUtils.POST);
        httpUtils.addHeader("Content-Type", "application/json");
        httpUtils.addHeader("X-Requested-Id", "2e6eb795e542f78328455024db4091");
        httpUtils.addHeader("Authorization", authorization);
        // 构造请求参数
//        Map<String, String> bodyMap = new HashMap<>();
//        bodyMap.put("type", "text");
//        bodyMap.put("content", "测试2");
//        Map<String, String> toMap = new HashMap<>();
//        toMap.put("pin", "ext.wenzhongming1");
//        toMap.put("app", "ee");
//        httpUtils.addParameter("body", bodyMap);
//        httpUtils.addParameter("to", toMap);
//        body = httpUtils.doRequest();
        //TODO: 支持对象参数，降低参数构造的复杂性

        // 请求机器人发送消息的接口
        //RobotMessage robotMessage = new RobotMessage("text", "测试2", "ext.wenzhongming1", "ee");
        //httpUtils.setObjectParam(robotMessage);
        //body = httpUtils.doRequest();
        LOGGER.warn(JSONObject.toJSONString(body));
    }
}
