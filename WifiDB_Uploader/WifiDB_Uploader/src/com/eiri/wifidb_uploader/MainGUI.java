package com.eiri.wifidb_uploader;

import java.util.Timer;
import java.util.TimerTask;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainGUI extends Activity implements OnClickListener {
	private static final String TAG = "WiFiDB_Demo";
	private Timer myTimer;
	private Context ctx;
	Switch ScanSwitch;
	TextView textStatus;
	Button buttonScan;
	static GoogleMap map;
	
	static Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {			  
				Bundle bundle = msg.getData();
				String lat = bundle.getString("lat");
				String lon = bundle.getString("lon");
				Log.d(TAG, "Timer_Tick - Lat:" + lat + "  -  Lon:" + lon);
				Double Latitude = Double.parseDouble(lat);
				Double Longitude = Double.parseDouble(lon);
				LatLng Position = new LatLng(Latitude, Longitude);
				UpdateMapLocation(Position);
			      }
		}; 	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		ctx = this;
		
		// Setup UI
		ScanSwitch = (Switch) findViewById(R.id.ScanSwitch);
		ScanSwitch.setOnClickListener(this);
		if (isScanServiceRunning()){
			ScanSwitch.setChecked(true);
		}else{
			ScanSwitch.setChecked(false);
		}

		// Setup UI Update Timer
		 myTimer = new Timer();
		  myTimer.schedule(new TimerTask() {
		   @Override
		   public void run() {
			   UpdateMap();
		   }

		  }, 0, 1000);	

		android.app.FragmentManager fragmentManager = getFragmentManager();  
	     MapFragment mapFragment = (MapFragment)fragmentManager.findFragmentById(R.id.map);  
	     map = mapFragment.getMap(); 

		//Setup GPS
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			Toast.makeText(this, "GPS is Enabled in your devide", Toast.LENGTH_SHORT).show();
		}else{
			showGPSDisabledAlertToUser();
		}			
	}
	
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		super.onDestroy();	
		// Stop Scan
		if(myTimer != null) {
			myTimer.cancel();
			myTimer.purge();
			myTimer = null;
		}
	}
	
	private void showGPSDisabledAlertToUser(){
		Log.d(TAG, "showGPSDisabledAlertToUser");
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setMessage("GPS is disabled in your device. Would you like to enable it?")
		.setCancelable(false)
		.setPositiveButton("Goto Settings Page To Enable GPS",
				new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int id){
						Intent callGPSSettingIntent = new Intent(
								android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
						startActivity(callGPSSettingIntent);
					}
				}
		);
		alertDialogBuilder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int id){
						dialog.cancel();
					}
				}
		);
		AlertDialog alert = alertDialogBuilder.create();
		alert.show();
	}	
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 0, 0, "Settings");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                startActivity(new Intent(this, QuickPrefsActivity.class));
                return true;
        }
        return false;
    }
    
	@Override
	public void onClick(View src) {
		int id = src.getId();
		if(id == R.id.ScanSwitch) {
	    	Log.d(TAG, "ScanSwitch Pressed");
	      	ScanSwitch = (Switch) findViewById(R.id.ScanSwitch);
	      	if (ScanSwitch.isChecked()){
	      		Log.d(TAG, "Start Scan");
	      		startService(new Intent(this, ScanService.class));
	      		ScanSwitch.setChecked(true);
	      	} else {
	      		Log.d(TAG, "Stop Scan");
	      		stopService(new Intent(this, ScanService.class));
	      		ScanSwitch.setChecked(false);
	        }
		}
	}
	
	private boolean isScanServiceRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (ScanService.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}

	 private void UpdateMap() {
		 Runnable runnable = new Runnable() {
			 public void run() {
					 Message msg = handler.obtainMessage();
					 Bundle bundle = new Bundle();
					 DatabaseHandler db = new DatabaseHandler(ctx);
					 String[] gps = db.getLatestGPS();
					 if (gps[0]!="0.0" && gps[1]!="0.0") {
						 bundle.putString("lat", gps[0]);
						 bundle.putString("lon", gps[1]);
						 msg.setData(bundle);
						 handler.sendMessage(msg);
					 }

				 }
	      };
	      
	      Thread mythread = new Thread(runnable);
	      mythread.start(); 
	 }
		
	public static void UpdateMapLocation(LatLng CurrentLoc) {
	   	map.moveCamera(CameraUpdateFactory.newLatLngZoom(CurrentLoc, 15));
	}
		
	public static void UpdateMapZoomLevel(Integer zoomlevel) {
	   	map.animateCamera(CameraUpdateFactory.zoomTo(zoomlevel), 2000, null); 
	}
}
