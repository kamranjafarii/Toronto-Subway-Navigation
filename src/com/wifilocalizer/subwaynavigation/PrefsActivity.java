package com.wifilocalizer.subwaynavigation;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PrefsActivity  extends PreferenceActivity {

	 @Override
	 public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  
	  getFragmentManager().beginTransaction().replace(android.R.id.content,
	                new PrefsFragment()).commit();
	 }
	}