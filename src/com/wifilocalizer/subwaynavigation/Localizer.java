package com.wifilocalizer.subwaynavigation;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.net.wifi.ScanResult;

public class Localizer implements Serializable{


	private static final long serialVersionUID = 1L;
	private static LinkedHashMap<String, LinkedHashMap<String, Integer>> AP_DB;
	private static LinkedHashMap<String, ReferencePoint> RP_DB;
	static private LinkedHashMap<String, String> MAC_Vendors = new LinkedHashMap<String, String>();;
    static private LinkedList<String> MAC_BlackList = new LinkedList<String>();
    static private LinkedList<String> SSID_BlackList = new LinkedList<String>();
	private static int rp_id=1;
	
	public class RP_Result implements Comparable<RP_Result>{
		
		public String RP_ID;
		public int RP_Count;
		
		public RP_Result(String ID, int count){
			RP_ID=ID;
			RP_Count = count;
		}

		@Override
		public int compareTo(RP_Result another) {
			if (this.RP_Count>another.RP_Count)
				return 1;
			else if (this.RP_Count==another.RP_Count)
				return 0;
			else
				return -1;
		}
		
		public String getID(){
			return RP_ID;
		}
	}
	
	
	
	public Localizer(Context context) {
		rp_id=0;
		AP_DB = new LinkedHashMap<String, LinkedHashMap<String, Integer>>();
		RP_DB = new LinkedHashMap<String, ReferencePoint>();
	}

	
	public static void addReferencePoint(ReferencePoint RP){
		if(Integer.parseInt(RP.getID())<1000000){
			String RP_ID = Integer.toString(rp_id++);
			RP.setID(RP_ID);
		}
		RP_DB.put(RP.getID(), RP);
		List<WiFiScanResult> scanresults = RP.getScanResults();
		
		for (WiFiScanResult result : scanresults){
			
			if (AP_DB.containsKey(result.BSSID)){
				if(!AP_DB.get(result.BSSID).containsKey(RP.getID())){
					AP_DB.get(result.BSSID).put(RP.getID(), result.level);
				}
			}else{
				AP_DB.put(result.BSSID, new LinkedHashMap<String, Integer>());
				AP_DB.get(result.BSSID).put(RP.getID(), result.level);
			}
		}
		
	}
	
	public static void removeReferencePoint(ReferencePoint RP){
		String RP_ID = RP.getID();
		List<WiFiScanResult> scanresults = RP.getScanResults();
		
		for (WiFiScanResult result : scanresults){
			if (AP_DB.containsKey(result.BSSID)){
				if(AP_DB.get(result.BSSID).containsKey(RP_ID)){
					AP_DB.get(result.BSSID).remove(RP_ID);
					if(AP_DB.get(result.BSSID).isEmpty())
						AP_DB.remove(result.BSSID);
				}
			}
		}
		RP_DB.remove(RP_ID);
	}
	
	
	public LocalizationResult Localize(List<ScanResult> scanresults, boolean USE_RSS) {
		LinkedHashMap<String, Integer> RP_COUNT = new LinkedHashMap<String, Integer>();
		LinkedHashMap<String, Integer> RP_DIFF = new LinkedHashMap<String, Integer>();
		for (ScanResult result : scanresults){
			if (AP_DB.containsKey(result.BSSID)){
				for (String ID : AP_DB.get(result.BSSID).keySet()){
					if (RP_COUNT.containsKey(ID)){
						RP_COUNT.put(ID, RP_COUNT.get(ID)+1);
						RP_DIFF.put(ID, RP_DIFF.get(ID)+Math.abs(result.level-AP_DB.get(result.BSSID).get(ID)));
					}else{
						RP_COUNT.put(ID, 1);
						RP_DIFF.put(ID, Math.abs(result.level-AP_DB.get(result.BSSID).get(ID)));
					}
				}
			}
		}
		
		LocalizationResult localizationresult = new LocalizationResult();
		
		if (RP_COUNT.isEmpty())
			return localizationresult;
		
		LinkedList<RP_Result> RPs = new LinkedList<RP_Result>(); 
		
		for (String ID : RP_COUNT.keySet()){
			RPs.add(new RP_Result(ID,RP_COUNT.get(ID)));
		}
		
		Collections.sort(RPs, Collections.reverseOrder());

		int i,j;		
		if (!USE_RSS){
			
			for (i=0;i<(RPs.size()-1);i++){			
				if (RPs.get(i).RP_Count>(RPs.get(i+1).RP_Count-2)) 
					break;
			}
		
		
			float[] weights = new float[i+1];
			float max=0;
			int max_index=0;
		
			for (j=0;j<=i;j++){
				weights[j] = (float)RPs.get(j).RP_Count / (float)RP_DB.get(RPs.get(j).getID()).getScanResults().size();
				if (weights[j]>max){
					max=weights[j];
					max_index=j;
				}
			}
		
		
			localizationresult.RP = RP_DB.get(RPs.get(max_index).getID());
			localizationresult.Accuracy = 5.0F / weights[max_index];
		
			
		}else{
			
			for (i=0;i<(RPs.size()-1);i++){			
				if (RPs.get(i).RP_Count>(RPs.get(i+1).RP_Count-2)) 
					break;
			}
			
			float[] weights = new float[i+1];
			float min=-1.0F;
			int min_index=0;
		
			for (j=0;j<=i;j++){
				weights[j] = (float)RP_DIFF.get(RPs.get(j).RP_ID);
				if (weights[j]<min){
					min=weights[j];
					min_index=j;
				}
			}
		
		
			localizationresult.RP = RP_DB.get(RPs.get(min_index).getID());
			localizationresult.Accuracy = 0.2F * weights[min_index];
						
			
		}
		
		return localizationresult;
	}
	
	
	public void clearRadioMap(){	
		rp_id=0;
		AP_DB.clear();
		RP_DB.clear();
	}
	
	
	static public void LoadVendorMacs(Context context){
		
		MAC_BlackList.add("apple");
		MAC_BlackList.add("samsung");
		MAC_BlackList.add("htc");
		MAC_BlackList.add("lg elec");
		MAC_BlackList.add("nokia");
		MAC_BlackList.add("ericsson");
		MAC_BlackList.add("sony");
		MAC_BlackList.add("huawei");
		//MAC_BlackList.add("motorola");
		MAC_BlackList.add("lenovo");
		MAC_BlackList.add("research in motion");
		MAC_BlackList.add("blackberry");
		
		SSID_BlackList.add("android");
		SSID_BlackList.add("iphone");
		SSID_BlackList.add("tablet");
		SSID_BlackList.add("laptop");
		
		XmlResourceParser parser = context.getResources().getXml(R.xml.vendormacs);
		
		try {
 
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
            	String name = parser.getName();
				String mac_prefix = null;
				String vendor_name = null;
				if((name != null) && name.equals("VendorMapping")) {
					int size = parser.getAttributeCount();
					for(int i = 0; i < size; i++) {
						String attrName = parser.getAttributeName(i);
						String attrValue = parser.getAttributeValue(i);
						if((attrName != null) && attrName.equals("mac_prefix")) {
							mac_prefix = attrValue;
						} else if ((attrName != null) && attrName.equals("vendor_name")) {
							vendor_name = attrValue;
						}
					}
					if (mac_prefix != null && vendor_name != null)
						MAC_Vendors.put(mac_prefix.toLowerCase(Locale.getDefault()), vendor_name.toLowerCase(Locale.getDefault()));
				}
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
       
	}
	
	
	public static boolean isValidAP(String BSSID, String SSID){

		String mac_prefix = BSSID.toLowerCase(Locale.getDefault()).substring(0, 8);
		if(MAC_Vendors.containsKey(mac_prefix)){				
			String vendor = MAC_Vendors.get(mac_prefix);
			for (String blocked_vendor : MAC_BlackList){
					if(vendor.contains(blocked_vendor)){
						return false;
					}
			}
		}
		
		for (String blocked_ssid : SSID_BlackList){
			if(SSID.toLowerCase(Locale.getDefault()).contains(blocked_ssid)){
				return false;
			}
	}
		
		return true;
	}
	
}
