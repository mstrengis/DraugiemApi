package draugiem.lv.api;

public interface AuthCallback {
	public void onLogin(User u, String apikey);
	public void onError();
	public void onNoApp();
}
