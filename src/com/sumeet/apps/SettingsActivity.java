package com.sumeet.apps;

import java.util.List;

import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener  {

	private static final boolean DEBUG = true;
	private static final String TAG = "SettingsActivity";
	private SharedPreferences prefs;
	private static String disc_timeout;
	private static EditTextPreference discPreference;
	
	private static String disp_font;
	private static ListPreference dispFontPreference;
	
	//private Preference searchPref;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_settings);
		debug("onCreate");
		prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		disc_timeout = prefs.getString(getString(R.string.pref_discoverable_timeout), getString(R.string.discoverable_default_timeout));
		disp_font = prefs.getString(getString(R.string.pref_display_font), getString(R.string.display_font_default));
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
		debug("onCreate: discoverable_timeout_str = " + disc_timeout);
		debug("onCreate: display_font_str = " + disp_font);
		if(!isSimplePreferences(this))
			return;
		//addPreferencesFromResource(R.xml.bluetooth_settings);
		debug("onCreate:SimplePreferences");
		getFragmentManager().beginTransaction().replace(android.R.id.content, new AllSettingsFragment()).commit();
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		//PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override 
	protected void onPause() {
		//PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}
	
	@Override
	public void onBuildHeaders(List<Header> target) {
		
		boolean result = isSimplePreferences(this);
		debug("onBuildHeaders: isSimplePreferences = " + result);
		if(!result)
			loadHeadersFromResource(R.xml.settings_header,target);
		
	}

	/*@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}*/
	
	/*private static OnPreferenceClickListener onSelectConnectPreference = (new OnPreferenceClickListener() {
		@Override
		  public boolean onPreferenceClick(Preference preference) {
			Intent searchIntent = new Intent(preference.getContext(), ABTConnectActivity.class);
		    preference.getContext().startActivityForResult(searchIntent, Constants.REQUEST_SEARCH);
		    return true;
		  }
	});*/
	private static boolean isXLargeTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
				>= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}
	
	private static boolean isSimplePreferences(Context context) {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
				|| !isXLargeTablet(context);
	}
	
	public static class BluetoothSettingsFragment extends PreferenceFragment{
		
		@Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        
	        if(DEBUG)
	        	Log.d(TAG,"BluetoothSettingsFragment:onCreate");
	        // Load the preferences from an XML resource
	        addPreferencesFromResource(R.xml.bluetooth_settings);
	        discPreference = (EditTextPreference)findPreference(getString(R.string.pref_discoverable_timeout));
	        if(discPreference != null) 
	        	discPreference.setSummary(getString(R.string.discoverable_timeout_summary) +". Current Value:" + disc_timeout);
	    }

	}
	
	public static class AllSettingsFragment extends PreferenceFragment{
		
		@Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        if(DEBUG)
	        	Log.d(TAG,"AllSettingsFragment: onCreate");
	        // Load the preferences from an XML resource
	        addPreferencesFromResource(R.xml.all_settings);
	        discPreference = (EditTextPreference)findPreference(getString(R.string.pref_discoverable_timeout));
	        if(discPreference != null) 
	        	discPreference.setSummary(getString(R.string.discoverable_timeout_summary) +". Current Value:" + disc_timeout);
	        
	        dispFontPreference = (ListPreference)findPreference(getString(R.string.pref_display_font));
	        if(dispFontPreference != null) 
	        	dispFontPreference.setSummary(disp_font + " pt");
	        
	    }

	}
	
	public static class DisplaySettingsFragment extends PreferenceFragment{
		
		@Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        
	        if(DEBUG)
	        	Log.d(TAG,"DisplaySettingsFragment: onCreate");
	        // Load the preferences from an XML resource
	        addPreferencesFromResource(R.xml.display_settings);
	        
	        dispFontPreference = (ListPreference)findPreference(getString(R.string.pref_display_font));
	        if(dispFontPreference != null) 
	        	dispFontPreference.setSummary(disp_font + " pt");
	    }

	}
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if(key.equals(getString(R.string.pref_discoverable_timeout))) {

			disc_timeout = sharedPreferences.getString(key, getString(R.string.discoverable_default_timeout));
			debug("SettingsActivity: onSharedPreferenceChanged discoverable timeout preference changed: " + disc_timeout
					+ " key = " + key);
			if(discPreference == null)
				Log.e(TAG,"discPreference = null");
			else
				discPreference.setSummary(getString(R.string.discoverable_timeout_summary) +". Current Value:" + disc_timeout);
			
		}
		else if(key.equals(getString(R.string.pref_display_font))) {
			disp_font = sharedPreferences.getString(key, getString(R.string.display_font_default));
			debug("SettingsActivity: onSharedPreferenceChanged display_font preference changed: " + disp_font
					+ " key = " + key);
			if(dispFontPreference == null) 
				Log.e(TAG,"dispFontPreference = null");
			else 
				dispFontPreference.setSummary(disp_font + " pt");
			
		}
	}

	
	private void debug(String msg) {
		if(DEBUG)
			 Log.d(TAG,msg);
	}
	

}
