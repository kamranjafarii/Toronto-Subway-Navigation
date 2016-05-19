
package com.wifilocalizer.subwaynavigation;

import java.io.Serializable;


public class Path implements Serializable{

	private static final long serialVersionUID = 1L;
	private ReferencePoint source;	
	private String mID="0";
	private ReferencePoint destination;	
	private int type;
	private int color;
	
	public Path(ReferencePoint src, ReferencePoint dst, int color) {
		source=src;
		destination=dst;
		this.color=color;
		type=0;
		mID="0";
	}


	public void setSource(ReferencePoint src) {
		source=src;
	}
	
	public ReferencePoint getSource() {
		return source;
	}
	
	public void setDestination(ReferencePoint dst) {
		destination=dst;
	}
	
	public ReferencePoint getDestination() {
		return destination;
	}
	
	public void setID(String ID){
		mID=ID;
	}
	
	public String getID(){
		return mID;
	}
	
	
	public void setType(int Type){
		type=Type;
	}
	
	public int getType(){
		return type;
	}
	
	public void setColor(int color) {
		this.color=color;
	}
	
	public int getColor() {
		return this.color;
	}
	
}

