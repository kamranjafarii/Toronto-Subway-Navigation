package com.wifilocalizer.subwaynavigation;


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.wifi.ScanResult;
import android.util.Log;

import com.wifilocalizer.subwaynavigation.JSON_ENUM.*;
import com.google.android.gms.maps.model.LatLng;


public class ReferencePoint implements Serializable{

	private static final long serialVersionUID = 1L;
	private LatLongAlt mlocation;	
	private String mID="0";
	private String name;
	private String description;
	private int type;
	private LinkedList<WiFiScanResult> mresults;
    
	public ReferencePoint(LatLng location, String Building_Code, int Floor) {
		mlocation=new LatLongAlt(location);
		mresults=new LinkedList<WiFiScanResult>();
		name="";
		description="";
		type=0;
		mID="0";
	}
	
	public ReferencePoint(LatLongAlt location) {
		mlocation=location;
		mresults=new LinkedList<WiFiScanResult>();
		type=0;
		name="";
		description="";
		mID="0";
	}
	

	public void setLocation(LatLng location) {
		mlocation=new LatLongAlt(location);
	}
	
	public LatLng getLocation() {
		return mlocation.getLatLng();
	}
	
	public double getAltitude() {
		return mlocation.getAlt();
	}

	public void addScanResults(List<ScanResult> results) {
		boolean seen = false;
		for (ScanResult result : results) {
			 seen = false;
			for (WiFiScanResult result_saved : mresults){
				if(result.BSSID.equals(result_saved.BSSID))
					seen=true;
			}
			if (!seen){
				if(!Localizer.isValidAP(result.BSSID, result.SSID)){
					continue;
				}		
				WiFiScanResult my_result = new WiFiScanResult();
				my_result.BSSID=result.BSSID;
				my_result.capabilities=result.capabilities;
				my_result.SSID=result.SSID;
				my_result.frequency=result.frequency;
				my_result.level=result.level;
				mresults.add(my_result);
				
			}
		}
		
	}
	
	
	public void addWiFiScanResult(WiFiScanResult scanresult) {	
		if(!Localizer.isValidAP(scanresult.BSSID, scanresult.SSID)){
			Log.d("SSID", scanresult.SSID);
			return;
		}
		mresults.add(scanresult);		
	}
	
	
	public void clearScanResults() {
		mresults.clear();
	}
	
	public boolean hasRSS(){
		if (mresults.size()==0)
			return false;
		else
			return true;
	}
	
	public List<WiFiScanResult> getScanResults() {
		return mresults;
	}
	
	public void setID(String ID){
		mID=ID;
	}
	
	public String getID(){
		return mID;
	}
	
	public void setName(String Name){
		name=Name;
	}
	
	public String getName(){
		return name;
	}
	
	public void setDescription(String Description){
		description=Description;
	}
	
	public String getDescription(){
		return description;
	}
	
	public void setType(int Type){
		type=Type;
	}
	
	public int getType(){
		return type;
	}
	
	public boolean PushToServer(String ServerAddress){
		
		JSONObject jsonobjectReq = new JSONObject();
		JSONObject jsonobjectRes = null;
		JSONObject jsonobjectAP = new JSONObject();
		JSONArray jsonArrayAPs = new JSONArray();
		
		try {

			jsonobjectReq.put(JsonKeys.rp_type.toString(), "manual");
			jsonobjectReq.put(JsonKeys.rp_name.toString(), name);
			jsonobjectReq.put(JsonKeys.command_type.toString(), CommandType.add_manual_rp.toString());
			jsonobjectReq.put(JsonKeys.latitude.toString(), Double.toString(mlocation.getLatLng().latitude));
			jsonobjectReq.put(JsonKeys.longitude.toString(), Double.toString(mlocation.getLatLng().longitude));
			jsonobjectReq.put(JsonKeys.accuracy.toString(), Double.toString(0));
			jsonobjectReq.put(JsonKeys.floor_number.toString(), Integer.toString(0));
			
			for (WiFiScanResult ap : mresults){
				jsonobjectAP = new JSONObject();
				jsonobjectAP.put(JsonKeys.rss.toString(), Integer.toString(ap.level));
				jsonobjectAP.put(JsonKeys.frequency.toString(), Integer.toString(ap.frequency));
				jsonobjectAP.put(JsonKeys.capabilities.toString(), ap.capabilities);
				jsonobjectAP.put(JsonKeys.ssid.toString(), ap.SSID);
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
		    // urlConnection.setChunkedStreamingMode(0);
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
		     
		     Localizer.removeReferencePoint(this);
		     this.mID = jsonobjectRes.getString(JsonKeys.rp_id.toString()); 
		     Localizer.addReferencePoint(this);
		     
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
	
	

	private long mac2long(String mac_str){
		long res=0;
		String[] tokens=mac_str.split(":");
		for(int i=0;i<tokens.length;i++){
			res= (res<<8) + Integer.parseInt(tokens[i], 16);
		}
		return res;
	}
	
	
public boolean RemoveFromServer(String ServerAddress){
		
		JSONObject jsonobjectReq = new JSONObject();
		JSONObject jsonobjectRes = null;
		
		try {

			jsonobjectReq.put(JsonKeys.rp_id.toString(), mID);
			jsonobjectReq.put(JsonKeys.command_type.toString(), CommandType.remove_rp.toString());
		
			
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
	
	
}
