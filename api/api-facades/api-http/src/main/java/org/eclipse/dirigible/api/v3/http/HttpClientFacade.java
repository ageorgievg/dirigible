package org.eclipse.dirigible.api.v3.http;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.eclipse.dirigible.api.v3.http.client.HttpClientHeader;
import org.eclipse.dirigible.api.v3.http.client.HttpClientParam;
import org.eclipse.dirigible.api.v3.http.client.HttpClientProxyUtils;
import org.eclipse.dirigible.api.v3.http.client.HttpClientRequestOptions;
import org.eclipse.dirigible.api.v3.http.client.HttpClientResponse;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.commons.api.scripting.IScriptingFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientFacade implements IScriptingFacade {

	private static final Logger logger = LoggerFactory.getLogger(HttpClientFacade.class);

	public static final String get(String url, String options) throws IOException {

		CloseableHttpClient httpClient = HttpClientProxyUtils.getHttpClient(true);

		HttpClientRequestOptions httpClientRequestOptions = parseOptions(options);
		RequestConfig config = prepareConfig(httpClientRequestOptions);
		HttpGet httpGet = new HttpGet(url);
		httpGet.setConfig(config);
		prepareHeaders(httpClientRequestOptions, httpGet);

		CloseableHttpResponse response = httpClient.execute(httpGet);

		return processResponse(response, httpClientRequestOptions.isBinary());
	}

	public static final String post(String url, String options) throws IOException {
		HttpClientRequestOptions httpClientRequestOptions = parseOptions(options);
		if (httpClientRequestOptions.getData() != null) {
			return postBinary(url, httpClientRequestOptions);
		} else if (httpClientRequestOptions.getText() != null) {
			return postText(url, httpClientRequestOptions);
		} else if (httpClientRequestOptions.getParams() != null) {
			return postForm(url, httpClientRequestOptions);
		} else if (httpClientRequestOptions.getFiles() != null) {
			return postFiles(url, httpClientRequestOptions);
		}
		throw new IllegalArgumentException("The element [data] or [text] or [params] or [files] in [options] have to be set for POST requests");
	}

	private static final String postBinary(String url, HttpClientRequestOptions httpClientRequestOptions) throws IOException {

		CloseableHttpClient httpClient = HttpClientProxyUtils.getHttpClient(true);

		RequestConfig config = prepareConfig(httpClientRequestOptions);
		HttpPost httpPost = new HttpPost(url);
		httpPost.setConfig(config);
		prepareHeaders(httpClientRequestOptions, httpPost);

		HttpEntity entity = EntityBuilder.create().setBinary(httpClientRequestOptions.getData()).setContentType(ContentType.APPLICATION_OCTET_STREAM)
				.build();

		httpPost.setEntity(entity);

		CloseableHttpResponse response = httpClient.execute(httpPost);

		return processResponse(response, httpClientRequestOptions.isBinary());
	}

	private static final String postText(String url, HttpClientRequestOptions httpClientRequestOptions) throws IOException {

		CloseableHttpClient httpClient = HttpClientProxyUtils.getHttpClient(true);

		if (httpClientRequestOptions.getText() == null) {
			throw new IllegalArgumentException("The element [text] in [options] cannot be null for POST requests in [text] mode");
		}
		RequestConfig config = prepareConfig(httpClientRequestOptions);
		HttpPost httpPost = new HttpPost(url);
		httpPost.setConfig(config);
		prepareHeaders(httpClientRequestOptions, httpPost);

		HttpEntity entity = EntityBuilder.create().setText(httpClientRequestOptions.getText())
				.setContentEncoding(httpClientRequestOptions.getCharacterEncoding())
				.setContentType(ContentType.create(httpClientRequestOptions.getContentType())).build();

		httpPost.setEntity(entity);

		CloseableHttpResponse response = httpClient.execute(httpPost);

		return processResponse(response, httpClientRequestOptions.isBinary());
	}

	private static final String postForm(String url, HttpClientRequestOptions httpClientRequestOptions) throws IOException {

		CloseableHttpClient httpClient = HttpClientProxyUtils.getHttpClient(true);

		if (httpClientRequestOptions.getParams() == null) {
			throw new IllegalArgumentException("The element [params] in [options] cannot be null for POST requests in [form] mode");
		}
		RequestConfig config = prepareConfig(httpClientRequestOptions);
		HttpPost httpPost = new HttpPost(url);
		httpPost.setConfig(config);
		prepareHeaders(httpClientRequestOptions, httpPost);

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		for (HttpClientParam httpClientParam : httpClientRequestOptions.getParams()) {
			params.add(new BasicNameValuePair(httpClientParam.getName(), httpClientParam.getValue()));
		}
		HttpEntity entity = new UrlEncodedFormEntity(params);

		httpPost.setEntity(entity);

		CloseableHttpResponse response = httpClient.execute(httpPost);

		return processResponse(response, httpClientRequestOptions.isBinary());
	}

	private static final String postFiles(String url, HttpClientRequestOptions httpClientRequestOptions) throws IOException {

		CloseableHttpClient httpClient = HttpClientProxyUtils.getHttpClient(true);

		if (httpClientRequestOptions.getParams() == null) {
			throw new IllegalArgumentException("The element [files] in [options] cannot be null for POST requests in [file] mode");
		}
		RequestConfig config = prepareConfig(httpClientRequestOptions);
		HttpPost httpPost = new HttpPost(url);
		httpPost.setConfig(config);
		prepareHeaders(httpClientRequestOptions, httpPost);

		MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
		for (String filePath : httpClientRequestOptions.getFiles()) {
			File file = new File(filePath);
			multipartEntityBuilder.addBinaryBody(file.getName(), file, ContentType.APPLICATION_OCTET_STREAM, file.getName());
		}
		HttpEntity entity = multipartEntityBuilder.build();

		httpPost.setEntity(entity);

		CloseableHttpResponse response = httpClient.execute(httpPost);

		return processResponse(response, httpClientRequestOptions.isBinary());
	}

	public static final String put(String url, String options) throws IOException {
		HttpClientRequestOptions httpClientRequestOptions = parseOptions(options);
		if (httpClientRequestOptions.getData() != null) {
			return postBinary(url, httpClientRequestOptions);
		} else if (httpClientRequestOptions.getText() != null) {
			return postText(url, httpClientRequestOptions);
		} else if (httpClientRequestOptions.getParams() != null) {
			return postForm(url, httpClientRequestOptions);
		} else if (httpClientRequestOptions.getFiles() != null) {
			return postFiles(url, httpClientRequestOptions);
		}
		throw new IllegalArgumentException("The element [data] or [text] or [params] or [files] in [options] have to be set for POST requests");
	}

	private static final String putBinary(String url, HttpClientRequestOptions httpClientRequestOptions) throws IOException {

		CloseableHttpClient httpClient = HttpClientProxyUtils.getHttpClient(true);

		RequestConfig config = prepareConfig(httpClientRequestOptions);
		HttpPut httpPut = new HttpPut(url);
		httpPut.setConfig(config);
		prepareHeaders(httpClientRequestOptions, httpPut);

		HttpEntity entity = EntityBuilder.create().setBinary(httpClientRequestOptions.getData()).setContentType(ContentType.APPLICATION_OCTET_STREAM)
				.build();

		httpPut.setEntity(entity);

		CloseableHttpResponse response = httpClient.execute(httpPut);

		return processResponse(response, httpClientRequestOptions.isBinary());
	}

	private static final String putText(String url, HttpClientRequestOptions httpClientRequestOptions) throws IOException {

		CloseableHttpClient httpClient = HttpClientProxyUtils.getHttpClient(true);

		if (httpClientRequestOptions.getText() == null) {
			throw new IllegalArgumentException("The element [text] in [options] cannot be null for POST requests in [text] mode");
		}
		RequestConfig config = prepareConfig(httpClientRequestOptions);
		HttpPut httpPut = new HttpPut(url);
		httpPut.setConfig(config);
		prepareHeaders(httpClientRequestOptions, httpPut);

		HttpEntity entity = EntityBuilder.create().setText(httpClientRequestOptions.getText())
				.setContentEncoding(httpClientRequestOptions.getCharacterEncoding())
				.setContentType(ContentType.create(httpClientRequestOptions.getContentType())).build();

		httpPut.setEntity(entity);

		CloseableHttpResponse response = httpClient.execute(httpPut);

		return processResponse(response, httpClientRequestOptions.isBinary());
	}

	public static final String delete(String url, String options) throws IOException {

		CloseableHttpClient httpClient = HttpClientProxyUtils.getHttpClient(true);

		HttpClientRequestOptions httpClientRequestOptions = parseOptions(options);
		RequestConfig config = prepareConfig(httpClientRequestOptions);
		HttpDelete httpDelete = new HttpDelete(url);
		httpDelete.setConfig(config);
		prepareHeaders(httpClientRequestOptions, httpDelete);

		CloseableHttpResponse response = httpClient.execute(httpDelete);

		return processResponse(response, httpClientRequestOptions.isBinary());
	}

	public static final String head(String url, String options) throws IOException {

		CloseableHttpClient httpClient = HttpClientProxyUtils.getHttpClient(true);

		HttpClientRequestOptions httpClientRequestOptions = parseOptions(options);
		RequestConfig config = prepareConfig(httpClientRequestOptions);
		HttpHead httpHead = new HttpHead(url);
		httpHead.setConfig(config);
		prepareHeaders(httpClientRequestOptions, httpHead);

		CloseableHttpResponse response = httpClient.execute(httpHead);

		return processResponse(response, httpClientRequestOptions.isBinary());
	}

	public static final String trace(String url, String options) throws IOException {

		CloseableHttpClient httpClient = HttpClientProxyUtils.getHttpClient(true);

		HttpClientRequestOptions httpClientRequestOptions = parseOptions(options);
		RequestConfig config = prepareConfig(httpClientRequestOptions);
		HttpTrace httpTrace = new HttpTrace(url);
		httpTrace.setConfig(config);
		prepareHeaders(httpClientRequestOptions, httpTrace);

		CloseableHttpResponse response = httpClient.execute(httpTrace);

		return processResponse(response, httpClientRequestOptions.isBinary());
	}

	private static HttpClientRequestOptions parseOptions(String options) {
		HttpClientRequestOptions httpClientRequestOptions = GsonHelper.GSON.fromJson(options, HttpClientRequestOptions.class);
		return httpClientRequestOptions;
	}

	private static RequestConfig prepareConfig(HttpClientRequestOptions httpClientRequestOptions) {

		RequestConfig.Builder configBuilder = RequestConfig.custom();
		configBuilder.setAuthenticationEnabled(httpClientRequestOptions.isAuthenticationEnabled())
				.setCircularRedirectsAllowed(httpClientRequestOptions.isCircularRedirectsAllowed())
				.setContentCompressionEnabled(httpClientRequestOptions.isContentCompressionEnabled())
				.setExpectContinueEnabled(httpClientRequestOptions.isExpectContinueEnabled())
				.setRedirectsEnabled(httpClientRequestOptions.isRedirectsEnabled())
				.setRelativeRedirectsAllowed(httpClientRequestOptions.isRelativeRedirectsAllowed())
				.setMaxRedirects(httpClientRequestOptions.getMaxRedirects())
				.setConnectionRequestTimeout(httpClientRequestOptions.getConnectionRequestTimeout())
				.setConnectTimeout(httpClientRequestOptions.getConnectTimeout()).setSocketTimeout(httpClientRequestOptions.getSocketTimeout())
				.setCookieSpec(httpClientRequestOptions.getCookieSpec())
				.setProxyPreferredAuthSchemes(httpClientRequestOptions.getProxyPreferredAuthSchemes())
				.setTargetPreferredAuthSchemes(httpClientRequestOptions.getTargetPreferredAuthSchemes());

		if ((httpClientRequestOptions.getProxyHost() != null) && (httpClientRequestOptions.getProxyPort() != 0)) {
			configBuilder.setProxy(new HttpHost(httpClientRequestOptions.getProxyHost(), httpClientRequestOptions.getProxyPort()));
		}

		RequestConfig config = configBuilder.build();
		return config;
	}

	private static void prepareHeaders(HttpClientRequestOptions httpClientRequestOptions, HttpRequestBase httpRequestBase) {
		for (HttpClientHeader httpClientHeader : httpClientRequestOptions.getHeaders()) {
			httpRequestBase.setHeader(httpClientHeader.getName(), httpClientHeader.getValue());
		}
	}

	private static String processResponse(CloseableHttpResponse response, boolean binary) throws IOException {
		try {
			HttpClientResponse httpClientResponse = new HttpClientResponse();
			httpClientResponse.setStatusCode(response.getStatusLine().getStatusCode());
			httpClientResponse.setStatusMessage(response.getStatusLine().getReasonPhrase());
			httpClientResponse.setProtocol(response.getProtocolVersion().getProtocol());
			httpClientResponse.setProtocol(response.getProtocolVersion().getProtocol());
			HttpEntity entity = response.getEntity();
			if (entity.getContent() != null) {
				byte[] content = IOUtils.toByteArray(entity.getContent());

				if ((ContentType.getOrDefault(entity).getMimeType().equals(ContentType.TEXT_PLAIN.getMimeType())
						|| ContentType.getOrDefault(entity).getMimeType().equals(ContentType.TEXT_HTML.getMimeType())
						|| ContentType.getOrDefault(entity).getMimeType().equals(ContentType.TEXT_XML.getMimeType())
						|| ContentType.getOrDefault(entity).getMimeType().equals(ContentType.APPLICATION_JSON.getMimeType())
						|| ContentType.getOrDefault(entity).getMimeType().equals(ContentType.APPLICATION_ATOM_XML.getMimeType())
						|| ContentType.getOrDefault(entity).getMimeType().equals(ContentType.APPLICATION_XML.getMimeType())
						|| ContentType.getOrDefault(entity).getMimeType().equals(ContentType.APPLICATION_XHTML_XML.getMimeType())) && (!binary)) {
					String text = new String(content, ContentType.getOrDefault(entity).getCharset());
					httpClientResponse.setText(text);
				} else {
					httpClientResponse.setData(content);
				}
			}

			for (Header header : response.getAllHeaders()) {
				httpClientResponse.getHeaders().add(new HttpClientHeader(header.getName(), header.getValue()));
			}
			EntityUtils.consume(entity);
			return GsonHelper.GSON.toJson(httpClientResponse);
		} finally {
			response.close();
		}
	}

}