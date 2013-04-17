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
import android.util.Base64;
import android.util.Log;

public class DraugiemAuth {
	private static final int AUTHORIZE = 1;
	private static final int PAYMENT = 2;
	private SharedPreferences mSharedPreferences;
	private AuthCallback mAuthCallback;
	private PaymentCallback mPaymentCallback;
	private String mAppHash;
	private String APP;
	private Activity mContext;
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
	            mAppHash = Base64.encodeToString(md.digest(), Base64.DEFAULT);
	            Log.e("Your Tag", mAppHash);
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
						mAuthCallback.onLogin(u, data.getStringExtra("apikey"));
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
			if(resultCode == Activity.RESULT_OK){
				mPaymentCallback.onSuccess();
			}else{
				if(data == null){
					mPaymentCallback.onError("");
				}else{
					mPaymentCallback.onError(data.getStringExtra("error"));
				}
			}
			
			return true;
		}
		
		return false;
	}
	
	public boolean authorizeFromCache(AuthCallback authcallback){
		try{
			String apikey = mSharedPreferences.getString("apikey", null);
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
									
			authcallback.onLogin(u, apikey);
			return true;
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}
	
	public void logout(){
		mSharedPreferences.edit().clear().commit();
	}
	
	public void payment(long transId, PaymentCallback callback){
		this.mPaymentCallback = callback;
		try{
			Intent draugiemIntent = new Intent("com.draugiem.lv.PAYMENT");
			draugiemIntent.putExtra("app", APP);
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
}
