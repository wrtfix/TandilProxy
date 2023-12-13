package com.temperies;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Objects;

public class HTTPConnector {

    private static final int CHAR_BUFFER_SIZE = 8 * 1024; // 8 KiB

    // most popular content types
    public static final String CONTENT_TYPE_ANY = "*/*";
    public static final String CONTENT_TYPE_BINARY = "application/octet-stream";
    public static final String CONTENT_TYPE_HTML = "text/html";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_TEXT = "text/plain";
    public static final String CONTENT_TYPE_URLENC = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_XML = "application/xml";

    // most popular encodings
    public static final String ENCODING_ASCII = "US-ASCII";
    public static final String ENCODING_CHINESE_SIMPLIFIED = "GB2312";
    public static final String ENCODING_CHINESE_TRADITIONAL = "Big5";
    public static final String ENCODING_JAPANESE = "Shift_JIS";
    public static final String ENCODING_LATIN_1 = "ISO-8859-1";
    public static final String ENCODING_LATIN_2 = "ISO-8859-2";
    public static final String ENCODING_UNICODE = "UTF-8";
    public static final String ENCODING_WINDOWS_CYRILLIC = "windows-1251";
    public static final String ENCODING_WINDOWS_WESTERN_EUROPEAN = "windows-1252";

    /**
     * 
     * @param isPost
     * @param request
     * @param URL
     * @param timeoutInSeconds
     * @param rsCharset
     * @param rqContentType
     * @param rqCharset
     * @param proxyHost
     * @param proxyPort
     * @return
     * @throws IOException
     * @throws URISyntaxException 
     */
	private static String callService(boolean isPost, String request, String URL, int timeoutInSeconds, String rsCharset,
			String rqContentType, String rqCharset, String proxyHost, int proxyPort, Hashtable<String, String> headers, String authentication) throws IOException {

		URL url = new URL(URL);
		URLConnection urlConn;
		String basicAuth = null;

		// set basic authentication info if present in the URL
		if (url.getUserInfo() != null) {
			String userInfo = url.getUserInfo().replace("_at_", "@");
			basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userInfo.getBytes());
			url = new URL(URL.replace(url.getUserInfo() + "@", ""));
		}

		if (proxyHost == null) {
			urlConn = url.openConnection();
		} else {
			// set the proxy data
			Proxy p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
			urlConn = url.openConnection(p);
		}

		if (headers != null) {
			for (String key : headers.keySet()) {
				urlConn.setRequestProperty(key, headers.get(key));
			}
		}

		if (basicAuth != null) {
			urlConn.setRequestProperty("Authorization", basicAuth);
		}

		if (authentication != null){
			urlConn.setRequestProperty("Authorization", "Basic ".concat(authentication));
		}

		// set the response charset
		if (rsCharset != null) {
			urlConn.addRequestProperty("Accept-Charset", rsCharset);
		}

		// prepare connection properties
		urlConn.setConnectTimeout(timeoutInSeconds * 1000);
		urlConn.setReadTimeout(timeoutInSeconds * 1000);
		urlConn.setUseCaches(false);

		if (isPost) {
			// send POST data
			HTTPConnector.sendPostData(urlConn, request, rqContentType, rqCharset);
		}

		// receive the response
		String response = HTTPConnector.receiveResponse(urlConn, rsCharset);

		if (urlConn instanceof HttpURLConnection) {
			((HttpURLConnection) urlConn).disconnect();
		}

		return response;
	}


    /**
     * 
     * @param urlConn
     * @param request
     * @param contentType
     * @param charset
     * @throws IOException
     */
    private static void sendPostData(URLConnection urlConn, String request, String contentType, String charset)
            throws IOException {

        // if (contentType != null) {
        // urlConn.addRequestProperty("Content-Type", contentType);
        // }
        // if (charset != null) {
        // urlConn.addRequestProperty("Content-Type", "charset=" + charset);
        // }
        //
        // urlConn.setDoOutput(true);
        ((HttpURLConnection) urlConn).setRequestMethod("POST");
        sendData(urlConn, request, contentType, charset);
        // if (request == null) {
        // return; // nothing to send
        // }
        //
        // // send the request
        // DataOutputStream printout = null;
        // try {
        // printout = new DataOutputStream(urlConn.getOutputStream());
        // printout.writeBytes(request);
        // } finally {
        // if (printout != null) {
        // printout.close();
        // }
        // }

    }

    private static void sendData(URLConnection urlConn, String request, String contentType, String charset) throws IOException {
        if (contentType != null) {
            urlConn.addRequestProperty("Content-Type", contentType);
        }
        if (charset != null) {
            urlConn.addRequestProperty("Charset", "charset=" + charset);
        }

        urlConn.setDoOutput(true);

        if (request == null) {
            return; // nothing to send
        }

        // send the request
        DataOutputStream printout = null;
        try {
            printout = new DataOutputStream(urlConn.getOutputStream());
            if (charset != null) {
                byte[] byteRequest = request.getBytes(charset);
                printout.write(byteRequest, 0, byteRequest.length);
            } else {
                printout.writeBytes(request);
            }
        } finally {
            if (printout != null) {
                printout.close();
            }
        }
    }

	/**
	 * 
	 * @param urlConn
	 * @param charset
	 * @return
	 * @throws IOException
	 */
	private static String receiveResponse(URLConnection urlConn, String charset) throws IOException {
		StringBuilder response = new StringBuilder(CHAR_BUFFER_SIZE);
		BufferedReader br = null;
		try {
			InputStream stream = null;
			if (((HttpURLConnection) urlConn).getResponseCode() >= 400) {
				stream = ((HttpURLConnection) urlConn).getErrorStream();
				if (Objects.isNull(stream)) {
					String initialString = String.format("Error connecting. Http error: %d",
							((HttpURLConnection) urlConn).getResponseCode());
					stream = new ByteArrayInputStream(initialString.getBytes());
				}
			} else {
				stream = urlConn.getInputStream();
			}
			if (charset == null) {
				br = new BufferedReader(new InputStreamReader(stream));
			} else {
				Charset cs = Charset.forName(charset);
				br = new BufferedReader(new InputStreamReader(stream, cs));
			}

			char[] buffer = new char[CHAR_BUFFER_SIZE];
			int read;

			while ((read = br.read(buffer)) != -1) {
				response.append(buffer, 0, read);
			}

		} finally {
			if (br != null) {
				br.close();
			}
		}

		return response.toString();
	}

    /*
     * --------------------- METHOD POST ---------------------
     */

	/**
	 *
	 * @param request
	 * @param URL
	 * @param timeoutInSeconds
	 * @param rsCharset
	 * @param rqContentType
	 * @param rqCharset
	 * @param proxyHost
	 * @param proxyPort
	 * @param headers
	 * @param authentication
	 * @return
	 * @throws IOException
	 */

    public static String callServiceByPost(String request, String URL, int timeoutInSeconds, String rsCharset,
        String rqContentType, String rqCharset, String proxyHost, int proxyPort, Hashtable<String, String> headers, String authentication) throws IOException {
        return HTTPConnector.callService(true, request, URL, timeoutInSeconds, rsCharset, rqContentType, rqCharset, proxyHost,
            proxyPort, headers, authentication);
    }

	/**
	 *
	 * @param request
	 * @param URL
	 * @param timeoutInSeconds
	 * @param rqContentType
	 * @param rqCharset
	 * @param rqHeaders
	 * @param authentication
	 * @return
	 * @throws IOException
	 */
	public static String callServiceByPost(String request, String URL, int timeoutInSeconds, String rqContentType,
			String rqCharset, Hashtable<String, String> rqHeaders, String authentication) throws IOException {
		// TODO Auto-generated method stub
		return HTTPConnector.callServiceByPost(request, URL, timeoutInSeconds, null, rqContentType, rqCharset, null, 0, rqHeaders, authentication);
	}


}
