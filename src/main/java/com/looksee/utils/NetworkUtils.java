package com.looksee.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

/**
 * Utility class for network I/O operations such as reading URLs with SSL/GZIP support.
 */
public final class NetworkUtils {

	private NetworkUtils() {
		// Utility class — prevent instantiation
	}

	/**
	 * Reads content from a URL with SSL and GZIP support.
	 *
	 * @param url the url to read
	 * @return the content read from the url
	 *
	 * @throws IOException if an error occurs while reading the url
	 * @throws NoSuchAlgorithmException if an error occurs while reading the url
	 * @throws KeyManagementException if an error occurs while reading the url
	 *
	 * precondition: url != null
	 */
	public static String readUrl(URL url) throws IOException, NoSuchAlgorithmException, KeyManagementException {
		assert url != null;

		SSLContext sc = SSLContext.getDefault();

		HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
		con.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");

		con.setSSLSocketFactory(sc.getSocketFactory());
		if (con.getContentEncoding() != null && con.getContentEncoding().equalsIgnoreCase("gzip")) {
			return readStream(new GZIPInputStream(con.getInputStream()));
		}
		else {
			return readStream(con.getInputStream());
		}
	}

	/**
	 * Reads an InputStream into a String.
	 *
	 * @param in the stream to read
	 * @return the content as a string
	 */
	private static String readStream(InputStream in) {
		StringBuilder sb = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			String nextLine = "";
			while ((nextLine = reader.readLine()) != null) {
				sb.append(nextLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
}
