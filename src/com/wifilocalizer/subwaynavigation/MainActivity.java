package com.wifilocalizer.subwaynavigation;


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.wifilocalizer.subwaynavigation.R;
import com.wifilocalizer.subwaynavigation.JSON_ENUM.CommandType;
import com.wifilocalizer.subwaynavigation.JSON_ENUM.JsonKeys;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;



@SuppressLint("InflateParams") public class MainActivity extends Activity implements OnMapClickListener, OnMapLongClickListener, OnMarkerDragListener, OnMarkerClickListener, OnMyLocationButtonClickListener{

	int app_mode;
	int app_state=0;
	final static int CONNECT_STATION=4;
	final static int SINGLE_WIFI_SCAN=5;
	final static int LOCALIZATION=6;
	
	/////////////////////////////////////////////////////////////////////////////
	String ServerAddress="https://pilot-server.appspot.com";
	public static String API_KEY="5ab9535ff010efcabb636b5dd6103fadb2bed9eb";
	public static String USER_NAME="kamran";
	public static String USER_ID="10";
	/////////////////////////////////////////////////////////////////////////////
	
	ProgressDialog progress;
	
	// Google Map
    private GoogleMap googleMap;
    
    Marker marker_float; 
    Marker locationmarker;
    
    LinkedHashMap<Marker, ReferencePoint> RP_Markers_DB = new LinkedHashMap<Marker, ReferencePoint>();
    LinkedHashMap<Polyline, Path> Path_Polyline_DB = new LinkedHashMap<Polyline, Path>();
	
    City TorontoCity = new City("Toronto");
    Localizer localizer;
    
    Context context;
    
	LatLng CENTER;	

	TextToSpeech t1;
	 
    ReferencePoint source_rp;
    ReferencePoint destination_rp;
    ReferencePoint user_destination_rp;
    ReferencePoint previous_station_rp;
    
    private List<Vertex> nodes;
    private List<Edge> edges;
    
    LinkedList<Path> NavigationPaths;
    LinkedList<ReferencePoint> NavigationRPs;
    
	// Wifi scanner part
    int counter=0;
    int online_wifi_samples_number;
    int offline_wifi_samples_number;
    boolean use_rss;
    boolean use_server;
    boolean ble_enable=true;
	WiFiScanReceiver receiver;
	WifiManager wifiManager = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Check whether we're recreating a previously destroyed instance
	    if (savedInstanceState != null) {
	        // Restore value of members from saved state
	       // mCurrentScore = savedInstanceState.getInt(STATE_SCORE);
	        //mCurrentLevel = savedInstanceState.getInt(STATE_LEVEL);
	    } else {
	        // Probably initialize members with default values for a new instance
	    }
	    
	    t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
	         @Override
	         public void onInit(int status) {
	            if(status != TextToSpeech.ERROR) {
	               t1.setLanguage(Locale.US);
	            }
	         }
	      });
	      
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		context = getApplicationContext();
		localizer = new Localizer(context);	
		Localizer.LoadVendorMacs(context);
		
		initilizeMap();
		loadPref();
		
		new LoadCityTask().execute(TorontoCity);
		
		// Initialize Wifi 
		try {
			wifiManager = (WifiManager) getBaseContext().getSystemService(Context.WIFI_SERVICE);
			//wifiManager.setWifiEnabled(false);
		} catch (Exception e) {

		}

				
		// Register Broadcast Receiver
		receiver=new WiFiScanReceiver();

		progress = new ProgressDialog(this);
		progress.setTitle("Wi-Fi Scanner");
		progress.setMessage("Scanning for Wi-Fi APs...");
		//progress.setIndeterminate(false);
		progress.setMax(100);
		progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progress.setOnCancelListener(new DialogInterface.OnCancelListener(){
            @Override
            public void onCancel(DialogInterface dialog) {
            	app_state=0;
            	Localizer.addReferencePoint(RP_Markers_DB.get(marker_float));

            	new SaveCityTask().execute(TorontoCity);

				if(RP_Markers_DB.get(marker_float).hasRSS())
					marker_float.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station_green));
				
				unregisterReceiver(receiver);
            	Toast.makeText(context, "Wi-Fi Scanning canceled!", Toast.LENGTH_SHORT).show();
            }
        });
		
		//startService(new Intent(this, RadioMapService.class));
		//stopService(new Intent(this, RadioMapService.class));
	}

	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	    // Save the user's current game state
	    //savedInstanceState.putInt(STATE_SCORE, mCurrentScore);

	    // Always call the superclass so it can save the view hierarchy state
	    super.onSaveInstanceState(savedInstanceState);
	}
	
	
    @Override
    public void onStop() {
    	super.onStop();  
    	app_state=0;
    	if (app_state==LOCALIZATION || app_state==SINGLE_WIFI_SCAN){
    		unregisterReceiver(receiver);
    		app_state=0;
    	}
    }
	
        
    @Override
    protected void onPause() {
        super.onPause();
        app_state=0;
        if (app_state==LOCALIZATION || app_state==SINGLE_WIFI_SCAN){
    		unregisterReceiver(receiver);
    		app_state=0;
    	}
    }
    
    
    @Override
    public void onResume() {
    	super.onResume();
    }

    
	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
		//menu.getItem(0).setTitle("new");
		if (app_state==LOCALIZATION){
	        menu.getItem(0).setEnabled(false);
	        menu.getItem(1).setEnabled(true);
	        for (int i=2;i<7;i++)
	        	menu.getItem(i).setEnabled(false);
	    }else{
	    	menu.getItem(0).setEnabled(true);
	        menu.getItem(1).setEnabled(false);
	        for (int i=2;i<7;i++)
	        	menu.getItem(i).setEnabled(true);
	    }
	    return true;
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.menu_start:
	        	app_state=LOCALIZATION;
	        	registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
	        	wifiManager.startScan();
	        	counter=0;
	        	previous_station_rp=null;
	        	user_destination_rp=null;
	        	NavigationPaths=null;
	        	NavigationRPs=null;
	        	return true;
	        	
	        case R.id.menu_stop:
	        	app_state=0;
	        	counter=0;
	        	unregisterReceiver(receiver);
	        	previous_station_rp=null;
	        	user_destination_rp=null;
	        	NavigationPaths=null;
	        	NavigationRPs=null;
	        	for (Marker marker : RP_Markers_DB.keySet()){
					marker.remove();
				}
	        	for (Polyline line : Path_Polyline_DB.keySet()){
					line.remove();
				}
				RP_Markers_DB.clear();
				Path_Polyline_DB.clear();
				
				if(locationmarker!=null){
					locationmarker.remove();
					locationmarker=null;
				}
				
				for (int i=0; i<TorontoCity.getNumberOfReferencePoints(); i++){
					if (TorontoCity.getReferencePoint(i).hasRSS())
						RP_Markers_DB.put(googleMap.addMarker(new MarkerOptions().position(TorontoCity.getReferencePoint(i).getLocation()).draggable(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station_green))), TorontoCity.getReferencePoint(i));
					else
						RP_Markers_DB.put( googleMap.addMarker(new MarkerOptions().position(TorontoCity.getReferencePoint(i).getLocation()).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station_red))), TorontoCity.getReferencePoint(i));	
				}
    		 
				for (int i=0; i<TorontoCity.getNumberOfPaths(); i++){						
    			 Path path=TorontoCity.getPath(i);
    				Polyline line = googleMap.addPolyline(new PolylineOptions()
    			     .add(trimlatlng(path.getSource().getLocation()), trimlatlng(path.getDestination().getLocation()))
    			     .width(20)
    			     .color(path.getColor()));
    				Path_Polyline_DB.put(line, path);
				}
	        	return true;
	        	
	        case R.id.menu_switchmap:
	        	if(googleMap.getMapType()==GoogleMap.MAP_TYPE_NORMAL)
	        		googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);	        		
	        	else
	        		googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);	        		
	        	return true;
	        	
	        case R.id.menu_removecommonaps:
	        	new AlertDialog.Builder(MainActivity.this)
			    .setTitle("Remove common APs?")
			    .setMessage("Are you sure?")
			    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) { 
			        	new SeparateRPsTask().execute(TorontoCity);
			        }
			     })
			    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) { 
			            // do nothing
			        }
			     })
			    .setIcon(android.R.drawable.ic_dialog_alert)
			    .show();
	        	return true;
	        	
	        case R.id.menu_import_text:
	        	new AlertDialog.Builder(MainActivity.this)
			    .setTitle("Importing City")
			    .setMessage("All previous data will be lost. Are you sure?")
			    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) { 
			        	new ImportFromTextTask().execute(TorontoCity);
			        }
			     })
			    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) { 
			            // do nothing
			        }
			     })
			    .setIcon(android.R.drawable.ic_dialog_alert)
			    .show();
	        	return true;

	        case R.id.menu_export_text:
	        	new ExportAsTextTask().execute(TorontoCity);
	        	return true;
	        	
	        case R.id.action_settings:
	        	 Intent i = new Intent(context, PrefsActivity.class);
	        	 startActivityForResult(i, 0); 
	        	 return true;
	        default:	            	
	        	return super.onOptionsItemSelected(item);
	    }
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		loadPref();
	}
	
	 
	private void loadPref(){
		SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		online_wifi_samples_number = Integer.valueOf(mySharedPreferences.getString("online_wifi_samples_number", "1"));
		offline_wifi_samples_number = Integer.valueOf(mySharedPreferences.getString("offline_wifi_samples_number", "100"));
		use_rss = mySharedPreferences.getBoolean("use_rss", true);	
		use_server = mySharedPreferences.getBoolean("use_server", false);	
		
		ServerAddress = mySharedPreferences.getString("server_address", "https://pilot-server.appspot.com");
		ServerAddress=ServerAddress+"/unipos?api_key="+API_KEY+"&"+JsonKeys.user_name.toString()+"="+USER_NAME
				+"&"+JsonKeys.user_id.toString()+"="+USER_ID;
		
		API_KEY = mySharedPreferences.getString("api_key", "5ab9535ff010efcabb636b5dd6103fadb2bed9eb");
		USER_NAME = mySharedPreferences.getString("user_name", "kamran");
		USER_ID = mySharedPreferences.getString("user_id", "10");
	}

	    
	private void initilizeMap(){
		 if (googleMap == null) {
            googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
 	  
            googleMap.setIndoorEnabled(false);
            googleMap.setMyLocationEnabled(true);
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
  		  		
  		  	CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(43.659652988335878, -79.397276867154886)).tilt(0).zoom(15).build();
  		  	googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
  		 
  		  	googleMap.setOnMarkerClickListener(this);
  		  	googleMap.setOnMapClickListener(this);
  		  	googleMap.setOnMarkerDragListener(this);
  		  	googleMap.setOnMyLocationButtonClickListener(this);
  		  
            // check if map is created successfully or not
            if (googleMap == null) {
                Toast.makeText(context, "Sorry! unable to create maps", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    
	@Override
    public void onMarkerDrag(Marker marker) {
		
		if (RP_Markers_DB.containsKey(marker)){
			RP_Markers_DB.get(marker).setLocation(marker.getPosition());
			ReferencePoint rp = RP_Markers_DB.get(marker);
			for(Path path : TorontoCity.getAllPaths()){
				if(path.getSource().equals(rp)||path.getDestination().equals(rp)){
					for(Polyline line: Path_Polyline_DB.keySet()){
						if(Path_Polyline_DB.get(line).equals(path)){
							List<LatLng> points= new LinkedList<LatLng>();
							points.add(path.getSource().getLocation());
							points.add(path.getDestination().getLocation());
							line.setPoints(points);
						}
					}
				}		
			}
    	}
		
    }
    
   
    @Override
    public void onMarkerDragEnd(Marker marker) {
    	new SaveCityTask().execute(TorontoCity);
    }

    
    @Override
    public void onMarkerDragStart(Marker marker) {

    	
    }


	@Override
	public void onMapLongClick(LatLng arg0) {

	}


	@Override
	public void onMapClick(LatLng arg0) {

		if(app_state==CONNECT_STATION || app_state==SINGLE_WIFI_SCAN || app_state==LOCALIZATION)
			return;
		
		CENTER=arg0;
		// Chain together various setter methods to set the dialog characteristics
		new AlertDialog.Builder(MainActivity.this).setTitle(R.string.dialog_title)
		.setItems(R.array.map_click_actions, new DialogInterface.OnClickListener() {
		
			public void onClick(DialogInterface dialog, int which) {
		              
				// The 'which' argument contains the index position
				// of the selected item
				switch(which){
				case 0:		//Add GPS RP here
		            		  
					Toast.makeText(context, "Not implemented yet!!!", Toast.LENGTH_LONG).show();
					break;
		            		   
				case 1:		//Add station here
					for (Marker marker : RP_Markers_DB.keySet()){
						marker.remove();
					}
					RP_Markers_DB.clear();
					
					for (int i=0; i<TorontoCity.getNumberOfReferencePoints(); i++){
						if (TorontoCity.getReferencePoint(i).hasRSS())
							RP_Markers_DB.put(googleMap.addMarker(new MarkerOptions().position(TorontoCity.getReferencePoint(i).getLocation()).draggable(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station_green))), TorontoCity.getReferencePoint(i));
						else
							RP_Markers_DB.put(googleMap.addMarker(new MarkerOptions().position(TorontoCity.getReferencePoint(i).getLocation()).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station_red))), TorontoCity.getReferencePoint(i));
					}
					
					LatLongAlt location = new LatLongAlt(CENTER);
					ReferencePoint RP = new ReferencePoint(location);
					TorontoCity.addReferencePoint(RP);
					new SaveCityTask().execute(TorontoCity);
					RP_Markers_DB.put( googleMap.addMarker(new MarkerOptions().position(CENTER).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station_red))), RP);	
					break;
					
				case 2:		// Get all stations
					
					for (Marker marker : RP_Markers_DB.keySet()){
						marker.remove();
					}
					RP_Markers_DB.clear();
					
					if(locationmarker!=null){
						locationmarker.remove();
						locationmarker=null;
					}
					
					for (int i=0; i<TorontoCity.getNumberOfReferencePoints(); i++){
						if (TorontoCity.getReferencePoint(i).hasRSS())
							RP_Markers_DB.put(googleMap.addMarker(new MarkerOptions().position(TorontoCity.getReferencePoint(i).getLocation()).draggable(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station_green))), TorontoCity.getReferencePoint(i));
						else
							RP_Markers_DB.put( googleMap.addMarker(new MarkerOptions().position(TorontoCity.getReferencePoint(i).getLocation()).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station_red))), TorontoCity.getReferencePoint(i));	
					}
				
					
					break;
					
				default:

					break;
		            	   
				}
		                  
			}
				      
		}).create().show();

	}
	
	
	@Override
	public boolean onMarkerClick(Marker marker) {
		
    	marker_float = marker;
    	
		if(app_state==CONNECT_STATION){
			final View dialog_view = MainActivity.this.getLayoutInflater().inflate(R.layout.select_line_color_dialog, null);
        	
			new AlertDialog.Builder(MainActivity.this)
		    .setTitle("Select line color:")
		    .setView(dialog_view)
		    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 

		        	Spinner line_color = (Spinner) dialog_view.findViewById(R.id.line_color);
		        	
		        	int select=line_color.getSelectedItemPosition();
		        	int color = 0;
		        	switch(select){
		        	case 0:
		        		color=Color.BLACK;
		        		break;
		        	case 1:
		        		color=Color.DKGRAY;
		        		break;
		        	case 2:
		        		color=Color.GRAY;
		        		break;
		        	case 3:
		        		color=Color.LTGRAY;
		        		break;
		        	case 4:
		        		color=Color.WHITE;
		        		break;
		        	case 5:
		        		color=Color.RED;
		        		break;
		        	case 6:
		        		color=Color.GREEN;
		        		break;
		        	case 7:
		        		color=Color.BLUE;
		        		break;
		        	case 8:
		        		color=Color.YELLOW;
		        		break;
		        	case 9:
		        		color=Color.CYAN;
		        		break;
		        	case 10:
		        		color=Color.MAGENTA;
		        		break;
		        		
		        	}
		        	destination_rp=RP_Markers_DB.get(marker_float);
					Path path=new Path(source_rp, destination_rp, color);
					TorontoCity.addPath(path);
					new SaveCityTask().execute(TorontoCity);
					Polyline line = googleMap.addPolyline(new PolylineOptions()
				     .add(trimlatlng(path.getSource().getLocation()), trimlatlng(path.getDestination().getLocation()))
				     .width(20)
				     .color(path.getColor()));
					Path_Polyline_DB.put(line, path);
		        	
		        }
		     })
		    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		            // do nothing
		        }
		     })
		    .setIcon(android.R.drawable.ic_dialog_alert)
		    .show();
			
			app_state=0;
			return true;
		}

	        if (RP_Markers_DB.containsKey(marker)){
	        	
					new AlertDialog.Builder(MainActivity.this).setTitle(R.string.dialog_title)
					.setItems(R.array.rp_outdoor_click_actions, new DialogInterface.OnClickListener() {
					
						public void onClick(DialogInterface dialog, int which) {
					              
							// The 'which' argument contains the index position
							// of the selected item
							switch(which){
							case 0:		//Collect RSS
								marker_float.setDraggable(false);
								app_state = SINGLE_WIFI_SCAN;
								registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));	
								progress.show();														
								progress.setMessage("Scanning for Wi-Fi APs...\nFound 0 APs.\n"+"Total "+Integer.toString(RP_Markers_DB.get(marker_float).getScanResults().size())+" APs saved for this RP.");	
								progress.setProgress(0);							    
								counter=0;
								wifiManager.startScan();
								break;
								
								
							case 1:		// Remove RSS
								
								new AlertDialog.Builder(MainActivity.this)
							    .setTitle("Remove RSS?")
							    .setMessage("Are you sure?")
							    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
							        public void onClick(DialogInterface dialog, int which) { 
							        	
							        	marker_float.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station_red));
							        	if(RP_Markers_DB.get(marker_float).hasRSS()){
							        		Localizer.removeReferencePoint(RP_Markers_DB.get(marker_float));
							        		RP_Markers_DB.get(marker_float).clearScanResults();		
							        		marker_float.setDraggable(true);
							        	}
							        
							        	new SaveCityTask().execute(TorontoCity);
							
							        }
							     })
							    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
							        public void onClick(DialogInterface dialog, int which) { 
							            // do nothing
							        }
							     })
							    .setIcon(android.R.drawable.ic_dialog_alert)
							    .show();
								break;
								
								
							case 2:		// Edit Information
								
								final View dialog_view = MainActivity.this.getLayoutInflater().inflate(R.layout.add_rps_dialog, null);
								EditText rp_name = (EditText) dialog_view.findViewById(R.id.rp_name);
					        	EditText rp_description = (EditText) dialog_view.findViewById(R.id.rp_description);
					        	Spinner rp_type = (Spinner) dialog_view.findViewById(R.id.rp_type);
					        	TextView ap_count = (TextView) dialog_view.findViewById(R.id.APCounttextView);
					        	
								if(RP_Markers_DB.get(marker_float).getName()!=null)
					        		rp_name.setText(RP_Markers_DB.get(marker_float).getName());
					        	
					        	if(RP_Markers_DB.get(marker_float).getDescription()!=null)
					        		rp_description.setText(RP_Markers_DB.get(marker_float).getDescription());
					       
					        	rp_type.setSelection((RP_Markers_DB.get(marker_float).getType()));
					        	
					        	ap_count.setText("Number of APs: "+Integer.toString(RP_Markers_DB.get(marker_float).getScanResults().size()));
								
								new AlertDialog.Builder(MainActivity.this)
							    .setTitle("RP Information")
							    .setView(dialog_view)
							    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							        public void onClick(DialogInterface dialog, int which) { 

							        	EditText rp_name = (EditText) dialog_view.findViewById(R.id.rp_name);
							        	EditText rp_description = (EditText) dialog_view.findViewById(R.id.rp_description);
							        	Spinner rp_type = (Spinner) dialog_view.findViewById(R.id.rp_type);
							        	
							        	RP_Markers_DB.get(marker_float).setType(rp_type.getSelectedItemPosition());
							        	RP_Markers_DB.get(marker_float).setName(rp_name.getText().toString());
							        	RP_Markers_DB.get(marker_float).setDescription(rp_description.getText().toString());

							        	new SaveCityTask().execute(TorontoCity);
							        	
							        }
							     })
							    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
							        public void onClick(DialogInterface dialog, int which) { 
							            // do nothing
							        }
							     })
							    .setIcon(android.R.drawable.ic_dialog_alert)
							    .show();

								break;
								
							case 3:		// Remove station
								
								new AlertDialog.Builder(MainActivity.this)
							    .setTitle("Remove station?")
							    .setMessage("Are you sure?")
							    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
							        public void onClick(DialogInterface dialog, int which) { 
							        	
							        	ReferencePoint rp = RP_Markers_DB.get(marker_float);//.setLocation(marker.getPosition());	
							    			
							        	for(Path path : TorontoCity.getAllPaths()){
							    			if(path.getSource().equals(rp)||path.getDestination().equals(rp)){			
							    				for(Polyline line: Path_Polyline_DB.keySet()){
							    					if(Path_Polyline_DB.get(line).equals(path)){
							    						Path_Polyline_DB.remove(line);							    							
							    						line.remove();
							    						TorontoCity.removePath(path);
							    						break;
							    					}
							    				}
							    			}		
							        	}
							    			
							        	if(rp.hasRSS())
											Localizer.removeReferencePoint(rp);
							        	TorontoCity.removeReferencePoint(rp);
							        	RP_Markers_DB.remove(marker_float);
							        	marker_float.remove();
							        	new SaveCityTask().execute(TorontoCity);
							        	
							        }
							     })
							    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
							        public void onClick(DialogInterface dialog, int which) { 
							            // do nothing
							        }
							     })
							    .setIcon(android.R.drawable.ic_dialog_alert)
							    .show();
								
								
								break;
								
								
								
							case 4:		// Push to Server
								new CreateRPonServerTask().execute(RP_Markers_DB.get(marker_float));
								break;
								
								
							case 5:		// Remove from Server
								new RemoveRPfromServerTask().execute(RP_Markers_DB.get(marker_float));
								break;
								
							case 6:		// Connect to another station
								app_state=CONNECT_STATION;
								source_rp=RP_Markers_DB.get(marker_float);
								Toast.makeText(context, "Now select destination...", Toast.LENGTH_SHORT).show();
								break;
								
							case 7:		// This is my destination
								if(RP_Markers_DB.get(marker_float).hasRSS()){
									user_destination_rp=RP_Markers_DB.get(marker_float);
									Toast.makeText(context, "You will be notified on "+user_destination_rp.getName()+" station.", Toast.LENGTH_SHORT).show();
									marker_float.setSnippet("Your destination");
									marker_float.setTitle("Destination");
									marker_float.showInfoWindow();
									
									if(previous_station_rp!=null){
										
										for (Marker marker : RP_Markers_DB.keySet()){
											marker.remove();
										}
							        	for (Polyline line : Path_Polyline_DB.keySet()){
											line.remove();
										}
										RP_Markers_DB.clear();
										Path_Polyline_DB.clear();
										
										if(locationmarker!=null){
											locationmarker.remove();
											locationmarker=null;
										}
										
										for (int i=0; i<TorontoCity.getNumberOfReferencePoints(); i++){
											if (TorontoCity.getReferencePoint(i).hasRSS())
												RP_Markers_DB.put(googleMap.addMarker(new MarkerOptions().position(TorontoCity.getReferencePoint(i).getLocation()).draggable(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station_green))), TorontoCity.getReferencePoint(i));
											else
												RP_Markers_DB.put( googleMap.addMarker(new MarkerOptions().position(TorontoCity.getReferencePoint(i).getLocation()).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station_red))), TorontoCity.getReferencePoint(i));	
										}
						    		 
										for (int i=0; i<TorontoCity.getNumberOfPaths(); i++){						
						    			 Path path=TorontoCity.getPath(i);
						    				Polyline line = googleMap.addPolyline(new PolylineOptions()
						    			     .add(trimlatlng(path.getSource().getLocation()), trimlatlng(path.getDestination().getLocation()))
						    			     .width(20)
						    			     .color(path.getColor()));
						    				Path_Polyline_DB.put(line, path);
										}
										
										nodes = new ArrayList<Vertex>();
										edges = new ArrayList<Edge>();
										for (int i = 0; i < TorontoCity.getNumberOfReferencePoints(); i++){
											nodes.add(new Vertex(Integer.toString(i),TorontoCity.getReferencePoint(i).getName()));									    
										}
										for (int i = 0; i < TorontoCity.getNumberOfPaths(); i++){
											Path path = TorontoCity.getPath(i);
											ReferencePoint src = path.getSource();
											ReferencePoint dst = path.getDestination();
										    edges.add(new Edge(Integer.toString(i),nodes.get(TorontoCity.getIndexofRP(src)), nodes.get(TorontoCity.getIndexofRP(dst)), 1));			
										    edges.add(new Edge(Integer.toString(i),nodes.get(TorontoCity.getIndexofRP(dst)), nodes.get(TorontoCity.getIndexofRP(src)), 1));
										}
										Graph graph = new Graph(nodes, edges);
									    DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(graph);
									    Log.d("node_size",Integer.toString(nodes.size()));
									    Log.d("city_size",Integer.toString(TorontoCity.getNumberOfReferencePoints()));
									    Log.d("idnex",Integer.toString(TorontoCity.getIndexofRP(previous_station_rp)));
									    dijkstra.execute(nodes.get(TorontoCity.getIndexofRP(previous_station_rp)));
									    LinkedList<Vertex> pathV = dijkstra.getPath(nodes.get(TorontoCity.getIndexofRP(user_destination_rp)));
									   
									    LinkedList<Path> path = new LinkedList<Path>();
									    
									    if(pathV!=null){
									    	
									    	for (Vertex vertex : pathV) {
									    		System.out.println(vertex);
									    	}
									    	
									    	for (int i=0;i<pathV.size();i++) {
									    		ReferencePoint src_station = TorontoCity.getReferencePoint(Integer.valueOf(pathV.get(i).getId()));
									    		ReferencePoint dst_station = null;
									    		if((i+1)<pathV.size())
									    			dst_station = TorontoCity.getReferencePoint(Integer.valueOf(pathV.get(i+1).getId()));
									    		if(dst_station!=null){
									    			for (int j = 0; j < TorontoCity.getNumberOfPaths(); j++){
									    				Path pathE = TorontoCity.getPath(j);
													
									    				if( (pathE.getSource().equals(src_station) || pathE.getSource().equals(dst_station))
									    						&& (pathE.getDestination().equals(src_station) || pathE.getDestination().equals(dst_station))){
									    					path.add(pathE);
									    				}
									    			}
									    		
									    		}
									    	}
									    
									    
									    for(int i=0;i<path.size();i++){
									    	for(int j=(i+1);j<path.size();j++){
									    		Path p1=path.get(i);
									    		Path p2=path.get(j);
									    		if ( (p1.getSource().equals(p2.getSource())&& p1.getDestination().equals(p2.getDestination())) ||
									    				(p1.getSource().equals(p2.getDestination())&& p1.getDestination().equals(p2.getSource())) ){
									    			if(i==0){
									    				if(p1.getColor()==path.get(i+1).getColor())
									    					path.remove(p2);
									    				else
									    					path.remove(p1);
									    			}else{
									    				if(p1.getColor()==path.get(i-1).getColor())
									    					path.remove(p2);
									    				else
									    					path.remove(p1);									    					
									    			}
									    		}
									    	}
									    }
									    
									    for(Path p : path){
					    					System.out.println(p.getSource().getName()+"  -- > "+p.getDestination().getName()+ " : "+Integer.toHexString(p.getColor()));
									    	for (Polyline line: Path_Polyline_DB.keySet()){
									    		if(Path_Polyline_DB.get(line).equals(p)){
									    			line.setWidth(30);
									    			line.setColor(Color.CYAN);
									    		}
									    	}
									    }
									    
									    NavigationPaths=path;
									    NavigationRPs= new LinkedList<ReferencePoint>(); 
									    
									    for (int i=0;i<pathV.size();i++) {
									    	NavigationRPs.add(TorontoCity.getReferencePoint(Integer.valueOf(pathV.get(i).getId())));
									    }
									    
									    LatLng st1 = NavigationRPs.get(0).getLocation();
										LatLng st2 = NavigationRPs.get(1).getLocation();
										double delta_lat=st1.latitude-st2.latitude;
										double delta_long=st1.longitude-st2.longitude;
										String t;
										if(Math.abs(delta_lat)>Math.abs(delta_long)){
											if(delta_lat>0)
												t = new String("South bound");
											else
												t = new String("North bound");
										}else{
											if(delta_long<0)
												t = new String("East bound");
											else
												t = new String("West bound");
										}
										t1.speak("Get on the"+t+" trains.", TextToSpeech.QUEUE_FLUSH, null);
									}
									
									}
								}else{
									Toast.makeText(context, "This station is not supported yet.", Toast.LENGTH_SHORT).show();									
								}
								
								break;
								
								default:
									
									break;
							}
						}
					})
					.setIcon(android.R.drawable.ic_dialog_alert)							   
					.show();
					
				}
	        
	        
	        return true;
	 }
	
	
	@Override
	public boolean onMyLocationButtonClick(){

		if (locationmarker != null){
			CameraPosition cameraPosition = new CameraPosition.Builder().target(locationmarker.getPosition()).tilt(0).zoom(15).build();
		  	googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
		}	
		return true;
    }
	
	
	// Handling WiFi samples
	public class WiFiScanReceiver extends BroadcastReceiver {
		
		@SuppressWarnings("unchecked")
		@Override
		  public void onReceive(Context c, Intent intent) {
			 
			List<ScanResult> results = wifiManager.getScanResults();
			
			if(app_state==LOCALIZATION){
				
				if(use_server){
					wifiManager.startScan();
					new LocalizeUsingServerTask().execute(results);
					return;
				}
				
				counter++;
				if(counter==online_wifi_samples_number){
				
				LocalizationResult localizationresult = localizer.Localize(results, use_rss);
				if (localizationresult.RP != null){
					if (locationmarker == null){
						locationmarker = googleMap.addMarker(new MarkerOptions().position(localizationresult.RP.getLocation()).draggable(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station_cyan)));						
					}else{
						locationmarker.setPosition(localizationresult.RP.getLocation());
						
						
					}
//					CameraPosition cameraPosition = new CameraPosition.Builder().target(locationmarker.getPosition()).zoom(15).build();					
//					googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
					locationmarker.setTitle("Current location");
					locationmarker.setSnippet("You are here at "+localizationresult.RP.getName()+" station.");
					locationmarker.showInfoWindow();	
					if(previous_station_rp==null){
						previous_station_rp=localizationresult.RP;
						Toast.makeText(context, "Arriving at "+localizationresult.RP.getName()+" station.", Toast.LENGTH_SHORT).show();
						if(previous_station_rp.equals(user_destination_rp))
							t1.speak("You have arrived at your destination, "+localizationresult.RP.getName()+" station.", TextToSpeech.QUEUE_FLUSH, null);
					}
					if(!previous_station_rp.equals(localizationresult.RP)){
						previous_station_rp=localizationresult.RP;
						Toast.makeText(context, "Arriving at "+localizationresult.RP.getName()+" station.", Toast.LENGTH_SHORT).show();
						if(previous_station_rp.equals(user_destination_rp)){
							t1.speak("You have arrived at your destination, "+localizationresult.RP.getName()+" station.", TextToSpeech.QUEUE_FLUSH, null);
							NavigationPaths=null;
							for (Marker marker : RP_Markers_DB.keySet()){
								marker.remove();
							}
				        	for (Polyline line : Path_Polyline_DB.keySet()){
								line.remove();
							}
							RP_Markers_DB.clear();
							Path_Polyline_DB.clear();
							
							if(locationmarker!=null){
								locationmarker.remove();
								locationmarker=null;
							}
							
							for (int i=0; i<TorontoCity.getNumberOfReferencePoints(); i++){
								if (TorontoCity.getReferencePoint(i).hasRSS())
									RP_Markers_DB.put(googleMap.addMarker(new MarkerOptions().position(TorontoCity.getReferencePoint(i).getLocation()).draggable(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station_green))), TorontoCity.getReferencePoint(i));
								else
									RP_Markers_DB.put( googleMap.addMarker(new MarkerOptions().position(TorontoCity.getReferencePoint(i).getLocation()).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station_red))), TorontoCity.getReferencePoint(i));	
							}
			    		 
							for (int i=0; i<TorontoCity.getNumberOfPaths(); i++){						
			    			 Path path=TorontoCity.getPath(i);
			    				Polyline line = googleMap.addPolyline(new PolylineOptions()
			    			     .add(trimlatlng(path.getSource().getLocation()), trimlatlng(path.getDestination().getLocation()))
			    			     .width(20)
			    			     .color(path.getColor()));
			    				Path_Polyline_DB.put(line, path);
							}
						}
					// Navigation
					if(NavigationPaths!=null){
						if(localizationresult.RP.equals(NavigationRPs.get(1))){
							if(NavigationPaths.size()>=2){
								if(NavigationPaths.get(0).getColor()!=NavigationPaths.get(1).getColor()){
									LatLng st1 = NavigationRPs.get(1).getLocation();
									LatLng st2 = NavigationRPs.get(2).getLocation();
									double delta_lat=st1.latitude-st2.latitude;
									double delta_long=st1.longitude-st2.longitude;
									String t;
									if(Math.abs(delta_lat)>Math.abs(delta_long)){
										if(delta_lat>0)
										t = new String("South bound");
										else
										t = new String("North bound");
									}else{
										if(delta_long<0)
										t = new String("East bound");
										else
										t = new String("West bound");
								}
								
								t1.speak("You should get of the train and get on the"+t+" trains.", TextToSpeech.QUEUE_FLUSH, null);
								}
							}
							
							NavigationRPs.remove(0);
							NavigationPaths.remove(0);
							for (Marker marker : RP_Markers_DB.keySet()){
								marker.remove();
							}
				        	for (Polyline line : Path_Polyline_DB.keySet()){
								line.remove();
							}
							RP_Markers_DB.clear();
							Path_Polyline_DB.clear();
							
							if(locationmarker!=null){
								locationmarker.remove();
								locationmarker=null;
							}
							
							for (int i=0; i<TorontoCity.getNumberOfReferencePoints(); i++){
								if (TorontoCity.getReferencePoint(i).hasRSS())
									RP_Markers_DB.put(googleMap.addMarker(new MarkerOptions().position(TorontoCity.getReferencePoint(i).getLocation()).draggable(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station_green))), TorontoCity.getReferencePoint(i));
								else
									RP_Markers_DB.put( googleMap.addMarker(new MarkerOptions().position(TorontoCity.getReferencePoint(i).getLocation()).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station_red))), TorontoCity.getReferencePoint(i));	
							}
			    		 
							for (int i=0; i<TorontoCity.getNumberOfPaths(); i++){						
			    			 Path path=TorontoCity.getPath(i);
			    				Polyline line = googleMap.addPolyline(new PolylineOptions()
			    			     .add(trimlatlng(path.getSource().getLocation()), trimlatlng(path.getDestination().getLocation()))
			    			     .width(20)
			    			     .color(path.getColor()));
			    				Path_Polyline_DB.put(line, path);
							}
							
							for(Path p : NavigationPaths){
		    					for (Polyline line: Path_Polyline_DB.keySet()){
						    		if(Path_Polyline_DB.get(line).equals(p)){
						    			line.setWidth(30);
						    			line.setColor(Color.CYAN);
						    		}
						    	}
						    }
							
							
						}else{
							
							
						}
		        	
					}
					
					}
					
					
				}else{
					
					Log.d("RP","NULL");
					
				}
				counter=0;
				}
				wifiManager.startScan();
				return;
			}
			
			if (app_state != SINGLE_WIFI_SCAN)
				return;
			
			counter++;
			
			int value=counter*100/offline_wifi_samples_number;
			progress.setProgress(value);
						
			RP_Markers_DB.get(marker_float).addScanResults(results);
			
			progress.setMessage("Scanning for Wi-Fi APs...\n"+"Found "+Integer.toString(results.size())+" APs.\n"+"Total "+Integer.toString(RP_Markers_DB.get(marker_float).getScanResults().size())+" APs saved for this RP.");

			if(counter != offline_wifi_samples_number)
				wifiManager.startScan();
			else{
				app_state=0;
				progress.setProgress(100);
				Localizer.addReferencePoint(RP_Markers_DB.get(marker_float));

				new SaveCityTask().execute(TorontoCity);
				
				if(RP_Markers_DB.get(marker_float).hasRSS())
					marker_float.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station_green));
				
				unregisterReceiver(receiver);
				
				progress.dismiss();
			}
			
			return;	
		}
		
		
	}
	
	 
	
	private class SaveCityTask extends AsyncTask<City, Integer, String> {
	     protected String doInBackground(City... city) {
	    	 city[0].Save(context);
	    	 return city[0].getName();
	     }

	     protected void onProgressUpdate(Integer... progress) {
	        // setProgressPercent(progress[0]);
	     }

	     protected void onPostExecute(String city_name) {
	    	 Toast.makeText(context, "City "+city_name+" saved.", Toast.LENGTH_SHORT).show();
	     }
	 }
	
	
	
	private class LoadCityTask extends AsyncTask<City, Integer, Boolean> {
	     protected Boolean doInBackground(City...city) {
	    	 try {
	 			FileInputStream fis = context.openFileInput("city.bin");
	 			ObjectInputStream is = new ObjectInputStream(fis);
	 			TorontoCity = (City) is.readObject();
	 			is.close();
	 			fis.close();
	 		} catch (FileNotFoundException e) {
	 			e.printStackTrace();
	 			return false;
	 		} catch (IOException e) {
	 			e.printStackTrace();
	 			return false;
	 		} catch (ClassNotFoundException e) {
	 			e.printStackTrace();
	 			return false;
	 		}

		 		
	    	 for (ReferencePoint rp : TorontoCity.getAllReferencePoints()){		 	
	    		 if (rp.hasRSS())
	    			 Localizer.addReferencePoint(rp);	
	    	 }
	    	 
	    	 return true;
	     }

	     protected void onProgressUpdate(Integer... progress) {
	        // setProgressPercent(progress[0]);
	     }

	     protected void onPostExecute(Boolean result) {
	    	 if(result){
	    		 
	    		 for (Marker marker : RP_Markers_DB.keySet()){
						marker.remove();
					}
		        	for (Polyline line : Path_Polyline_DB.keySet()){
						line.remove();
					}
					RP_Markers_DB.clear();
					Path_Polyline_DB.clear();
					
					if(locationmarker!=null){
						locationmarker.remove();
						locationmarker=null;
					}
					
	    		 for (int i=0; i<TorontoCity.getNumberOfReferencePoints(); i++){
						if (TorontoCity.getReferencePoint(i).hasRSS())
							RP_Markers_DB.put(googleMap.addMarker(new MarkerOptions().position(TorontoCity.getReferencePoint(i).getLocation()).draggable(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station_green))), TorontoCity.getReferencePoint(i));
						else
							RP_Markers_DB.put( googleMap.addMarker(new MarkerOptions().position(TorontoCity.getReferencePoint(i).getLocation()).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station_red))), TorontoCity.getReferencePoint(i));	
	    		 }
	    		 
	    		 for (int i=0; i<TorontoCity.getNumberOfPaths(); i++){						
	    			 Path path=TorontoCity.getPath(i);
	    				Polyline line = googleMap.addPolyline(new PolylineOptions()
	    			     .add(trimlatlng(path.getSource().getLocation()), trimlatlng(path.getDestination().getLocation()))
	    			     .width(20)
	    			     .color(path.getColor()));
	    				Path_Polyline_DB.put(line, path);
	    		 }
	    		 Toast.makeText(context, "City loaded.", Toast.LENGTH_SHORT).show();
	    	 }
	    	 else
	    		 Toast.makeText(context, "City loading failed.", Toast.LENGTH_SHORT).show();
	     }
	 }

	
	
	private class ExportAsTextTask extends AsyncTask<City, Integer, Boolean> {
		
	     protected Boolean doInBackground(City...city) {
	    	 return city[0].ExportAlltoStorage(context);
	     }

	     protected void onProgressUpdate(Integer... progress) {

	     }

	     protected void onPostExecute(Boolean result) {
	    	 if(result)
	        		Toast.makeText(context, "Export Successful!", Toast.LENGTH_LONG).show();
	        	else
	        		Toast.makeText(context, "Export Failed!", Toast.LENGTH_LONG).show(); 	 
	     }
	 }
	
	
	
	private class ImportFromTextTask extends AsyncTask<City, Integer, Boolean> {
		
	     protected Boolean doInBackground(City...city) {
	    	 return city[0].ImportAllfromStorage(context);
	     }

	     protected void onProgressUpdate(Integer... progress) {

	     }

	     protected void onPostExecute(Boolean result) {
	    	 if(result){
	    		 	RP_Markers_DB.clear();
	    		 	googleMap.clear();
	    		 	localizer.clearRadioMap();
	        		 try {
	     	 			FileInputStream fis = context.openFileInput("city.bin");
	     	 			ObjectInputStream is = new ObjectInputStream(fis);
	     	 			TorontoCity = (City) is.readObject();
	     	 			is.close();
	     	 			fis.close();
	     	 		} catch (FileNotFoundException e) {
	     	 			e.printStackTrace();
	     	 		} catch (IOException e) {
	     	 			e.printStackTrace();
	     	 		} catch (ClassNotFoundException e) {
	     	 			e.printStackTrace();
	     	 		}

	        		 for (ReferencePoint rp : TorontoCity.getAllReferencePoints()){		 	
	    	    		 if (rp.hasRSS())
	    	    			 Localizer.addReferencePoint(rp);	
	    	    	 }
	    		 	
	     	 		Toast.makeText(context, "Import Successful!", Toast.LENGTH_LONG).show();
	     	 		runOnUiThread(new Runnable() {
	     	 	        @Override
	     	 	        public void run() {
	     	 	        	new LoadCityTask().execute(TorontoCity);
	     	 	        }
	     	 	    });
	    	 }else
	        		Toast.makeText(context, "Import Failed!", Toast.LENGTH_LONG).show(); 	 
	     }
	 }
	
	
	
	private class SeparateRPsTask extends AsyncTask<City, Integer, Integer> {

		List<String> RESTRICTED_MACs = new LinkedList<String>();
		
	     protected Integer doInBackground(City...city) {
	    	 
	    	 RESTRICTED_MACs.clear();	    	    
	    	 if(RP_Markers_DB.size()==0){
	    		 return 0;
	    	 }
	    	 
	    	 for (Marker marker1 : RP_Markers_DB.keySet()){
	    		 for (Marker marker2 : RP_Markers_DB.keySet()){
	    			 if(marker1.equals(marker2))
	    				 continue;
	    			 
	    			 for (WiFiScanResult ap1 : RP_Markers_DB.get(marker1).getScanResults()){
	    				 for (WiFiScanResult ap2 : RP_Markers_DB.get(marker2).getScanResults()){
		    				 if (ap1.BSSID.contains(ap2.BSSID)){
		    					 if(!RESTRICTED_MACs.contains(ap1.BSSID)){
 		 	                		Log.d("RESTRICETED",ap1.BSSID);
 		 	                		RESTRICTED_MACs.add(ap1.BSSID);
 		 	                	}
		    				 }
		    			 }
	    			 }
	    			 
	    		 }
	    	 }
	    	 

	    	 if (RESTRICTED_MACs.size()==0)
	    		 return 0;
	    	 
 		 	                
	    	 for (String mac : RESTRICTED_MACs){
	    		 for (Marker marker1 : RP_Markers_DB.keySet()){	  		 	     
	    			 for (WiFiScanResult ap1 : RP_Markers_DB.get(marker1).getScanResults()){ 		 	     	 	     	 
	    				 if (ap1.BSSID.contains(mac)){  	 
	    					 Log.d("REMOVED",ap1.BSSID); 		 	     	    
	    					 RP_Markers_DB.get(marker1).getScanResults().remove(ap1); 	
	    					 break;
	    				 } 	    	
	    			 }
	    		 }    	 	
	    	 }
	    	 
	    	 
	    	 if (RESTRICTED_MACs.size()!=0)
	    		 city[0].Save(context);
	    	 
	    	 return RESTRICTED_MACs.size();
	     }

	     protected void onProgressUpdate(Integer... progress) {

	     }

	     protected void onPostExecute(Integer result) {
	    	 if(result!=0){
	    		 	RP_Markers_DB.clear();
	    		 	googleMap.clear();
	    		 	localizer.clearRadioMap();
	        		 try {
	     	 			FileInputStream fis = context.openFileInput("city.bin");
	     	 			ObjectInputStream is = new ObjectInputStream(fis);
	     	 			TorontoCity = (City) is.readObject();
	     	 			is.close();
	     	 			fis.close();
	     	 		} catch (FileNotFoundException e) {
	     	 			e.printStackTrace();
	     	 		} catch (IOException e) {
	     	 			e.printStackTrace();
	     	 		} catch (ClassNotFoundException e) {
	     	 			e.printStackTrace();
	     	 		}

	        		 for (ReferencePoint rp : TorontoCity.getAllReferencePoints()){		 	
	    	    		 if (rp.hasRSS())
	    	    			 Localizer.addReferencePoint(rp);	
	    	    	 }
	    		 		
	    		 	
	    	 		
	     	 		Toast.makeText(context, "Found "+Integer.toString(result)+" duplicate APs.", Toast.LENGTH_LONG).show();
	    	 }else
	        		Toast.makeText(context, "No duplicate APs were found.", Toast.LENGTH_LONG).show(); 	 
	     }
	 }

	
	
	private class CreateRPonServerTask extends AsyncTask<ReferencePoint, Integer, Boolean> {
		
	     protected Boolean doInBackground(ReferencePoint...RPs) {
	    	 boolean result=false;
	    	 for(ReferencePoint rp: RPs){
	    		 result = rp.PushToServer(ServerAddress);
	    		 if(!result)
	    			 break;
	    	 }
	    	 if(result){
	    		 new SaveCityTask().execute(TorontoCity);		
	    	 }
	    	 return result;
	     }

	     protected void onProgressUpdate(Integer... progress) {

	     }

	     protected void onPostExecute(Boolean result) {
	    	 if(result)
	        		Toast.makeText(context, "Push Successful!", Toast.LENGTH_LONG).show();
	        	else
	        		Toast.makeText(context, "Push Failed!", Toast.LENGTH_LONG).show(); 	 
	     }
	 }
	
	
	
	private class RemoveRPfromServerTask extends AsyncTask<ReferencePoint, Integer, Boolean> {
		
	     protected Boolean doInBackground(ReferencePoint...RPs) {
	    	 boolean result=false;
	    	 for(ReferencePoint rp: RPs){
	    		 result = rp.RemoveFromServer(ServerAddress);
	    		 if(!result)
	    			 break;
	    	 }
	    	 if(result){					
	    		 new SaveCityTask().execute(TorontoCity);				
	    	 }
	    	 return result;
	     }

	     protected void onProgressUpdate(Integer... progress) {

	     }

	     protected void onPostExecute(Boolean result) {
	    	 if(result){
	        		Toast.makeText(context, "Remove Successful!", Toast.LENGTH_LONG).show();
	        		
	    	 }
	        	else
	        		Toast.makeText(context, "Remove Failed!", Toast.LENGTH_LONG).show(); 	 
	     }
	 }
	
	
	
	private class LocalizeUsingServerTask extends AsyncTask<List<ScanResult>, Integer, Boolean> {
		JSONObject jsonobjectRes = null;
		LocalizationResult localizationresult;
		
		protected Boolean doInBackground(List<ScanResult>...results) {
			localizationresult = localizer.Localize(results[0], false);
			
			JSONObject jsonobjectReq = new JSONObject();
	 		
			JSONObject jsonobjectAP = new JSONObject();
			JSONArray jsonArrayAPs = new JSONArray();
			
			try {
				jsonobjectReq.put(JsonKeys.command_type.toString(), CommandType.localize.toString());
				
				for (ScanResult ap : results[0]){
					jsonobjectAP = new JSONObject();
					jsonobjectAP.put(JsonKeys.rss.toString(), Integer.toString(ap.level));
					jsonobjectAP.put(JsonKeys.mac.toString(), Long.toString(mac2long(ap.BSSID)));
					jsonArrayAPs.put(jsonobjectAP);
				}
				
				jsonobjectReq.put(JsonKeys.aps.toString(), jsonArrayAPs);
				
				System.out.println(jsonobjectReq.toString(2));
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			HttpURLConnection urlConnection=null;
			 try {
				 URL url = new URL(ServerAddress);		 
				 if(ServerAddress.startsWith("https"))
					 urlConnection = (HttpsURLConnection) url.openConnection();
				 else
					 urlConnection = (HttpURLConnection) url.openConnection();
				 urlConnection.setRequestProperty("Content-Type", "application/json");
			     urlConnection.setRequestMethod("POST");
			     OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream()); 
			     urlConnection.connect();
			     
			     out.write(jsonobjectReq.toString(2).getBytes());
			     out.flush();
			     out.close();
			     
			     BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			     
			     String buffer = "";
			     while(true){
			    	 
			    	 String line = in.readLine();
			    	 if (line==null)
			    		 break;
			    	 buffer += line;
			    	 
			     }
			     
			     jsonobjectRes = new JSONObject(new String(buffer));
			     
			     Log.d("json", jsonobjectRes.toString(2));
			     
			 }catch(MalformedURLException err){			 
				 return false;
			 }catch(IOException err){
				 return false;		 
			 }catch(JSONException err){
				 return false;
			 }finally {
			     urlConnection.disconnect();
			 }
			 
			return true;
	     }

	     protected void onProgressUpdate(Integer... progress) {

	     }

	     protected void onPostExecute(Boolean result) {
	    	 if(result){
						
	    		 double latitude = 0;
	    		 double longitude = 0;
	    		 String rp_id = "";
	    		 	try {
	    		 	rp_id = jsonobjectRes.getString(JsonKeys.rp_id.toString());	
	    		 	 latitude = Double.parseDouble(jsonobjectRes.getString(JsonKeys.latitude.toString()));
	    		 	 longitude = Double.parseDouble(jsonobjectRes.getString(JsonKeys.longitude.toString()));
	    		 	} catch (NumberFormatException e) {
	    		 		e.printStackTrace();
	    		 	} catch (JSONException e) {
	    		 		e.printStackTrace();
	    		 	}
	    		 	
	    		 	if(!localizationresult.RP.getID().equals(rp_id))
	    		 		Toast.makeText(context, "Local and Server don't match!", Toast.LENGTH_LONG).show(); 
	    		 	
						LatLng location = new LatLng(latitude, longitude);
						if (locationmarker == null){
							
							locationmarker = googleMap.addMarker(new MarkerOptions().position(location).draggable(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station_cyan)));						

						}else{
							locationmarker.setPosition(location);
						}
	        		
	    	 }	        	
	    	 else
	        		Toast.makeText(context, "Localize Failed!", Toast.LENGTH_LONG).show(); 	 
	     }
	 }
	
	
	
	private long mac2long(String mac_str){
		long res=0;
		String[] tokens=mac_str.split(":");
		for(int i=0;i<tokens.length;i++){
			res= (res<<8) + Integer.parseInt(tokens[i], 16);
		}
		return res;
	}
	
	private LatLng trimlatlng(LatLng in){
		LatLng out= new LatLng(in.latitude, in.longitude);
		return out;
		
	}
	
}
