package com.wifilocalizer.subwaynavigation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import com.google.android.gms.maps.model.LatLng;

import android.content.Context;
import android.graphics.Color;
import android.os.Environment;

public class City implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private LinkedList<ReferencePoint> mrps;
	private LinkedList<Path> mconnections;
	LinkedHashMap<String, WiFiScanResult> AP_Manifest;
	
	public City(String Name) {
		name=Name;
		mrps = new LinkedList<ReferencePoint>();
		mconnections = new LinkedList<Path>();
		AP_Manifest = new LinkedHashMap<String, WiFiScanResult>();
	}
	
	public void Save(Context context){
		
		FileOutputStream fos;
		try {
			fos = context.openFileOutput("city.bin", Context.MODE_PRIVATE);
			ObjectOutputStream os = new ObjectOutputStream(fos);
			os.writeObject(this);
			os.close();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public String getName(){
		return name;
	}
	
	public void setName(String Name){
		name=Name;
	}
	
	public void addReferencePoint(ReferencePoint RP){
		mrps.add(RP);
	}
	
	public void removeReferencePoint(ReferencePoint RP){
		mrps.remove(RP);
	}
	
	public ReferencePoint getReferencePoint(int Index){
		return mrps.get(Index);
	}
	
	public int getNumberOfReferencePoints(){
		return mrps.size();
	}
	
	
	public void addPath(Path path){
		mconnections.add(path);
	}
	
	public void removePath(Path path){
		mconnections.remove(path);
	}
	
	public Path getPath(int Index){
		return mconnections.get(Index);
	}
	
	public int getNumberOfPaths(){
		return mconnections.size();
	}
	
	public int getIndexofRP(ReferencePoint RP){
		return mrps.indexOf(RP);
	}
	
	
	public boolean ExportAlltoStorage(Context context){
		String	foldername="SubwayNavigation";
		boolean success = false;
		BufferedWriter bfwriter_city=null;
		File cityFile=null;
		File root = Environment.getExternalStorageDirectory();
		File appDirectory = new File(root.toString()+"/"+foldername);
		if (!appDirectory.exists()) {
		    success = appDirectory.mkdir();
		    if (success){
				
			}else{
				return false;
			}
		}
		
		String FileName = "city.info";
		cityFile=new File(appDirectory.getAbsolutePath(), "/"+FileName);
		try {
			bfwriter_city=new BufferedWriter(new FileWriter(cityFile));

			bfwriter_city.write("// City name");bfwriter_city.newLine();
			bfwriter_city.write(name);bfwriter_city.newLine();
			bfwriter_city.write("// Number of Stations");bfwriter_city.newLine();
			bfwriter_city.write(Integer.toString(mrps.size()));
			bfwriter_city.newLine();
				
			bfwriter_city.write("// List of stations : Station Name, Station Description, Station Type, Station ID, Number of APs, Latitude, Longitude, Altitude, MAC Address1, Level1, ...");
			bfwriter_city.newLine();
			
			AP_Manifest.clear();
			
			for(int k=0; k<mrps.size();k++){
				ReferencePoint rp = mrps.get(k);
				LatLng rp_location = rp.getLocation();
				bfwriter_city.write(rp.getName()+","+rp.getDescription()+","+Integer.toString(rp.getType())+","+rp.getID().toString()+","+Integer.toString(rp.getScanResults().size())+","+Double.toString(rp_location.latitude)+","+Double.toString(rp_location.longitude)+","
								+Double.toString(rp.getAltitude()));
				if(rp.getScanResults().size()!=0)
					bfwriter_city.write(",");
				for (int l=0;l<(rp.getScanResults().size()-1);l++){
					WiFiScanResult scanresult = rp.getScanResults().get(l);
					bfwriter_city.write(scanresult.BSSID+","+Integer.toString(scanresult.level)+",");
					if(!AP_Manifest.containsKey(scanresult.BSSID)){
						AP_Manifest.put(scanresult.BSSID, scanresult);
					}		
				}
			
				if(rp.getScanResults().size()!=0){
					WiFiScanResult scanresult = rp.getScanResults().get(rp.getScanResults().size()-1);
					bfwriter_city.write(scanresult.BSSID+","+Integer.toString(scanresult.level));
					if(!AP_Manifest.containsKey(scanresult.BSSID)){
						AP_Manifest.put(scanresult.BSSID, scanresult);
					}		
				}
				bfwriter_city.newLine();
			}
			
			bfwriter_city.write("// Number of Paths");bfwriter_city.newLine();
			bfwriter_city.write(Integer.toString(mconnections.size()));
			bfwriter_city.newLine();
			bfwriter_city.write("// List of paths: Source station index, Destination station index, Color code");
			bfwriter_city.newLine();
			
			
			for(int k=0; k<mconnections.size();k++){
				Path path = mconnections.get(k);
	        	int color_idx = 0;
	        	switch(path.getColor()){
	        	case Color.BLACK:
	        		color_idx=0;
	        		break;
	        	case Color.DKGRAY:
	        		color_idx=1;
	        		break;
	        	case Color.GRAY:
	        		color_idx=2;
	        		break;
	        	case Color.LTGRAY:
	        		color_idx=3;
	        		break;
	        	case Color.WHITE:
	        		color_idx=4;
	        		break;
	        	case Color.RED:
	        		color_idx=5;
	        		break;
	        	case Color.GREEN:
	        		color_idx=6;
	        		break;
	        	case Color.BLUE:
	        		color_idx=7;
	        		break;
	        	case Color.YELLOW:
	        		color_idx=8;
	        		break;
	        	case Color.CYAN:
	        		color_idx=9;
	        		break;
	        	case Color.MAGENTA:
	        		color_idx=10;
	        		break;
	        		
	        	}
				bfwriter_city.write(getIndexofRP(path.getSource())+","+getIndexofRP(path.getDestination())+","+Integer.toString(color_idx));
			
				bfwriter_city.newLine();
			}
			
				
			bfwriter_city.write("// AP Manifest data: MAC, SSID, Capabilites, Frequency");
			bfwriter_city.newLine();
			for (String AP_MAC : AP_Manifest.keySet()){
				WiFiScanResult AP = AP_Manifest.get(AP_MAC);
				bfwriter_city.write(AP.BSSID+","+AP.SSID+","+AP.capabilities+","+Integer.toString(AP.frequency));
				bfwriter_city.newLine();
			}
			
			bfwriter_city.flush();
			bfwriter_city.close();
			
					
				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}
	
	
	public boolean ImportAllfromStorage(Context context){
		String	foldername="SubwayNavigation";
		int rp_count,path_count;
		BufferedReader bfreader_city=null;
		File cityFile=null;
		File root = Environment.getExternalStorageDirectory();
		File appDirectory = new File(root.toString()+"/"+foldername);
		
		mrps.clear();
		AP_Manifest.clear();
		
		String FileName = "city.info";
		cityFile=new File(appDirectory.getAbsolutePath(), "/"+FileName);
		try {
			bfreader_city=new BufferedReader(new FileReader(cityFile));

			bfreader_city.readLine();
			name = bfreader_city.readLine();
			bfreader_city.readLine();
			String[] temp_strr = bfreader_city.readLine().split(",");
			rp_count = Integer.valueOf(temp_strr[0]);

			bfreader_city.readLine();
			LinkedList<String> rp_lines = new LinkedList<String>();
			for (int i=0;i<rp_count;i++){
				rp_lines.add(bfreader_city.readLine());
			}
			
			bfreader_city.readLine();
			temp_strr = bfreader_city.readLine().split(",");
			path_count = Integer.valueOf(temp_strr[0]);

			bfreader_city.readLine();
			LinkedList<String> path_lines = new LinkedList<String>();
			for (int i=0;i<path_count;i++){
				path_lines.add(bfreader_city.readLine());
			}
			
			bfreader_city.readLine();
			String ap_line = bfreader_city.readLine();
			
			while(ap_line != null){
				String[] ap_chars = ap_line.split(",");
				WiFiScanResult accesspoint = new WiFiScanResult();
				accesspoint.BSSID=ap_chars[0];
				accesspoint.SSID=ap_chars[1];
				accesspoint.capabilities=ap_chars[2];
				accesspoint.frequency=Integer.valueOf(ap_chars[3]);				
				AP_Manifest.put(accesspoint.BSSID, accesspoint);
				ap_line = bfreader_city.readLine();
			}
			
			
			
			for (int i=0;i<rp_count;i++){
				String[] temp_str = rp_lines.get(i).split(",");
				String rp_name = temp_str[0];
				String rp_description = temp_str[1];
				int rp_type = Integer.valueOf(temp_str[2]);
				String rp_id = temp_str[3];
				int ap_count = Integer.valueOf(temp_str[4]);
				double latitude = Double.valueOf(temp_str[5]); 
				double longitude = Double.valueOf(temp_str[6]);
				double altitude = Double.valueOf(temp_str[7]);
				
				LatLng latlong = new LatLng(latitude, longitude);
				LatLongAlt location = new LatLongAlt(latlong);
				location.setAlt(altitude);
				
				ReferencePoint rp = new ReferencePoint(location);
				rp.setName(rp_name);
				rp.setDescription(rp_description);
				rp.setType(rp_type);
				rp.setID(rp_id);
				
				
				for (int m=0;m<ap_count;m++){
					WiFiScanResult scanresult = new WiFiScanResult();
					scanresult.BSSID=temp_str[m*2+8];
					scanresult.level = Integer.valueOf(temp_str[m*2+9]);	
					scanresult.SSID=AP_Manifest.get(scanresult.BSSID).SSID;
					scanresult.capabilities=AP_Manifest.get(scanresult.BSSID).capabilities;
					scanresult.frequency=AP_Manifest.get(scanresult.BSSID).frequency;
					rp.addWiFiScanResult(scanresult);
				}
				
				mrps.add(rp);
			}
			
			
			
			for (int i=0;i<path_count;i++){
				String[] temp_str = path_lines.get(i).split(",");
				int source_index = Integer.valueOf(temp_str[0]);
				int destination_index = Integer.valueOf(temp_str[1]);
				int color_index = Integer.valueOf(temp_str[2]);
				int color = 0;
				switch(color_index){
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
				Path path = new Path(mrps.get(source_index), mrps.get(destination_index), color);
				
				mconnections.add(path);
			}
			
			bfreader_city.close();
						
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		this.Save(context);
		
		return true;
	}
	
	public LinkedList<ReferencePoint> getAllReferencePoints(){
		return mrps;
	}

	public LinkedList<Path> getAllPaths(){
		return mconnections;
	}
	
}
