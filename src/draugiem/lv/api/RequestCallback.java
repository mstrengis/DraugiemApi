package draugiem.lv.api;

public interface RequestCallback {
	public void onResponse(String response);
	public void onError();
}
