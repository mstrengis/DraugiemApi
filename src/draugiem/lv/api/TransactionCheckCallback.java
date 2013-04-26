package draugiem.lv.api;

public interface TransactionCheckCallback { 
	public void onOk();
	public void onFailed();
	public void onStopChecking();
}
