package com.sumeet.apps;

import java.io.BufferedWriter;
import java.io.File;
//import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
//import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Locale;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
//import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
//import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
//import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
//import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
//import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
//import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class ABTActivity extends FragmentActivity implements
		ActionBar.TabListener,OnSharedPreferenceChangeListener {
	
	private static final boolean DEBUG = true;
	private static final String TAG = "ABTActivity";
	
	private BluetoothAdapter mBTAdapter;
	private BluetoothDevice remDev = null;
	private BluetoothSocket btSocket = null;
	private InputStream btInStream = null;
	private OutputStream btOutStream = null;
	private BluetoothServerSocket btServerSocket = null;
	//private OutputStream fileOutStream = null;
	FileWriter fileWriter = null, hexFileWriter = null;
    BufferedWriter fileOutStream = null, hexFileOutStream = null;
	
	private SharedPreferences prefs;
	private SharedPreferences.Editor editor;
	private static int btState; 
	private static int asciiDataState = Constants.DATA_STATE_IDLE;
	private static int hexDataState = Constants.DATA_STATE_IDLE;
	
	private ConnectTask connectTask = null;
	private ConnectedTask connectedTask = null;
	private ListenTask listenTask = null;
	
	public static boolean showHexTab, listen, secureConnection, logAsciiData, logHexData;
	private static boolean logAsciiFileOpen = false;
	private static boolean logHexFileOpen = false;
	private static boolean mounted = false;
	private File path = null, logAsciiFile = null, logHexFile = null;
	
	private String discoverable_timeout_str;
	public static int discoverable_timeout;
	private String display_font_str;
	public static int display_font;
	private EditText txEditText;
	
	private String logFileName, logHexFileName;
	private String logFilePath;

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_abt);
		btState = Constants.BT_IDLE;
		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Create the adapter that will return a fragment for each of the 
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		
		txEditText = (EditText) findViewById(R.id.edit_text);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				debug("Page selected " + position);
				actionBar.setSelectedNavigationItem(position);
			}
		});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			debug("Adding TAB to action bar. Index : " + i);
			actionBar.addTab(actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}
		
		mBTAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBTAdapter == null) {
			Log.e(TAG, "Bluetooth not supported on Device");
			Toast.makeText(this, "Bluetooth not supported on Device",Toast.LENGTH_LONG).show();
			finish();
			return;
		} else {
			debug("Bluetooth supported on Device");
		}
		
		logFilePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/ABT/";
		debug("onCreate: logFilePath = " + logFilePath);
		
		//prefs = getSharedPreferences(Constants.PREFS,MODE_PRIVATE);
		prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		editor = prefs.edit();
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
		
		showHexTab = prefs.getBoolean(getString(R.string.pref_enable_hex),
				getApplicationContext().getResources().getBoolean(R.bool.default_pref_enable_hex));
		debug("onCreate: showHexTab = " + showHexTab);
		if((!showHexTab) && (actionBar.getTabCount() == 2)) { //Hide the Hex Tab
			debug("onCreate: Remove Hex Tab");
			actionBar.removeTabAt(1);
		}
		
		listen = prefs.getBoolean(getString(R.string.pref_bluetooth_listen),
				getApplicationContext().getResources().getBoolean(R.bool.default_pref_bluetooth_listen));
		debug("onCreate: listen = " + listen);
		
		secureConnection = prefs.getBoolean(getString(R.string.pref_secure_connection), 
				getApplicationContext().getResources().getBoolean(R.bool.default_pref_secure_connection));
		debug("onCreate: secureConnection = " + secureConnection);
		
		logAsciiData = prefs.getBoolean(getString(R.string.pref_log_ascii_data), 
				getApplicationContext().getResources().getBoolean(R.bool.default_pref_log_ascii_data));
		debug("onCreate: logData = " + logAsciiData);
		
		logHexData = prefs.getBoolean(getString(R.string.pref_log_hex_data), 
				getApplicationContext().getResources().getBoolean(R.bool.default_pref_log_hex_data));

		discoverable_timeout_str = prefs.getString(getString(R.string.pref_discoverable_timeout), getString(R.string.discoverable_default_timeout));
		//debug("onCreate: discoverable_timeout_str = " + discoverable_timeout_str);
		discoverable_timeout = Integer.valueOf(discoverable_timeout_str);
		debug("onCreate: discoverable_timeout = " + discoverable_timeout);
		
		display_font_str = prefs.getString(getString(R.string.pref_display_font), getString(R.string.display_font_default));
		display_font = Integer.valueOf(display_font_str);
		debug("onCreate: display_font = " + display_font);
		txEditText.setTextSize(TypedValue.COMPLEX_UNIT_PT, display_font);
		
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
		this.registerReceiver(mBluetoothEventReceiver, filter);
		
		IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
		this.registerReceiver(mBluetoothEventReceiver, filter2);
		
		IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		this.registerReceiver(mBluetoothEventReceiver, filter3);
		
		IntentFilter filter4 = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
		this.registerReceiver(mBluetoothEventReceiver, filter4);

	}
		
	
	@Override 
	public void onStart() {
		super.onStart();
		debug("onStart: btState = " + btState);
		if(!mBTAdapter.isEnabled()) {
			//Request to turn on Bluetooth if not already switched ON
			Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBTIntent, Constants.REQUEST_ENABLE_BT);
		}
	}

	@Override
	protected void onDestroy() {
		boolean btenabled = prefs.getBoolean(Constants.PREF_BT_ENABLED, false);
		if((btState == Constants.BT_CONNECTED) && (connectedTask != null)) {
			connectedTask.cancel(true);
		}
		if((btState == Constants.BT_LISTENING) && (listenTask != null)) {
			//listenTask.cancel(true);
			closeServerSocket();
		}
		if(logAsciiFileOpen && (fileOutStream != null)) {
			stopBluetoothLogging();
		}
		if(logHexFileOpen && (hexFileOutStream != null)) {
			stopBluetoothHexLogging();
		}
		debug("onDestroy: BT Enabled: " + btenabled);
		if(btenabled && mBTAdapter.isEnabled()) {
			Toast.makeText(getApplicationContext(),"Disabling Bluetooth..",
					Toast.LENGTH_SHORT).show();
			debug("Disabling Bluetooth...");
			mBTAdapter.disable();
			
		}
		editor.putBoolean(Constants.PREF_BT_ENABLED, false);
		editor.commit();
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);
		this.unregisterReceiver(mBluetoothEventReceiver);
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.abt, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch(item.getItemId()){
		case R.id.action_settings:
			debug("Menu:Settings Selected");
			//Intent settingsIntent = new Intent(this, SampleActivity.class);
			//startActivityForResult(settingsIntent, Constants.REQUEST_SETTINGS);
			startActivity(new Intent(this, SettingsActivity.class));
			//startActivity(new Intent(this, SampleActivity.class));
			break;
		case R.id.action_connect:
			debug("Menu: Connect Selected");
			if((btState == Constants.BT_IDLE) || (btState == Constants.BT_LISTENING)) {
				startActivityForResult(new Intent(this,ABTConnectActivity.class),Constants.REQUEST_SEARCH);
			}
			else if(btState == Constants.BT_CONNECTED) {
				if(connectedTask != null) {
					connectedTask.cancel(true);
				}
			}
			break;
		case R.id.led_indication:
			debug("Nothing to be done for server LED indication");
			break;
		case R.id.action_send_file:
			debug("Menu: Send File Selected");
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}
	 
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		debug("onPrepareOptions Menu Called");
		MenuItem connect = menu.findItem(R.id.action_connect);
		MenuItem serverLED = menu.findItem(R.id.led_indication);
		MenuItem sendFile = menu.findItem(R.id.action_send_file);
		listen = prefs.getBoolean(getString(R.string.pref_bluetooth_listen),
				getApplicationContext().getResources().getBoolean(R.bool.default_pref_bluetooth_listen));
		if(btState == Constants.BT_CONNECTED) {
			connect.setIcon(R.drawable.connected);
			serverLED.setIcon(R.drawable.red_led);
			if(menu.findItem(R.id.action_send_file) == null)
				menu.add(sendFile.getGroupId(), sendFile.getItemId(), sendFile.getOrder(), sendFile.getTitle());
		}
		else {
			connect.setIcon(R.drawable.search);
			menu.removeItem(R.id.action_send_file);
			
			if((listen == true) && (!this.isFinishing())) { //restart Bluetooth Server if Listen option was enabled
				if((listenTask != null) && (listenTask.getStatus() == AsyncTask.Status.RUNNING)) {
					serverLED.setIcon(R.drawable.green_led);
					debug("onPrepareOptionsMenu: Bluetooth Server already running");
				}
				else {
					int ret = startBluetoothServer();
					if((ret == Constants.STATUS_SUCCESS) || (ret == Constants.STATUS_SERVER_ALREADY_RUNNING)) {
						debug("onPrepareOptions: Bluetooth Server restarted");
						serverLED.setIcon(R.drawable.green_led);
					}
					else if(ret == Constants.STATUS_WAITING_DISCOVERABILITY) {
						debug("onPrepareOptions: Waiting for discoverability to be ON before starting server");
					}
					else {
						Log.e(TAG,"onPrepareOptions: Bluetooth Server failed to restart");
					}
				}
			}
		}
		
		
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		debug("onActivityResult: requestCode " + requestCode + " resultCode " + resultCode);
		switch(requestCode) {
		case Constants.REQUEST_ENABLE_BT:
			if(resultCode == Activity.RESULT_OK) {
				debug("onActivityResult: Bluetooth Enabled Successfully. Commiting BT_ENABLED = true");
				editor.putBoolean(Constants.PREF_BT_ENABLED, true);
				editor.commit();
				if((listen == true) && (!this.isFinishing())) {
					int ret = startBluetoothServer();
					if((ret == Constants.STATUS_SUCCESS) || (ret == Constants.STATUS_SERVER_ALREADY_RUNNING)) {
						debug("REQUEST_ENABLE_BT: Bluetooth Server restarted");
						invalidateOptionsMenu();
					}
					else if(ret == Constants.STATUS_WAITING_DISCOVERABILITY) {
						debug("REQUEST_ENABLE_BT: Waiting for discoverability to be ON before starting server");
					}
					else {
						Log.e(TAG,"REQUEST_ENABLE_BT: Bluetooth Server failed to restart");
					}
				}
			}
			else {
				Log.e(TAG,"ERROR: Bluetooth Not Enabled.. Activity cannot start");
				Toast.makeText(this, "Bluetooth not enabled. Application exiting", Toast.LENGTH_SHORT).show();
				finish();
			}
			break;
		case Constants.REQUEST_SETTINGS:
			if(requestCode == Activity.RESULT_OK) {
				debug("Settings Activity finished");
			}
			break;
		case Constants.REQUEST_SEARCH:
			if(resultCode == Activity.RESULT_OK) {
				//First close server socket if it is running
				if((listenTask != null) && (listenTask.getStatus() == AsyncTask.Status.RUNNING)) {
					debug("onActivityResult: REQUEST_SEARCH, cancelling Bluetooth Server Task");
					//listenTask.cancel(true);
					if(closeServerSocket()) {
						debug("onSharedPreferenceChanged: server socket closed successfully");
						//invalidateOptionsMenu();
					}
					else {
						Log.e(TAG,"onSharedPreferenceChanged: ERROR, failed to close server socket");
					}
				}
				if((data != null) && data.hasExtra(Constants.EXTRA_DEVICE_ADDRESS)) {
					String addr = data.getExtras().getString(Constants.EXTRA_DEVICE_ADDRESS);
					debug("Search Activity returned address = " + addr);
					connectTask = new ConnectTask();
					connectTask.execute(addr);
				}
			}
			break;
		case Constants.REQUEST_DISCOVERABLE:
			if((resultCode == Activity.RESULT_FIRST_USER) || (resultCode == discoverable_timeout)) { //RESULT_FIRST_USER returned when Discoverability time is indefinite
				debug("Device discoverable for " + ((resultCode == Activity.RESULT_FIRST_USER)?"indefinite duration":discoverable_timeout) +
						"..starting Bluetooth Server");
				//call start Bluetooth server again this time to start listening on socket
				int ret = startBluetoothServer();
				if((ret == Constants.STATUS_SUCCESS) || (ret == Constants.STATUS_SERVER_ALREADY_RUNNING)) {
					debug("REQUEST_DISCOVERABLE: Bluetooth Server restarted");
					invalidateOptionsMenu();
				}
				else if(ret == Constants.STATUS_WAITING_DISCOVERABILITY) {
					Log.e(TAG,"REQUEST_DISCOVERABLE: Waiting for discoverability to be ON before starting server. " +
							"Control should never come here");
				}
				else {
					Log.e(TAG,"REQUEST_DISCOVERABLE: Bluetooth Server failed to restart");
				}
			}
			else {
				Log.e(TAG,"Device failed to enter discoverability mode. Server Socket not initialized");
				Toast.makeText(getApplicationContext(), "Bluetooth Server failed to start", 
						Toast.LENGTH_SHORT).show();
				editor.putBoolean(getString(R.string.pref_bluetooth_listen), false);
				editor.commit();
			}
			break;
		default:
			break;
		}
	}
	
	public static int getBTState() {
		return btState;
	}
	
	@Override
	public void onTabSelected(ActionBar.Tab tab,FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		debug("onTabSelected: " + tab.getPosition());
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,FragmentTransaction fragmentTransaction) {
	}

	@Override 
	public void onTabReselected(ActionBar.Tab tab,FragmentTransaction fragmentTransaction) {
	}

	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if(key.equals(getString(R.string.pref_bluetooth_listen))) {
			listen = sharedPreferences.getBoolean(key, 
					getApplicationContext().getResources().getBoolean(R.bool.default_pref_bluetooth_listen));
			debug("Listen Preference Changed: listen = " + listen);
			if(listen == true)  { //State IDLE, Start Server
				int ret = startBluetoothServer();
				if((ret == Constants.STATUS_SUCCESS) || (ret == Constants.STATUS_SERVER_ALREADY_RUNNING)) {
					debug("Bluetooth Server Started successfully");
					invalidateOptionsMenu();
				}
				else if(ret == Constants.STATUS_WAITING_DISCOVERABILITY) {
					debug("Waiting for discoverability to turn ON before starting server");
				}
				else {
					Log.e(TAG,"Bluetooth Server failed to start");
				}
			}
			else { //Stop Bluetooth Server
				if((listenTask != null) && (listenTask.getStatus() == AsyncTask.Status.RUNNING)) {
					debug("onSharedPreferenceChanged: Close Bluetooth Server Socket");
					//listenTask.cancel(true);
					if(closeServerSocket()) {
						debug("onSharedPreferenceChanged: server socket closed successfully");
						//invalidateOptionsMenu();
					}
					else {
						Log.e(TAG,"onSharedPreferenceChanged: ERROR, failed to close server socket");
					}
				}
				else if(listenTask == null) {
					Log.e(TAG,"ListenTask = null");
				}
				else {
					Log.e(TAG,"ListenTask not running");
				}
			}
		}
		else if(key.equals(getString(R.string.pref_enable_hex))) {
			showHexTab = sharedPreferences.getBoolean(key, 
					getApplicationContext().getResources().getBoolean(R.bool.default_pref_enable_hex));
			final ActionBar actionBar = getActionBar();
			debug("Enable Hex Preference Changed: enableHex = " + showHexTab + " tabCount = " + actionBar.getTabCount());
			if(showHexTab) {
				if(actionBar.getTabCount() == 1) {
					actionBar.addTab(actionBar.newTab().setText(mSectionsPagerAdapter.getPageTitle(1))
							.setTabListener(this));
				}
			}
			else {
				if(actionBar.getTabCount() == 2) {
					actionBar.removeTabAt(1);
				}
			}
		}
		else if(key.equals(getString(R.string.pref_secure_connection))) {
			secureConnection = sharedPreferences.getBoolean(key, 
					getApplicationContext().getResources().getBoolean(R.bool.default_pref_secure_connection));
			debug("Secure Connection preference changed: secureConnection = " + secureConnection);
		}
		else if(key.equals(getString(R.string.pref_discoverable_timeout))) {

			discoverable_timeout_str = sharedPreferences.getString(key, getString(R.string.discoverable_default_timeout));
			discoverable_timeout = Integer.valueOf(discoverable_timeout_str);
			debug("discoverable timeout preference changed: " + discoverable_timeout);
			if((discoverable_timeout < Constants.MIN_DISCOVERABLE_DUR) || (discoverable_timeout > Constants.MAX_DISCOVERABLE_DUR)) {
				Log.e(TAG,"Discoverable duration " + discoverable_timeout + " is outside valid range 10-300 s. Using default value 0");
				discoverable_timeout = Constants.DISCOVERABLE_DUR_INDEFINITE;
				editor.putString(getString(R.string.pref_discoverable_timeout), getString(R.string.discoverable_default_timeout));
				editor.commit();
			}
			
		}
		else if(key.equals(getString(R.string.pref_display_font))) {
			display_font_str = sharedPreferences.getString(key, getString(R.string.display_font_default));
			display_font = Integer.valueOf(display_font_str);
			debug("display font preference changed: " + display_font);
			txEditText.setTextSize(TypedValue.COMPLEX_UNIT_PT, display_font);
			AsciiSectionFragment ascii =(AsciiSectionFragment) getSupportFragmentManager().
					findFragmentByTag(mSectionsPagerAdapter.makeFragmentName(0));
			if(ascii != null)
				ascii.setAsciiTextFontSize(display_font);
			else 
				Log.e(TAG,"onSharedPreferenceChanged: ERROR AsciiFragment = null");
			
			HexSectionFragment hex =(HexSectionFragment) getSupportFragmentManager().
					findFragmentByTag(mSectionsPagerAdapter.makeFragmentName(1));
			if(hex != null)
				hex.setHexTextFontSize(display_font);
			else 
				Log.e(TAG,"onSharedPreferenceChanged: ERROR HexFragment = null");
		}
		else if(key.equals(getString(R.string.pref_log_ascii_data))) {
			logAsciiData = sharedPreferences.getBoolean(key, 
					getApplicationContext().getResources().getBoolean(R.bool.default_pref_log_ascii_data));
			debug("Log ASCII Data preference changed: logAsciiData = " + logAsciiData );
			if(!logAsciiData) {
				if(stopBluetoothLogging() != Constants.STATUS_SUCCESS) {
					Log.e(TAG,"Log ASCII Data preference changed: Failed to stop Bluetooth ASCII Logging");
				}
			}
		}
		else if(key.equals(getString(R.string.pref_log_hex_data))) {
			logHexData = sharedPreferences.getBoolean(key, 
					getApplicationContext().getResources().getBoolean(R.bool.default_pref_log_hex_data));
			debug("Log HEX Data preference changed: logHexData = " + logHexData);
			if(!logHexData) {
				if(stopBluetoothHexLogging() != Constants.STATUS_SUCCESS) {
					Log.e(TAG,"Log HEX Data preference changed: Failed to stop Bluetooth HEX Logging");
				}
			}
		}
	}
	
	private final BroadcastReceiver mBluetoothEventReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
				debug("ACTION_ACL_CONNECTED");
			}
			else if(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
				debug("ACTION_ACL_DISCONNECT_REQUESTED");
			}
			else if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
				debug("ACTION_ACL_DISCONNECTED");
				//disconnect();
				if(btState == Constants.BT_CONNECTED) {
					if((connectedTask != null) && (connectedTask.getStatus() == AsyncTask.Status.RUNNING)) {
						debug("ACL Disconnect Event: Cancelling connected task and closing Bluetooth connection");
						connectedTask.cancel(true);
					}
				}
			}
			else if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
				debug("ACTION_SCAN_MODE_CHANGED");
				if(intent.hasExtra(BluetoothAdapter.EXTRA_SCAN_MODE)) {
					Bundle extras = intent.getExtras();
					int scanMode = extras.getInt(BluetoothAdapter.EXTRA_SCAN_MODE);
					switch(scanMode) {
					case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
					case BluetoothAdapter.SCAN_MODE_NONE:
						debug("ScanMode = " + ((scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE) ? "connectable": "none"));
						if((listenTask != null) && (listenTask.getStatus() == AsyncTask.Status.RUNNING)) {
							debug("ACTION_SCAN_MODE_CHANGED: Close Bluetooth Server Socket");
							editor.putBoolean(getString(R.string.pref_bluetooth_listen), false);
							editor.commit();
						}
						else if(listenTask == null) {
							Log.e(TAG,"ACTION_SCAN_MODE_CHANGED: ListenTask = null");
						}
						else {
							Log.e(TAG,"ACTION_SCAN_MODE_CHANGED: ListenTask not running");
						}
						break;
					case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
						debug("ScanMode = connectable_discoverable");
						break;
					}
					
					int prevscanMode = extras.getInt(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE);
					switch(prevscanMode) {
					case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
						debug("PrevScanMode = connectable");
						break;
					case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
						debug("PrevScanMode = connectable_discoverable");
						break;
					case BluetoothAdapter.SCAN_MODE_NONE:
						debug("PrevScanMode = none");
						break;
					}
				}
			}
			
		}
	};
	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			debug("SectionsPagerAdapter: getItem "+position);
			Fragment fragment = null;
			Bundle args;
			switch(position) {
			case 0: //ASCII
				fragment = new AsciiSectionFragment();
				args = new Bundle();
				args.putInt(AsciiSectionFragment.ARG_SECTION_NUMBER, position + 1);
				fragment.setArguments(args);
				break;
			case 1: //Hex
				fragment = new HexSectionFragment();
				args = new Bundle();
				args.putInt(HexSectionFragment.ARG_SECTION_NUMBER, position + 1);
				fragment.setArguments(args);
				break;
			}
			return fragment;
			
		}

		@Override
		public int getCount() {
			// Show total pages.
			return Constants.TOTAL_TABS;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_ascii_section).toUpperCase(l);
			case 1:
				return getString(R.string.title_hex_section).toUpperCase(l);
			case 2:
				return getString(R.string.title_section3).toUpperCase(l);
			}
			return null;
		}
		
		private String makeFragmentName(int position) {
		     return "android:switcher:" + R.id.pager + ":" + position;
		}
	}

	/**
	 * A dummy fragment representing a section of the app, but that simply
	 * displays dummy text.
	 */
	public static class AsciiSectionFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "ascii_section_number";
		private TextView asciiTV;
		private ScrollView asciiSV;

		public AsciiSectionFragment() {
			if(DEBUG)
				Log.d(TAG,"AsciiSectionFragment Constructor");
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_abt_ascii,container, false);
			//TextView dummyTextView = (TextView) rootView.findViewById(R.id.ascii_content);
			//dummyTextView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
			asciiTV = (TextView) rootView.findViewById(R.id.ascii_content);
			asciiSV = (ScrollView)rootView.findViewById(R.id.ascii_content_scroll);
			if(DEBUG)
				Log.d(TAG, "ASCII onCreateView called");
			//asciiRootView = rootView;
			setAsciiTextFontSize(display_font);
			return rootView;
		}
		
		
		public void setRxAsciiText(String data) {

			if(asciiTV == null) {
				Log.e(TAG, "asciiTV = null");
			}
			else {
				Spannable rxAsciiTxt;
				if((asciiDataState == Constants.DATA_STATE_TX) ||
						((asciiDataState == Constants.DATA_STATE_IDLE) && (asciiTV.getText().length() != 0))) {
					rxAsciiTxt = new SpannableString(Constants.LF + data);
					//asciiTV.append(Constants.LF+data);
				}
				else {
					rxAsciiTxt = new SpannableString(data);
					//asciiTV.append(data);
				}
				rxAsciiTxt.setSpan(new ForegroundColorSpan(Color.BLUE), 0, rxAsciiTxt.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				asciiTV.append(rxAsciiTxt);
				asciiDataState = Constants.DATA_STATE_RX;
			}
			if(asciiSV == null) {
				Log.e(TAG,"asciiSV = null");
			}
			else {
				asciiSV.fullScroll(View.FOCUS_DOWN);
			}
		}
		
		public void setTxAsciiText(String data) {
			if(asciiTV == null) {
				Log.e(TAG, "asciiTV = null");
			}
			else {
				Spannable txAsciiTxt;
				if((asciiDataState == Constants.DATA_STATE_RX) ||
						((asciiDataState == Constants.DATA_STATE_IDLE) && (asciiTV.getText().length() != 0))){
					txAsciiTxt = new SpannableString(Constants.LF+data);
				}
				else {
					txAsciiTxt = new SpannableString(data);
				}
				txAsciiTxt.setSpan(new ForegroundColorSpan(Color.MAGENTA), 0, txAsciiTxt.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				asciiTV.append(txAsciiTxt);
				asciiDataState = Constants.DATA_STATE_TX;
			}
			if(asciiSV == null) {
				Log.e(TAG,"asciiSV = null");
			}
			else {
				asciiSV.fullScroll(View.FOCUS_DOWN);
			}
		}
		
		public void clearAsciiText() {
			
			if(asciiTV == null) {
				Log.e(TAG, "asciiTV = null");
			}
			else {
				asciiTV.setText("");
			}
		}
		
		public void setAsciiTextFontSize(int fontSize) {
			if(asciiTV == null) {
				Log.e(TAG,"asciiTV = null");
			}
			else {
				asciiTV.setTextSize(TypedValue.COMPLEX_UNIT_PT, fontSize);
			}
		}
	}
	
	public static class HexSectionFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "hex_section_number";
		private TextView hexTV;
		private ScrollView hexSV;

		public HexSectionFragment() {
			if(DEBUG)
				Log.d(TAG,"HexSectionFragment Constructor");
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_abt_hex,container, false);
			hexTV = (TextView) rootView.findViewById(R.id.hex_content);
			hexSV = (ScrollView) rootView.findViewById(R.id.hex_content_scroll);
			//dummyTextView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
			if(DEBUG)
				Log.d(TAG, "HEX onCreateView called");
			setHexTextFontSize(display_font);
			return rootView;
		}
		
		//public void setRxHexText(byte[] data, int length) {
		public void setRxHexText(String data) {
			//if(DEBUG)
			//	Log.d(TAG,"+setTxText: data = " + data);

			if(hexTV == null) {
				Log.e(TAG, "hexTV = null");
			}
			else {
				Spannable rxHexTxt;
				if((hexDataState == Constants.DATA_STATE_TX) ||
						((hexDataState == Constants.DATA_STATE_IDLE) && (hexTV.getText().length() != 0))) {
					rxHexTxt = new SpannableString(Constants.LF+data); 
					//hexTV.append(Constants.LF+data);
				}
				else {  
					rxHexTxt = new SpannableString(data); 
					//hexTV.append(data);
				}
				rxHexTxt.setSpan(new ForegroundColorSpan(Color.BLUE), 0, rxHexTxt.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				hexTV.append(rxHexTxt);
				//for(int i=0; i<length; i++) {
				//	hexTV.append(Integer.toHexString(data[i])+ ' ');
				//}
				hexDataState = Constants.DATA_STATE_RX;
			}
			if(hexSV == null) {
				Log.e(TAG,"hexSV = null");
			}
			else {
				hexSV.fullScroll(View.FOCUS_DOWN);
			}
		}
		
		public void setTxHexText(byte[] data, int length) {
			//if(DEBUG)
			//	Log.d(TAG,"+setTxText: data = " + data);

			if(hexTV == null) {
				Log.e(TAG, "hexTV = null");
			}
			else {
				if((hexDataState == Constants.DATA_STATE_RX) || 
						((hexDataState == Constants.DATA_STATE_IDLE) && (hexTV.getText().length() != 0))) {
					hexTV.append(Constants.LF);
				}
				Spannable txHexTxt = new SpannableString(Utils.toHexStringUsingCharArray(data)); 
				txHexTxt.setSpan(new ForegroundColorSpan(Color.MAGENTA), 0, txHexTxt.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				//hexTV.append(Utils.toHexStringUsingCharArray(data));
				hexTV.append(txHexTxt);
				hexDataState = Constants.DATA_STATE_TX;	
				
			}
			if(hexSV == null) {
				Log.e(TAG,"hexSV = null");
			}
			else {
				hexSV.fullScroll(View.FOCUS_DOWN);
			}
		}
		
		public void clearHexText() {
			if(hexTV == null) {
				Log.e(TAG, "hexTV = null");
			}
			else {
				hexTV.setText("");
			}
		}
		
		public void setHexTextFontSize(int fontSize) {
			if(hexTV == null) {
				Log.e(TAG,"asciiTV = null");
			}
			else {
				hexTV.setTextSize(TypedValue.COMPLEX_UNIT_PT, fontSize);
			}
		}
	}
	
	public boolean disconnect() {
		if(btInStream != null) {
			debug("Closing Input Stream..");
			try {
				btInStream.close();
			}
			catch(IOException ioe) {
				Log.e(TAG,"Failed to close input stream. ERROR: " + ioe);
				return false;
			}
			btInStream = null;
		}
		if(btOutStream != null) {
			debug("Closing Output Stream..");
			try {
				btOutStream.close();
			}
			catch(IOException ioe) {
				Log.e(TAG,"Failed to close output stream. ERROR: " + ioe);
				return false;
			}
			btOutStream = null;
		}
		if(btSocket != null) {
			debug("Closing BTSocket..");
			try {
				btSocket.close();
			}
			catch(IOException ioe) {
				Log.e(TAG,"Failed to close output stream. ERROR: " + ioe);
				return false;
			}
			btSocket = null;
		}
		return true;
	}
	
	public int startBluetoothServer() {
		if(btState == Constants.BT_IDLE) {
			if(mBTAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
				debug("startBluetoothServer: Bluetooth not discoverable, enabling Discoverability mode");
				Intent discoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				//discoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, Constants.DISCOVERABLE_DUR_INDEFINITE);
				discoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, discoverable_timeout);
				startActivityForResult(discoverable,Constants.REQUEST_DISCOVERABLE);
				return Constants.STATUS_WAITING_DISCOVERABILITY;
			}
			else if((listenTask != null) && (listenTask.getStatus() == AsyncTask.Status.RUNNING)) {
				Log.e(TAG,"startBluetoothServer: ERROR, Start Bluetooth server called when server already running");
				return Constants.STATUS_SERVER_ALREADY_RUNNING;
			}
			else {
				listenTask = new ListenTask();
				listenTask.execute();
				return Constants.STATUS_SUCCESS;
			}
		}
		else {
			Log.e(TAG,"startBluetoothServer: Cannot Start Bluetooth Server in non idle state: " + Utils.getBTState(btState));
			return Constants.STATUS_LISTEN_FAIL;
		}
	}
	
	public boolean closeServerSocket() {
		if(btServerSocket != null) {
			try {
				btServerSocket.close();
			}
			catch(IOException ioe) {
				Log.e(TAG,"Bluetooth Server Socket close failed: " + ioe.getMessage());
				return false;
			}
			btServerSocket = null;
		}
		else {
			Log.e(TAG,"closeServerSocket: BTServerSocket = null");
			return false;
		}
		return true;
	}
	
	public int logBluetoothData(String data) {
		
		if(!logAsciiFileOpen) {
			mounted = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
			if(!mounted) {
				Log.e(TAG, "logBluetoothData: Media FileSystem Not Mounted: Logs will not be saved");
				Toast.makeText(this, "Media FileSystem Not Mounted: Logs will not be saved",Toast.LENGTH_LONG).show();
				return Constants.STATUS_FS_NOT_MOUNTED;
			}
			if(!logAsciiData) {
				Log.e(TAG, "logBluetoothData: Logging flag not enabled");
				return Constants.STATUS_LOGGING_NOT_ENABLED;
			}
			Calendar c = Calendar.getInstance();
			logFileName = String.format(Locale.getDefault(),"%s%02d%02d%04d_%02d%02d%02d.txt", 
					Constants.ASCII_LOG_FILE_NAME,c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.MONTH)+1
					,c.get(Calendar.YEAR),c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),c.get(Calendar.SECOND));
			
			//Check if directory is present
			path = new File(logFilePath);
			path.mkdirs();
			debug("logBluetoothData: directory path " + logFilePath + " created");
			
			logAsciiFile = new File(logFilePath, logFileName);
			debug("logBluetoothData: file  " + logFilePath + "/" + logFileName + " created");
			
			try {
				fileWriter = new FileWriter(logAsciiFile);
				fileOutStream = new BufferedWriter(fileWriter);
			}
			catch(IOException ioe) {
				Log.e(TAG,"logBluetoothData: Failed to open File Output Stream : " + ioe.getMessage());
				return Constants.STATUS_LOG_FILE_FAILED;
			}
			logAsciiFileOpen = true;
		}
		
		if(logAsciiFile != null) {
			try {
				//fileOutStream = new FileOutputStream(logFile);
				//fileOutStream.write(data);
				

	            fileOutStream.append(data);
				
			}
			catch(IOException ioe) {
				Log.e(TAG,"logBluetoothData: Failed to open File Output Stream : " + ioe.getMessage());
				return Constants.STATUS_LOG_FILE_FAILED;
			}
		}
		else {
			Log.e(TAG,"logBluetoothData: logFile = null");
			return Constants.STATUS_LOG_FILE_FAILED;
		}
		return Constants.STATUS_SUCCESS;
	}
	
	public int stopBluetoothLogging() {
		if(fileOutStream != null) {
			try {
				debug("Closing log file stream");
				fileOutStream.close();
			}
			catch (IOException ioe) {
				Log.e(TAG,"Failed to stop logging: ERROR " + ioe.getMessage());
				return Constants.STATUS_STOP_LOGGING_FAILED;
			}
		}
		if(fileWriter != null) {
			try {
				debug("Closing log file writer");
				fileWriter.close();
			}
			catch (IOException ioe) {
				Log.e(TAG,"Failed to close file writer: ERROR " + ioe.getMessage());
				return Constants.STATUS_STOP_LOGGING_FAILED;
			}
		}
		logAsciiFileOpen = false;
		fileOutStream = null;
		fileWriter = null;
		return Constants.STATUS_SUCCESS;
	}
	
	public int logBluetoothHexData(String data) {
		
		if(!logHexFileOpen) {
			mounted = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
			if(!mounted) {
				Log.e(TAG, "logBluetoothHexData: Media FileSystem Not Mounted: Logs will not be saved");
				Toast.makeText(this, "Media FileSystem Not Mounted: Logs will not be saved",Toast.LENGTH_LONG).show();
				return Constants.STATUS_FS_NOT_MOUNTED;
			}
			if(!showHexTab || !logHexData) {
				Log.e(TAG, "logBluetoothHexData: Logging flag not enabled");
				return Constants.STATUS_LOGGING_NOT_ENABLED;
			}
			Calendar c = Calendar.getInstance();
			logHexFileName = String.format(Locale.getDefault(),"%s%02d%02d%04d_%02d%02d%02d.txt", 
					Constants.HEX_LOG_FILE_NAME,c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.MONTH)+1
					,c.get(Calendar.YEAR),c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),c.get(Calendar.SECOND));
			
			//Check if directory is present
			path = new File(logFilePath);
			path.mkdirs();
			debug("logBluetoothHexData: directory path " + logFilePath + " created");
			
			logHexFile = new File(logFilePath, logHexFileName);
			debug("logBluetoothHexData: file  " + logFilePath + "/" + logHexFileName + " created");
			
			try {
				hexFileWriter = new FileWriter(logHexFile);
				hexFileOutStream = new BufferedWriter(hexFileWriter);
			}
			catch(IOException ioe) {
				Log.e(TAG,"logBluetoothHexData: Failed to open File Output Stream : " + ioe.getMessage());
				return Constants.STATUS_LOG_FILE_FAILED;
			}
			logHexFileOpen = true;
		}
		
		if(logHexFile != null) {
			try {
	            hexFileOutStream.append(data);
				
			}
			catch(IOException ioe) {
				Log.e(TAG,"logBluetoothHexData: Failed to open File Output Stream : " + ioe.getMessage());
				return Constants.STATUS_LOG_FILE_FAILED;
			}
		}
		else {
			Log.e(TAG,"logBluetoothHexData: logHexFile = null");
			return Constants.STATUS_LOG_FILE_FAILED;
		}
		return Constants.STATUS_SUCCESS;
	}

	public int stopBluetoothHexLogging() {
		if(hexFileOutStream != null) {
			try {
				debug("Closing log file stream");
				hexFileOutStream.close();
			}
			catch (IOException ioe) {
				Log.e(TAG,"Failed to stop logging: ERROR " + ioe.getMessage());
				return Constants.STATUS_STOP_LOGGING_FAILED;
			}
		}
		if(hexFileWriter != null) {
			try {
				debug("Closing log file writer");
				hexFileWriter.close();
			}
			catch (IOException ioe) {
				Log.e(TAG,"Failed to close file writer: ERROR " + ioe.getMessage());
				return Constants.STATUS_STOP_LOGGING_FAILED;
			}
		}
		logHexFileOpen = false;
		hexFileOutStream = null;
		hexFileWriter = null;
		return Constants.STATUS_SUCCESS;
	}
	
	public void runMediaScanner() {
		if(logAsciiFile != null) {
			MediaScannerConnection.scanFile(this, new String[] {logAsciiFile.toString()}, null, 
				new MediaScannerConnection.OnScanCompletedListener() {
					
					@Override
					public void onScanCompleted(String path, Uri uri) {
						// TODO Auto-generated method stub
						debug("MediaScan Completed: " + path + ":" + " uri->" + uri);
					}
				});
		}
	}
	
	class ConnectTask extends AsyncTask<String, Void, Integer> {
		
		private ProgressDialog progress;
		
		public ConnectTask() {
			progress = new ProgressDialog(ABTActivity.this);
			progress.setCanceledOnTouchOutside(false);
		}
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			debug("ConnectTask : Pre-execute");
			super.onPreExecute();
			this.progress.setMessage(" Creating Bluetooth Connection. Please Wait...");
			this.progress.show();
		}
		
		@Override
		protected Integer doInBackground(String... params) {
			debug("address params = " + params[0]);

			if(!BluetoothAdapter.checkBluetoothAddress(params[0])) {
				Log.e(TAG, "Invalid Bluetooth Address: " + params[0]);
				return Integer.valueOf(Constants.STATUS_INVALID_BDADDR);
			}
			remDev = mBTAdapter.getRemoteDevice(params[0]);
			if(remDev == null) {
				Log.e(TAG,"Remote address null");
				return Integer.valueOf(Constants.STATUS_REMADDR_NULL);
			}
			btState = Constants.BT_CONNECTING;
			try {
				if(secureConnection) {
					debug("Creating Secure RFCOMM connection");
					btSocket = remDev.createRfcommSocketToServiceRecord(Constants.SPP_UUID);
				}
				else {
					debug("Creating Insecure RFCOMM connection");
					btSocket = remDev.createInsecureRfcommSocketToServiceRecord(Constants.SPP_UUID);
				}
			}
			catch(IOException ioe) {
				Log.e(TAG,"Failed to create RFCOMM connection to remote device: "+ params[0]);
				btSocket = null;
				return Constants.STATUS_CONNECT_FAIL;
			}
			if(btSocket != null) {
				debug("Creating bluetooth connection to remote device " + params[0]);
				mBTAdapter.cancelDiscovery();
				try {
					btSocket.connect();
				}
				catch(IOException ioe) {
					Log.e(TAG, "Failed to connect RFCOMM socket: " + ioe.getMessage());
					try {
						btSocket.close();
					}
					catch(Exception ioe2) {
						Log.e(TAG,"Failed to close RFCOMM socket: " + ioe2.getMessage());
						return Constants.STATUS_CONNECT_FAIL;
					}
					return Constants.STATUS_CONNECT_FAIL;
				}
				debug("Bluetooth Connection Success. Obtaining Input and Output Streams");
				
				try {
					btInStream = btSocket.getInputStream();
				}
				catch(IOException ioe) {
					Log.e(TAG,"Failed to obtain Input Stream: " + ioe.getMessage());
					try {
						btSocket.close();
					}
					catch(Exception ioe2) {
						Log.e(TAG,"Failed to close RFCOMM socket: " + ioe2.getMessage());
						return Constants.STATUS_OBTAIN_IS_FAIL;
					}
					return Constants.STATUS_OBTAIN_IS_FAIL;
				}
				debug("Obtained Input Stream");
				
				try {
					btOutStream = btSocket.getOutputStream();
				}
				catch(IOException ioe) {
					Log.e(TAG,"Failed to obtain Output Stream: " + ioe.getMessage());
					try {
						btSocket.close();
						btInStream.close();
					}
					catch(Exception ioe2) {
						Log.e(TAG,"Failed to close RFCOMM socket: " + ioe2.getMessage());
						return Constants.STATUS_OBTAIN_OS_FAIL;
					}
					return Constants.STATUS_OBTAIN_OS_FAIL;
				}
				debug("Obtained Output Stream");
				return Integer.valueOf(Constants.STATUS_SUCCESS);
			}
			
			return Constants.STATUS_CONNECT_FAIL;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			debug("ConnectTask PostExecute : result = " + Utils.getResult(result.intValue()));
			if(progress.isShowing())
				progress.cancel();
			
			switch(result.intValue()) {
			case Constants.STATUS_SUCCESS:
				btState = Constants.BT_CONNECTED;
				Toast.makeText(getApplicationContext(), "Bluetooth Connection Successful", 
						Toast.LENGTH_SHORT).show();
				connectedTask = new ConnectedTask();
				connectedTask.execute();
				break;
			case Constants.STATUS_INVALID_BDADDR:
			case Constants.STATUS_REMADDR_NULL:
				btState = Constants.BT_IDLE;
				Toast.makeText(getApplicationContext(), "Invalid Bluetooth Address: " + remDev.getAddress(), 
						Toast.LENGTH_SHORT).show();
				break;
			case Constants.STATUS_CONNECT_FAIL:
				btState = Constants.BT_IDLE;
				Toast.makeText(getApplicationContext(), "Failed to create RFCOMM Connection to : " 
				+ remDev.getAddress(), Toast.LENGTH_SHORT).show();
				break;
			case Constants.STATUS_OBTAIN_IS_FAIL:
				btState = Constants.BT_IDLE;
				Toast.makeText(getApplicationContext(), "Failed to create obtain Input Stream : " 
				+ remDev.getAddress(), Toast.LENGTH_SHORT).show();
				break;
			case Constants.STATUS_OBTAIN_OS_FAIL:
				btState = Constants.BT_IDLE;
				Toast.makeText(getApplicationContext(), "Failed to create obtain Output Stream : " 
				+ remDev.getAddress(), Toast.LENGTH_SHORT).show();
				break;
			}
			invalidateOptionsMenu();
			
		}
		
	}
	
	class ListenTask extends AsyncTask<Void, Void, Integer> {
		public ListenTask() {
			//Do any initialization here if needed
		}
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			debug("Listen : Pre-execute btState = " + Utils.getBTState(btState));
			super.onPreExecute();
		}
		
		@Override
		protected Integer doInBackground(Void... params) {
			//Start Bluetooth Server
			try {
				debug("ListenTask: doInBackground: Setting Bluetooth State to Listening");
				btState = Constants.BT_LISTENING;
				if(secureConnection) {
					debug("ListenTask: Listen using Secure Connection");
					btServerSocket = mBTAdapter.listenUsingInsecureRfcommWithServiceRecord(Constants.SPP_SERVICE_NAME, Constants.SPP_UUID);
				}
				else {
					debug("ListenTask: Listen using Insecure Connection");
					btServerSocket = mBTAdapter.listenUsingRfcommWithServiceRecord(Constants.SPP_SERVICE_NAME, Constants.SPP_UUID);
				}
			}
			catch(IOException ioe) {
				Log.e(TAG,"Failed to create Bluetooth Server Socket. Exception " + ioe.getMessage());
				return Constants.STATUS_LISTEN_FAIL;
			}
			
			if((btServerSocket != null) && (btState == Constants.BT_LISTENING)) {
				while(btState != Constants.BT_CONNECTED) {
					try {
						debug("ListenTask: Waiting for Bluetooth Connection");
						btSocket = btServerSocket.accept();
					}
					catch(IOException ioe) {
						Log.e(TAG,"Server Socket Accept failed: " + ioe.getMessage());
						btSocket = null;
						return Constants.STATUS_ACCEPT_FAIL;
					}
					
					if(btSocket != null) {
						debug("Bluetooth Socket Connected. Closing Server Socket now");
						remDev = btSocket.getRemoteDevice();
						closeServerSocket();
						
						try {
							btInStream = btSocket.getInputStream();
						}
						catch(IOException ioe) {
							Log.e(TAG,"Failed to obtain Input Stream: " + ioe.getMessage());
							try {
								btSocket.close();
							}
							catch(Exception ioe2) {
								Log.e(TAG,"ListenTask: Failed to close RFCOMM socket: " + ioe2.getMessage());
								return Constants.STATUS_OBTAIN_IS_FAIL;
							}
							return Constants.STATUS_OBTAIN_IS_FAIL;
						}
						debug("ListenTask: Obtained Input Stream");
												
						try {
							btOutStream = btSocket.getOutputStream();
						}
						catch(IOException ioe) {
							Log.e(TAG,"ListenTask: Failed to obtain Output Stream: " + ioe.getMessage());
							try {
								btSocket.close();
								btInStream.close();
							}
							catch(Exception ioe2) {
								Log.e(TAG,"ListenTask: Failed to close RFCOMM socket: " + ioe2.getMessage());
								return Constants.STATUS_OBTAIN_OS_FAIL;
							}
							return Constants.STATUS_OBTAIN_OS_FAIL;
						}
						debug("ListenTask: Obtained Output Stream");
						break; //break the while loop
					}
				}
			}
			return Constants.STATUS_SUCCESS;
		}
		
		@Override 
		protected void onCancelled() {
			debug("Listentask onCancelled: Closing Bluetooth Server Socket");
			if(closeServerSocket()) {
				btState = Constants.BT_IDLE;
				invalidateOptionsMenu();
			}
			else {
				Log.e(TAG,"Listentask onCancelled: ERROR, failed to close Bluetooth Server Socket");
			}
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			debug("ListenTask PostExecute: result = " + Utils.getResult(result.intValue()));
			switch(result.intValue()) {
			case Constants.STATUS_LISTEN_FAIL:
				if(btState == Constants.BT_LISTENING) // Dont reset btState if we came here from connecting state
					btState = Constants.BT_IDLE;
				Toast.makeText(getApplicationContext(), "Failed to obtain Bluetooth Server Socket ", 
						Toast.LENGTH_SHORT).show();
				break;
			case Constants.STATUS_ACCEPT_FAIL: // Dont reset btState if we came here from connecting state
				if(btState == Constants.BT_LISTENING)
					btState = Constants.BT_IDLE;
				Toast.makeText(getApplicationContext(), "Bluetooth Server Socket Closed ", 
						Toast.LENGTH_SHORT).show();
				break;
			case Constants.STATUS_SUCCESS:
				btState = Constants.BT_CONNECTED;
				Toast.makeText(getApplicationContext(), "Bluetooth Connection Successful", 
						Toast.LENGTH_SHORT).show();
				connectedTask = new ConnectedTask();
				connectedTask.execute();
				break;
			case Constants.STATUS_OBTAIN_IS_FAIL:
				btState = Constants.BT_IDLE;
				Toast.makeText(getApplicationContext(), "Failed to create obtain Input Stream : " 
				+ remDev.getAddress(), Toast.LENGTH_SHORT).show();
				break;
			case Constants.STATUS_OBTAIN_OS_FAIL:
				btState = Constants.BT_IDLE;
				Toast.makeText(getApplicationContext(), "Failed to create obtain Output Stream : " 
				+ remDev.getAddress(), Toast.LENGTH_SHORT).show();
				break;
			}
			invalidateOptionsMenu();
		}
		
	}
	
	class ConnectedTask extends AsyncTask<Void, String[], Integer> {
		private byte[] buffer;
		private String[] strBuffer;
		//private StringBuffer hexBuffer;
		private int bytes = 0, avail =0, total = 0;
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			debug("ConnectedTask : Pre-execute");
			super.onPreExecute();
			strBuffer = new String[2];
			
			Calendar c = Calendar.getInstance();
			
			String date = String.format(Locale.getDefault(),"%02d/%02d/%04d", 
					c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.MONTH)+1
					,c.get(Calendar.YEAR),c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),c.get(Calendar.SECOND));
			String time = String.format(Locale.getDefault(),"%02d:%02d:%02d", 
					c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),c.get(Calendar.SECOND));
			if(logAsciiData) {
				logBluetoothData("\n" + date + "," + time + ": Bluetooth Connection Created with " + remDev.getAddress() +";" + remDev.getName());
				logBluetoothData(Constants.delimiter);
			}
			
			if(showHexTab && logHexData) {
				logBluetoothHexData("\n" + date + "," + time + ": Bluetooth Connection Created with " + remDev.getAddress() +";" + remDev.getName());
				logBluetoothHexData(Constants.delimiter);
			}
		}
		
		@Override
		protected Integer doInBackground(Void...params) {
			if((btInStream == null) || (btOutStream == null) || (btSocket == null)) {
				Log.e(TAG,"Connection Stream/Socket null. Cannot start read thread");
				return Integer.valueOf(Constants.STATUS_SOCKET_NULL);
			}
			if(btState != Constants.BT_CONNECTED) {
				Log.e(TAG,"Bluetooth Not Connected. Cannot start read thread");
				return Integer.valueOf(Constants.STATUS_BT_UNCONNECTED);
			}
			while(!isCancelled()) {
				try {
					avail = btInStream.available();
					if(avail > 0) {
						buffer = new byte[avail];
						bytes = btInStream.read(buffer,0,avail);
						total += bytes;
						debug("Read " + total + " bytes from Input Stream");
						strBuffer[0] = new String(buffer,0,bytes);
						strBuffer[1] = new String(Utils.toHexStringUsingCharArray(buffer));
						publishProgress(strBuffer);
					}
				}
				catch (IOException ioe) {
					Log.e(TAG,"ConnectedThread: Connection Lost to device: " + remDev.getAddress() +
							". ERROR: " + ioe.getMessage());
					return Integer.valueOf(Constants.STATUS_CONNECTEDTASK_CANCEL);
				}
			}
			return Integer.valueOf(Constants.STATUS_SUCCESS);
		}
		
		@Override 
		protected void onCancelled() {
			debug("connected task cancelled");
			if(disconnect()) {
				btState = Constants.BT_IDLE;
				asciiDataState = Constants.DATA_STATE_IDLE;
				hexDataState = Constants.DATA_STATE_IDLE;
				Toast.makeText(getApplicationContext(),"Bluetooth device disconnected",	Toast.LENGTH_SHORT).show();
				/*if(stopBluetoothLogging() != Constants.STATUS_SUCCESS) {
					Log.e(TAG,"ConnectedTask: onCancelled, failed to stop Bluetooth Logging");
				}*/
				Calendar c = Calendar.getInstance();
				String date = String.format(Locale.getDefault(),"%02d/%02d/%04d", 
						c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.MONTH)+1
						,c.get(Calendar.YEAR),c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),c.get(Calendar.SECOND));
				String time = String.format(Locale.getDefault(),"%02d:%02d:%02d", 
						c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),c.get(Calendar.SECOND));
				if(logAsciiData) {
					logBluetoothData("\n" + date + "," + time + ": Bluetooth Connection Disconnected ");
					logBluetoothData(Constants.delimiter);
				}
				if(showHexTab && logHexData) {
					logBluetoothHexData("\n" + date + "," + time + ": Bluetooth Connection Disconnected ");
					logBluetoothHexData(Constants.delimiter);
				}
				invalidateOptionsMenu();
			}
		}
		
		@Override
		protected void onProgressUpdate(String[]... data) {
			int status;
			AsciiSectionFragment ascii =(AsciiSectionFragment) getSupportFragmentManager().
					findFragmentByTag(mSectionsPagerAdapter.makeFragmentName(0));
			if(ascii != null) {
				if(logAsciiData) {
					if((asciiDataState == Constants.DATA_STATE_TX) || (asciiDataState == Constants.DATA_STATE_IDLE)){
						if((status = logBluetoothData(Constants.LF + "<<" + data[0][0])) != Constants.STATUS_SUCCESS) {
							Log.e(TAG,"Failed to Log Bluetooth Data. Status = " + Utils.getResult(status));
						}
					}
					else {
						if((status = logBluetoothData(data[0][0])) != Constants.STATUS_SUCCESS) {
							Log.e(TAG,"Failed to Log Bluetooth Data. Status = " + Utils.getResult(status));
						}
					}
				}
				
				ascii.setRxAsciiText(data[0][0]);
			}
			else {
				Log.e(TAG,"ASCII fragment = null");
			}
			if(showHexTab) {
				HexSectionFragment hex =(HexSectionFragment) getSupportFragmentManager().
						findFragmentByTag(mSectionsPagerAdapter.makeFragmentName(1));
				if(hex != null) {
					if(logHexData) {
						if((hexDataState == Constants.DATA_STATE_TX) || (hexDataState == Constants.DATA_STATE_IDLE)) {
							if((status = logBluetoothHexData(Constants.LF + "<<" + data[0][1])) != Constants.STATUS_SUCCESS) {
								Log.e(TAG,"Failed to log Bluetooth Hex Data. Status = " + Utils.getResult(status));
							}
						}
						else {
							if((status = logBluetoothHexData(data[0][1])) != Constants.STATUS_SUCCESS) {
								Log.e(TAG,"Failed to log Bluetooth Hex Data. Status = " + Utils.getResult(status));
							}
						}
					}
					hex.setRxHexText(data[0][1]);
				}
				else {
					Log.e(TAG,"HEX fragment = null");
				}
			}
		
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			debug("ConnectedTask onPostExecute: result: " + Utils.getResult(result.intValue()));
			switch(result.intValue()) {
			case Constants.STATUS_SUCCESS:
				debug("Connected Task finished successfully");
				break;
			case Constants.STATUS_BT_UNCONNECTED:
				btState = Constants.BT_IDLE;
				Toast.makeText(getApplicationContext(), "Bluetooth Connection not present ", Toast.LENGTH_SHORT).show();
				break;
			case Constants.STATUS_SOCKET_NULL:
				btState = Constants.BT_IDLE;
				Toast.makeText(getApplicationContext(), "Null Bluetooth Socket ", Toast.LENGTH_SHORT).show();
				break;
			case Constants.STATUS_CONNECTEDTASK_CANCEL:
				btState = Constants.BT_IDLE;
				Toast.makeText(getApplicationContext(), "Bluetooth Stream Closed ", Toast.LENGTH_SHORT).show();
				break;
			}
			Calendar c = Calendar.getInstance();
			String date = String.format(Locale.getDefault(),"%02d/%02d/%04d", 
					c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.MONTH)+1
					,c.get(Calendar.YEAR),c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),c.get(Calendar.SECOND));
			String time = String.format(Locale.getDefault(),"%02d:%02d:%02d", 
					c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),c.get(Calendar.SECOND));
			if(logAsciiData) {
				logBluetoothData("\n" + date + "," + time + ": Bluetooth Connection Disconnected ");
				logBluetoothData(Constants.delimiter);
			}
			if(showHexTab && logHexData) {
				logBluetoothHexData("\n" + date + "," + time + ": Bluetooth Connection Disconnected ");
				logBluetoothHexData(Constants.delimiter);
			}
			invalidateOptionsMenu();
		}
		
		public void sendData(byte[] data) {
			int status;
			if(btOutStream != null) {
				try {
					btOutStream.write(data);
					String msg = new String(data,0, data.length);
					AsciiSectionFragment ascii =(AsciiSectionFragment) getSupportFragmentManager().
							findFragmentByTag(mSectionsPagerAdapter.makeFragmentName(0));
					if(ascii != null) {
						if(logAsciiData) {
							if((asciiDataState == Constants.DATA_STATE_RX) || (asciiDataState == Constants.DATA_STATE_IDLE)) {
								if((status = logBluetoothData(Constants.LF + ">>" + msg)) != Constants.STATUS_SUCCESS) {
									Log.e(TAG,"Failed to Log Bluetooth Data. Status = " + Utils.getResult(status));
								}
							}
							else {
								if((status = logBluetoothData(msg)) != Constants.STATUS_SUCCESS) {
									Log.e(TAG,"Failed to Log Bluetooth Data. Status = " + Utils.getResult(status));
								}
							}
						}
						ascii.setTxAsciiText(msg);
					}
					else {
						Log.e(TAG,"ASCII fragment = null");
					}
					
					if(showHexTab) {
						HexSectionFragment hex =(HexSectionFragment) getSupportFragmentManager().
								findFragmentByTag(mSectionsPagerAdapter.makeFragmentName(1));
						if(hex != null) {
							if(logHexData) {
								if( (hexDataState == Constants.DATA_STATE_RX) || (hexDataState == Constants.DATA_STATE_IDLE)) {
									if((status = logBluetoothHexData(Constants.LF + ">>" + Utils.toHexStringUsingCharArray(data))) 
											!= Constants.STATUS_SUCCESS) {
										Log.e(TAG,"Failed to log Bluetooth Hex Data. Status = " + Utils.getResult(status));
									}
								}
								else {
									if((status = logBluetoothHexData(Utils.toHexStringUsingCharArray(data))) != Constants.STATUS_SUCCESS) {
										Log.e(TAG,"Failed to log Bluetooth Hex Data. Status = " + Utils.getResult(status));
									}
								}
							}
							hex.setTxHexText(data,data.length);
						}
						else {
							Log.e(TAG,"ASCII fragment = null");
						}
					}
					//processWriteBytes(buffer, buffer.length);

				} catch (IOException ioe) {
					Log.e(TAG, "Exception during write", ioe);
				}
			}
		}
		
	}
	
	public void onClickSendButton(View v) {
		if(getBTState() != Constants.BT_CONNECTED) {
			Log.e(TAG,"Bluetooth not connected. No data sent");
			return;
		}
		EditText txtView = (EditText)findViewById(R.id.edit_text);
		String data = txtView.getText().toString();
		//debug("Default Charset = " + Charset.defaultCharset());
		if((data.length() > 0) && (connectedTask != null) && (connectedTask.getStatus() == AsyncTask.Status.RUNNING)) {
			connectedTask.sendData(data.getBytes());
			txtView.setText("");
		}
		else if(data.length() == 0) {
			Log.e(TAG,"SendData: Data length = 0");
		}
		else if(connectedTask == null) {
			Log.e(TAG,"SendData: connectedTask = null");
		}
		else {
			Log.e(TAG,"SendData: connectedTask not running");
		}
		
	}
	
	public void onClickClearButton(View v) {
		AsciiSectionFragment ascii =(AsciiSectionFragment) getSupportFragmentManager().
				findFragmentByTag(mSectionsPagerAdapter.makeFragmentName(0));
		if(ascii != null) {
			ascii.clearAsciiText();
		}
		else {
			Log.e(TAG,"ASCII fragment = null");
		}
		
		HexSectionFragment hex =(HexSectionFragment) getSupportFragmentManager().
				findFragmentByTag(mSectionsPagerAdapter.makeFragmentName(1));
		if(hex != null) {
			hex.clearHexText();
		}
		else {
			Log.e(TAG,"HEX fragment = null");
		}
	}
	
	public void debug(String msg) {
		if (DEBUG)
			Log.d(TAG, msg);
	}

}
