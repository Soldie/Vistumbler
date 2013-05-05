package com.eiri.wifidb_uploader;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class ScanService extends Service {
	private static final String TAG = "WiFiDB_ScanService";
	public static final int MSG_SET_MAP_POSITION = 1;
	public static final int MSG_SET_MAP_ZOOM_LEVEL = 2;
	private Context ctx;
	static WifiManager wifi;
	SharedPreferences sharedPrefs;
	String WifiDb_SessionID;
	private static Timer Scan_timer;
	private static Timer Upload_timer;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate"); 
		super.onCreate();
		ctx = this; 
		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		super.onDestroy();	
		// Stop Scan
		if(Scan_timer != null) {
			Scan_timer.cancel();
			Scan_timer.purge();
			Scan_timer = null;
		}
		// Stop Upload
		if(Upload_timer != null) {
			Upload_timer.cancel();
			Upload_timer.purge();
			Upload_timer = null;
		}		
		// Stop GpS
		GPS.stop(ctx);
	}
	
	@Override
	public void onStart(Intent intent, int startid) {
		Log.d(TAG, "onStart");
		// Setup WiFi
		
		wifi.startScan();		
		//Setup GpS
		GPS.start(ctx);
		//Setup Timer
		Integer ScanInterval = Integer.parseInt(sharedPrefs.getString("wifidb_scan_interval", "2000"));
		Integer UploadInterval = Integer.parseInt(sharedPrefs.getString("wifidb_upload_interval", "10000"));
		Log.d(TAG, "ScanInterval:" + ScanInterval);
		Log.d(TAG, "UploadInterval:" + UploadInterval);
		Scan_timer = new Timer();
		Scan_timer.scheduleAtFixedRate(new ScanTask(), 0, ScanInterval);
		Upload_timer = new Timer();
		Upload_timer.scheduleAtFixedRate(new UploadTask(), 0, UploadInterval);		

	}
	
	private class UploadTask extends TimerTask
    { 
        public void run() 
        {
        	// Get Prefs
        	String WifiDb_ApiURL = sharedPrefs.getString("wifidb_upload_api_url", "http://dev01.wifidb.net/wifidb/api/");
        	String WifiDb_Username = sharedPrefs.getString("wifidb_username", "Anonymous");
        	String WifiDb_ApiKey = sharedPrefs.getString("wifidb_apikey", "");
        	Log.d(TAG, "WifiDb_ApiURL: " + WifiDb_ApiURL + " WifiDb_Username: " + WifiDb_Username + " WifiDb_ApiKey: " + WifiDb_ApiKey + " WifiDb_SID: " + WifiDb_SessionID);
	    		       	
        	// Upload APs
        	DatabaseHandler db = new DatabaseHandler(ctx);
        	db.UploadToWifiDB(WifiDb_ApiURL, WifiDb_SessionID, WifiDb_Username, WifiDb_ApiKey);
        }
    }
	
	private class ScanTask extends TimerTask
    { 
        public void run() 
        {
        	//Get Current Time
        	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        	String currentDateandTime = sdf.format(new Date());
    	
        	//Initiate Wifi Scan
        	wifi.startScan();

        	// Get Location
        	Location location = GPS.getLocation(ctx);
        	Double latitude = location.getLatitude();
        	Double longitude = location.getLongitude();
        	float Accuracy = location.getAccuracy();
        	double Altitude = location.getAltitude();
        	float Bearing = location.getBearing();
        	String Provider = location.getProvider();
        	float Speed = location.getSpeed();
        	
        	Integer sats = GPS.getSats(ctx);
        	
         	if (sats<3) {
            	latitude = 0.0;
            	longitude = 0.0;
            	Accuracy = 0;
            	Altitude = 0.0;
            	Bearing = 0;
            	Provider = null;
            	Speed = 0;
        	}      
         	
        	Log.d(TAG, "LAT: " + latitude.toString()
        			+ " LONG: " + longitude.toString()
        			+ " Accuracy: " + Accuracy
        			+ " Altitude: " + Altitude
        			+ " Bearing: " + Bearing
        			+ " Provider: " + Provider 
        			+ " Speed: " + Speed 
        			+ " sats: " + sats);
        	

        	
        	DatabaseHandler db = new DatabaseHandler(ctx);
        	long GpsID = db.addGPS(latitude, longitude, sats, Accuracy, Altitude, Speed, Bearing, currentDateandTime);     	

        	// Get Wifi Info
	        List<ScanResult> results = ScanService.wifi.getScanResults();
	        for (ScanResult result : results) {  
	        	long ApID = db.addAP(GpsID, result.BSSID, result.SSID, result.frequency, result.capabilities, result.level, currentDateandTime);
	        	long HistID = db.addHist(ApID, GpsID, result.level, currentDateandTime);
	        	Log.d(TAG, "GpsID:" + GpsID
	        				+ " HistID:" + HistID
	        				+ " ApID:" + ApID 
	        				+ " SSID:" + result.SSID
	        				+ " BSSID:" + result.BSSID 
	        				+ " capabilities:" + result.capabilities
	        				+ " freq:" + result.frequency 
	        				+ " level:" + result.level);
	     	}	      
        }
    }  
}