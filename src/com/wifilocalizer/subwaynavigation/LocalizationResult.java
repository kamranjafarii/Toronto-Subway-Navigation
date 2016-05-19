package com.wifilocalizer.subwaynavigation;

public class LocalizationResult {

	public boolean Successful; 
	public double Latitude;
	public double Longitude;
	public double Altitude;
	public float Accuracy;
	public ReferencePoint RP;
	
	public LocalizationResult() {
		Successful=false; 
		Latitude=0.0;
		Longitude=0.0;
		Altitude=0.0;
		Accuracy=(float) 0.0;
	}

}
