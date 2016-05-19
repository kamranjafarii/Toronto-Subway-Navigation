package com.wifilocalizer.subwaynavigation;

import java.io.Serializable;

public class WiFiScanResult implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String SSID;
	public String BSSID;
	public String capabilities;
	public int frequency;
	public int level;
	
}
