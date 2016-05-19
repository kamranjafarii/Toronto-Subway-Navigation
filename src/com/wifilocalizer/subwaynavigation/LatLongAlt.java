package com.wifilocalizer.subwaynavigation;

import java.io.Serializable;

import com.google.android.gms.maps.model.LatLng;

public class LatLongAlt implements Serializable{


	private static final long serialVersionUID = 1L;
	private double mlatitude;
	private double mlongitude;
	private double maltitude;
	
	public LatLongAlt(LatLng latlng) {
		mlatitude=latlng.latitude;
		mlongitude=latlng.longitude;
		maltitude=0;
	}

	public void setLatLng(LatLng latlng){
		mlatitude=latlng.latitude;
		mlongitude=latlng.longitude;
	}
	
	public LatLng getLatLng(){
		return new LatLng(mlatitude,mlongitude);
	}
	
	public void setAlt(double altitude){		
		maltitude=altitude;
	}
	
	public double getAlt(){		
		return maltitude;
	}	
	
}
