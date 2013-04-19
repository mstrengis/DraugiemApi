package draugiem.lv.api;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Handler;
import android.util.Base64;

public class DraugiemAuth {
	private static final int AUTHORIZE = 1;
	private static final int PAYMENT = 2;
	private SharedPreferences mSharedPreferences;
	private AuthCallback mAuthCallback;
	private PaymentCallback mPaymentCallback;
	private String mAppHash;
	public String APP; 
	public String APIKEY;
	private Activity mContext;
	private int mTimesChecked = 0, mNeedToCheck = 0;
	public DraugiemAuth(String app, Activity context){
		mContext = context;
		mSharedPreferences = context.getSharedPreferences("DraugiemApi", Context.MODE_PRIVATE);
		try {
			this.APP = app;
	        PackageInfo info = context.getPackageManager().getPackageInfo(
	                context.getPackageName(), 
	                PackageManager.GET_SIGNATURES);
	        
	        for (Signature signature : info.signatures) {
	            MessageDigest md = MessageDigest.getInstance("SHA");
	            md.update(signature.toByteArray());
	            mAppHash = Base64.encodeToString(md.digest(), Base64.DEFAULT).trim();
	        }
	    } catch (NameNotFoundException e) {

	    } catch (NoSuchAlgorithmException e) {

	    }
	}
	
	public boolean onActivityResult(int requestCode, int resultCode, Intent data){
		switch(requestCode){
		case AUTHORIZE:
			if(resultCode == Activity.RESULT_OK){
				if(data == null){
					mAuthCallback.onError();
				}else{
					try{
						JSONObject userToApp = new JSONObject(data.getStringExtra("user"));
						User u = new User(
							userToApp.optInt("id"),
							userToApp.optString("name"),
							userToApp.optString("surname"),
							userToApp.optString("nick"),
							userToApp.optString("city"),
							userToApp.optString("lang"),
							userToApp.optString("imageIcon"),
							userToApp.optString("imageLarge"),
							userToApp.optString("birthday"),
							userToApp.optInt("age"),
							userToApp.optInt("sex")
						);
						mSharedPreferences.edit().putString("apikey", data.getStringExtra("apikey")).putString("user", userToApp.toString()).commit();
						APIKEY = data.getStringExtra("apikey");
						mAuthCallback.onLogin(u, APIKEY);
					}catch(Exception e){
						e.printStackTrace();
						mAuthCallback.onError();
					}
				}
			}else{
				mAuthCallback.onError();
			}
			return true;
		case PAYMENT:
			if(mPaymentCallback == null){
				return false;
			}
			
			if(resultCode == Activity.RESULT_OK){
				if(data.getBooleanExtra("possibleSms", false)){
					mPaymentCallback.onPossibleSms();
				}else if(data.getBooleanExtra("paymentSuccess", false)){
					mPaymentCallback.onSuccess();
				}else if(data.getBooleanExtra("paymentFailed", false)){
					if(data.hasExtra("error")){
						mPaymentCallback.onError(data.getStringExtra("error"));
					}else{
						mPaymentCallback.onError("");
					}
				}else{
					mPaymentCallback.onError("");
				}
			}else{
				mPaymentCallback.onUserCanceled(); 
			}
			
			return true;
		}
		
		return false;
	}
	
	public boolean authorizeFromCache(AuthCallback authcallback){
		try{
			APIKEY = mSharedPreferences.getString("apikey", null);
			if(APIKEY == null){
				return false;
			}
			
			JSONObject userToApp = new JSONObject(mSharedPreferences.getString("user", null));
			User u = new User(
				userToApp.optInt("id"),
				userToApp.optString("name"),
				userToApp.optString("surname"),
				userToApp.optString("nick"),
				userToApp.optString("city"),
				userToApp.optString("lang"),
				userToApp.optString("imageIcon"),
				userToApp.optString("imageLarge"),
				userToApp.optString("birthday"),
				userToApp.optInt("age"),
				userToApp.optInt("sex")
			);
			authcallback.onLogin(u, APIKEY);
			return true;
		}catch(Exception e){
		
		}
		return false;
	}
	
	public void logout(){
		mSharedPreferences.edit().clear().commit();
		APIKEY = null;
	}
	
	public void payment(int transId, PaymentCallback callback){
		this.mPaymentCallback = callback;
		try{
			Intent draugiemIntent = new Intent("com.draugiem.lv.PAYMENT");
			draugiemIntent.putExtra("app", APP);
			draugiemIntent.putExtra("apikey", APIKEY);
			draugiemIntent.putExtra("transId", transId);
			draugiemIntent.putExtra("fingerprint", mAppHash);
			mContext.startActivityForResult(draugiemIntent, PAYMENT); 
		}catch(Exception e){
			mPaymentCallback.onNoApp();
		}
	}
	
	public void authorize(AuthCallback callback){
		this.mAuthCallback = callback;
		try{
			Intent draugiemIntent = new Intent("com.draugiem.lv.AUTHORIZE");
			draugiemIntent.putExtra("app", APP);
			draugiemIntent.putExtra("fingerprint", mAppHash);
			mContext.startActivityForResult(draugiemIntent, AUTHORIZE); 
		}catch(Exception e){
			mAuthCallback.onNoApp();
		}
	}
	
	
	public void checkTransaction(final int transactionId, final int checkTimes, final TransactionCheckCallback transactionCallback){
		mNeedToCheck = checkTimes;
		mTimesChecked++; 
		new Request(Request.prepareRq("app_transactionCheck", "id="+ transactionId, this), new RequestCallback(){
			@Override
			public void onResponse(String response) {
				try{
					JSONObject re = new JSONObject(response);
					re = re.optJSONObject("app_transactionCheck");
					String status = re.optString("status");
					if(status.equals("OK")){
						mTimesChecked = 0;
						transactionCallback.onOk();
						return;
					}else if(status.equals("FAILED")){
						mTimesChecked = 0;
						transactionCallback.onFailed();
						return;
					}
				}catch(Exception e){
					e.printStackTrace();
				}
				
				if(mTimesChecked > mNeedToCheck){
					transactionCallback.onStopChecking();
				}else{
					new Handler().postDelayed(new Runnable(){
						public void run(){
							checkTransaction(transactionId, checkTimes, transactionCallback);
						}
					}, 1500);
					
				}
			}
			@Override
			public void onError() {
				new Handler().postDelayed(new Runnable(){
					public void run(){
						checkTransaction(transactionId, checkTimes, transactionCallback);
					}
				}, 1500);
			}
		}).execute();
	} 
	
	public void getTransactionId(long paymentId, final TransactionCallback transactionCallback){
		new Request(Request.prepareRq("app_transactionCreate", "service="+ paymentId, this), new RequestCallback(){
			@Override
			public void onResponse(String response) {
				try{
					JSONObject re = new JSONObject(response);
					re = re.optJSONObject("app_transactionCreate");
					re = re.optJSONObject("transaction");
					
					transactionCallback.onTransaction(re.optInt("id"), re.optString("link"));
				}catch(Exception e){
					e.printStackTrace();
					transactionCallback.onTransaction(0, null);
				}
			}
			@Override
			public void onError() {
				transactionCallback.onTransaction(0, null);
			}
		}).execute();
	}
	
}
