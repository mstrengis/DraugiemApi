package draugiem.lv.api;

public interface PaymentCallback {
	public void onSuccess();
	public void onError(String error);
	public void onNoApp();
	public void onPossibleSms();
	public void onUserCanceled();
}
