package draugiem.lv.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONObject;
import android.os.AsyncTask;

public class Request extends AsyncTask<Void, Void, String>{
	private DefaultHttpClient mHttpClient;
	private String mPostContent;
	private RequestCallback mReCallback;
	public Request(String postContent, RequestCallback reCallback){
		mPostContent = postContent;
		mReCallback = reCallback;
		
		BasicHttpParams params = new BasicHttpParams();
		HttpConnectionParams.setTcpNoDelay(params, true);
		HttpConnectionParams.setStaleCheckingEnabled(params, false);

		HttpConnectionParams.setConnectionTimeout(params, 60 * 1000);
		HttpConnectionParams.setSoTimeout(params, 60 * 1000);

		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION,
				HttpVersion.HTTP_1_1);
		HttpClientParams.setRedirecting(params, false);

		HttpProtocolParams.setUserAgent(params, "Android");

		SchemeRegistry localSchemeRegistry = new SchemeRegistry();
		localSchemeRegistry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		localSchemeRegistry.register(new Scheme("https", SSLSocketFactory
				.getSocketFactory(), 443));

		ConnManagerParams.setMaxTotalConnections(params, 20);
		ConnManagerParams.setMaxConnectionsPerRoute(params,
				new ConnPerRouteBean(20));

		ClientConnectionManager manager = new ThreadSafeClientConnManager(
				params, localSchemeRegistry);
		mHttpClient = new DefaultHttpClient(manager, params);
	}
	
	@Override
	protected String doInBackground(Void... unused) {
		String response = null;
		try{
			HttpPost post = new HttpPost("https://m.draugiem.lv/api/");
			try {
				post.setHeader("Content-Type", "application/json");
				post.setEntity(new StringEntity(mPostContent, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return null;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}

			if (isCancelled()) {
				return null;
			}

			ResponseHandler<String> MyBRH = new BasicResponseHandler();
			boolean retry = true;
			int retries = 0;
			while (retry) {
				if (isCancelled()) {
					return null;
				}
				retry = false;
				retries++;
				try {
					response = null;
					response = mHttpClient.execute(post, MyBRH);
				} catch (OutOfMemoryError e){
					retry = true;
				} catch (ClientProtocolException e) {
					e.printStackTrace();
					retry = false;
				} catch (ConnectTimeoutException e) {
					e.printStackTrace();
					retry = true;
				} catch (IOException e) {
					e.printStackTrace();
					retry = false;
				} catch (Exception e) {
					e.printStackTrace();
					retry = false;
				}

				if (retry && retries > 10) {
					retry = false;
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return response;
	}
	
	@Override
	public void onPostExecute(String response){
		if(response == null || response.equals("")){
			mReCallback.onError();
		}else{
			mReCallback.onResponse(response);
		}
	}
	
	
	public static JSONObject prepareRqMethod(JSONObject method, DraugiemAuth authObj) {
		try {
			JSONObject rq = new JSONObject();
			JSONObject auth = new JSONObject();

			method.put("users_get", new JSONObject());

			if(authObj != null){
				auth.put("app", authObj.APP);
				auth.put("apikey", authObj.APIKEY);
			}
			
			rq.put("auth", auth);
			rq.put("method", method);
			return rq;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static JSONObject prepareRqJSON(String methodName, JSONObject par, DraugiemAuth authObj) {
		try {
			JSONObject rq = new JSONObject();
			JSONObject auth = new JSONObject();
			JSONObject method = new JSONObject();

			method.put(methodName, par);
			method.put("users_get", new JSONObject());

			if(authObj != null){
				auth.put("app", authObj.APP);
				auth.put("apikey", authObj.APIKEY);
			}
			rq.put("auth", auth);
			rq.put("method", method);
			return rq;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static String prepareRq(String methodName, String params,
			boolean decode, DraugiemAuth authObj) {
		try {
			JSONObject rq = new JSONObject();
			JSONObject auth = new JSONObject();
			JSONObject method = new JSONObject();

			JSONObject par = new JSONObject();

			if (params != null) {
				String[] paramParts = params.split("\\&");
				int l = paramParts.length;
				for (int i = 0; i < l; i++) {
					String[] keyValuePair = paramParts[i].split("\\=");
					if (keyValuePair.length == 2) {
						if (decode) {
							par.put(keyValuePair[0],
									URLDecoder.decode(keyValuePair[1], "UTF-8"));
						} else {
							par.put(keyValuePair[0], keyValuePair[1]);
						}
					} else {
						par.put(keyValuePair[0], "");
					}
				}
			}

			method.put(methodName, par);
			method.put("users_get", "");
			
			auth.put("app", authObj.APP);
			auth.put("apikey", authObj.APIKEY);
			rq.put("auth", auth);
			rq.put("method", method);
			return rq.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static String prepareRq(String methodName, String params, DraugiemAuth authObj) {
		return prepareRq(methodName, params, false, authObj);
	}

	public static String prepareRq(String method, DraugiemAuth authObj) {
		return prepareRq(method, null, authObj);
	}
}
