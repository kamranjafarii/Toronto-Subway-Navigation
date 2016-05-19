package com.wifilocalizer.subwaynavigation;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
 
// here is the OnRevieve methode which will be called when boot completed
public class BootCompleted extends BroadcastReceiver{
     @Override
     public void onReceive(Context context, Intent intent) {
    	 // THis automatically ends in eight seconds
 //we double check here for only boot complete event
 if(intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED))
   {
     //here we start the service             
   //  Intent serviceIntent = new Intent(context, RadioMapService.class);
     //context.startService(serviceIntent);
   }
 }
}