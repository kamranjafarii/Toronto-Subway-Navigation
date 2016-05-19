package com.wifilocalizer.subwaynavigation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import org.json.JSONObject;

/*import android.net.Uri;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
*/
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class RadioMapService extends Service{

	//WiFiScanReceiver receiver;
	//WifiManager wifiManager = null;
	private static final String TAG = "MyService";
	private Socket socket;

	private static final int SERVERPORT = 9876;
	//private static final String SERVER_IP = "107.167.187.16";
	private static final String SERVER_IP = "100.64.210.12";
	private Thread clientThread = null;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {

		clientThread = new Thread(new ClientThread());
		Toast.makeText(this, "Congrats! MyService Created", Toast.LENGTH_LONG).show();
		Log.d(TAG, "onCreate");
		// Initialize Wifi
			/*	try {
					wifiManager = (WifiManager) getBaseContext()
							.getSystemService(Context.WIFI_SERVICE);
					//wifiManager.setWifiEnabled(false);
				} catch (Exception e) {

				}
				*/
				// Register Broadcast Receiver
				/*receiver=new WiFiScanReceiver();
				
				registerReceiver(receiver, new IntentFilter(
						WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
						wifiManager.startScan();
						*/
				 /* 	    NotificationManager notificationManager;
					    Notification myNotification;
					    final String myBlog = "http://android-er.blogspot.com/";
					    final int MY_NOTIFICATION_ID=1;

					   notificationManager =
					      (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

					    
					   
					  myNotification = new Notification(R.drawable.ic_launcher,
					      "Notification!",
					      System.currentTimeMillis());
					    
					    Context context = getApplicationContext();
					    
					    String notificationTitle = "Exercise of Notification!";
					    String notificationText = "http://android-er.blogspot.com/";
					    Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(myBlog));
					    PendingIntent pendingIntent
					     = PendingIntent.getActivity(getBaseContext(),
					       0, myIntent,
					        Intent.FLAG_ACTIVITY_NEW_TASK);
					    myNotification.defaults |= Notification.DEFAULT_SOUND;
					    myNotification.flags |= Notification.FLAG_AUTO_CANCEL;
					    myNotification.setLatestEventInfo(context,
					      notificationTitle,
					      notificationText,
					      pendingIntent);
					    notificationManager.notify(MY_NOTIFICATION_ID, myNotification);*/
	}

	@Override
	public void onStart(Intent intent, int startId) {
		clientThread.start();
		Toast.makeText(this, "My Service Started", Toast.LENGTH_LONG).show();
		Log.d(TAG, "onStart");	
	}
	
	@Override
	public void onDestroy() {
		Toast.makeText(this, "MyService Stopped", Toast.LENGTH_LONG).show();
		//unregisterReceiver(receiver);
		Log.d(TAG, "onDestroy");
		clientThread.interrupt();
		try {
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// Handling wifi samples
	public class WiFiScanReceiver extends BroadcastReceiver {
		
		@Override
		  public void onReceive(Context c, Intent intent) {
			
		//	List<ScanResult> results = wifiManager.getScanResults();
			//Toast.makeText(c, "Number APs:"+Integer.toString(results.size()), Toast.LENGTH_SHORT).show();
			//wifiManager.startScan();
			

			try {
				/*if (socket.isClosed())
					Toast.makeText(c, "Closed", Toast.LENGTH_SHORT).show();
				
				if (socket.isConnected())
					Toast.makeText(c, "Connected", Toast.LENGTH_SHORT).show();
				*/
				
				if (socket!=null){
				PrintWriter out = new PrintWriter(new BufferedWriter(
						new OutputStreamWriter(socket.getOutputStream())),
						true);
				//out.println("Number APs:"+Integer.toString(results.size()));
				 JSONObject jsonObj = new JSONObject();
				// jsonObj.put("count", results.size());
				 out.println(jsonObj.toString());
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			//catch (JSONException e) {
				// TODO Auto-generated catch block
			//	e.printStackTrace();
			//}
			
		  }
	}
	
	
	class ClientThread implements Runnable {

		@Override
		public void run() {
			
			try {
				InetAddress serverAddr = InetAddress.getByName(SERVER_IP);

				socket = new Socket(serverAddr, SERVERPORT);

			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		}

	}
}
