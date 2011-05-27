package com.caseystella.news.nlp.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLEditorKit;


public class URLHelper {
	static final Logger log = Logger.getLogger(URLHelper.class.toString());
	public static int RSS_TIMEOUT_WAIT=Integer.parseInt(System.getProperty("rssTimeoutWait", "2500"));
	public static int RSS_MAX_TRIES=Integer.parseInt(System.getProperty("rssMaxTries", "1440"));
	static Map <URL, Reference<String>> mEtagCache = new HashMap<URL, Reference<String>>();
	static Map <URL, Reference<String>> mLastModifiedCache = new HashMap<URL, Reference<String>>();
	static String mUserAgents[] = new String[]{
		"Mozilla/5.0 (X11; U; Linux x86_64; en-GB; rv:1.8.1.5) Gecko/20070719 Iceweasel/2.0.0.5 (Debian-2.0.0.5-0etch1)",
		"Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0; H010818)",
		"IE/6.0 (Windows; U; Windows NT 5.1; en-US; rv:1.2) Gecko/20021126"
	};
	public static URL getURLFromString(String pURL) {
		URL lTmpURL = null;
		try {
			lTmpURL = new URL(pURL);
		}
		catch(MalformedURLException mue) {
			mue.printStackTrace();
		}
		return lTmpURL;
	}

	public static void clearCaches() {
		mEtagCache.clear();
		mLastModifiedCache.clear();
	}

	private static String loadETag(URL pUrl) {
		Reference<String> lEtagRef = mEtagCache.get(pUrl);
		return lEtagRef == null?null:lEtagRef.get();

	}

	private static void storeETag(URL pUrl, String pEtag) {
		mEtagCache.put(pUrl, new SoftReference<String>(pEtag));
	}

	private static String loadLastModified(URL pUrl) {
		Reference<String> lLastModifiedRef = mLastModifiedCache.get(pUrl);
		return lLastModifiedRef == null?null:lLastModifiedRef.get();
	}

	private static void storeLastModified(URL pUrl, String pLastModified) {
		mLastModifiedCache.put(pUrl, new SoftReference<String>(pLastModified));
	}

	public static InputStream getInputStream(URL pUrl) throws Exception {
		HttpURLConnection lConnection = (HttpURLConnection) pUrl.openConnection();
		return getInputStream(lConnection);
	}

	public static InputStream getInputStream(HttpURLConnection sourceConnection) throws Exception {
		return getInputStream(sourceConnection, false);
	}

	public static String getUserAgent() {
		Random lRandom = new Random(System.currentTimeMillis());
		int index = lRandom.nextInt(mUserAgents.length);
		return mUserAgents[index];
	}

	
	public static InputStream getInputStream(HttpURLConnection sourceConnection, boolean pLoadOnlyIfNew) throws Exception {
		try {
			if(sourceConnection.getReadTimeout() != 60*1000) {
				//add parameters to the connection
				sourceConnection.setFollowRedirects(true);
				sourceConnection.setRequestProperty( "User-Agent", getUserAgent() );
				//allow both GZip and Deflate (ZLib) encodings
				sourceConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
				sourceConnection.setReadTimeout(60*1000);
				sourceConnection.setConnectTimeout(60*1000);
			}
			if(pLoadOnlyIfNew) {
				// obtain the ETag from a local store, returns null if not found
				String etag = URLHelper.loadETag(sourceConnection.getURL());

				if (etag != null) {
				  sourceConnection.addRequestProperty("If-None-Match", etag);
				}

				// obtain the Last-Modified from a local store, returns null if not found
				String lastModified = loadLastModified(sourceConnection.getURL());
				if (lastModified != null) {
				  sourceConnection.addRequestProperty("If-Modified-Since",lastModified);
				}
			}

			//establish connection, get response headers
			sourceConnection.connect();
		}
		catch(Exception ex) {
			//ignore exceptions here, if they're real we'll see them thrown below...
		}

		//obtain the encoding returned by the server
		String encoding = sourceConnection.getContentEncoding();
		//if it returns Not modified then we already have the content, return
		if (pLoadOnlyIfNew && sourceConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
		  //disconnect() should only be used when you won't
		  //connect to the same site in a while,
		  //since it disconnects the socket. Only losing
		  //the stream on an HTTP 1.1 connection will
		  //maintain the connection waiting and be
		  //faster the next time around
		  sourceConnection.disconnect();
		  throw new Exception("Ignoring " + sourceConnection.getURL() + " because we've already seen it...");
		}

		if(pLoadOnlyIfNew) {
			//get the last modified & etag and
			//store them for the next check
			storeLastModified(sourceConnection.getURL(), sourceConnection.getHeaderField("Last-Modified"));
			storeETag(sourceConnection.getURL(), sourceConnection.getHeaderField("ETag"));
		}
		InputStream resultingInputStream = null;

		//create the appropriate stream wrapper based on
		//the encoding type
		if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
		  resultingInputStream = new GZIPInputStream(sourceConnection.getInputStream());
		}
		else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
		  resultingInputStream = new InflaterInputStream(sourceConnection.getInputStream(), new Inflater(true));
		}
		else {
		  resultingInputStream = sourceConnection.getInputStream();
		}
		return new BufferedInputStream(resultingInputStream);
	}

	public static Document retrieve(URL pUrl) throws IOException, BadLocationException
	{
		EditorKit kit = new HTMLEditorKit();
		Document doc = kit.createDefaultDocument();

		// The Document class does not yet handle charset's properly.
		doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);

		// Create a reader on the HTML content.

		Reader rd = null;
		// Retrieve from Internet.
		
		URLConnection conn = pUrl.openConnection();
		rd = new InputStreamReader(conn.getInputStream());
		
		// Parse the HTML.
		kit.read(rd, doc, 0);
		
		return doc;
	}
	
}
