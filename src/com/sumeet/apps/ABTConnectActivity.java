 package com.sumeet.apps;

import java.util.Set;
import java.util.Vector;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class ABTConnectActivity extends Activity {

	private static final boolean DEBUG = true;
	private static final String TAG = "ABTConnectActivity";
	private BluetoothAdapter mBTAdapter = null;
	
	private Vector<BluetoothDevice> btDevices;
	private Vector<String>btAddresses;
	private ArrayAdapter<String> mBTDevicesArrayAdapter;
	
	private static final String BULLET = "  \u2022";
	private static final String CHECK = "  \u2713";
	
	private Button searchButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		debug("onCreate");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.device_list);
		mBTAdapter = BluetoothAdapter.getDefaultAdapter();
		btDevices = new Vector<BluetoothDevice>();
		btAddresses = new Vector<String>();
		mBTDevicesArrayAdapter = new ArrayAdapter<String>(this,R.layout.list_entry);
		
		searchButton = (Button)findViewById(R.id.button_scan);
		ListView mBTDevicesListView = (ListView)findViewById(R.id.listdevices);
		mBTDevicesListView.setAdapter(mBTDevicesArrayAdapter);
		mBTDevicesListView.setOnItemClickListener(onSelectBluetoothDevice);
		
		debug("onCreate: Registering broadcast receiver for Bluetooth inquiry results and inquiry finished");
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(bluetoothInqReceiver, filter);
		
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(bluetoothInqReceiver, filter);
		
		Set<BluetoothDevice> pairedDevices = mBTAdapter.getBondedDevices();
		if(pairedDevices.size() > 0 ){
			debug("onCreate: Found " + pairedDevices.size() + " paired devices");
			for(BluetoothDevice device: pairedDevices) {
				
				btDevices.add(device);
				btAddresses.add(device.getAddress());
				mBTDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress() + CHECK);
			}
		}
		else {
			debug("onCreate: No paired devices found");
		}
	}

	private OnItemClickListener onSelectBluetoothDevice = new OnItemClickListener(){
		public void onItemClick(AdapterView<?> av,View v, int position, long id ) {
			debug("DeviceList: onItemClick, id " + id + " selected. Cancelling discovery");
			mBTAdapter.cancelDiscovery();
			
			String text = ((TextView) v).getText().toString();
			debug("DeviceList: onItemClick text = " + text);
			String addr = text.substring(text.length()-20, text.length()-3);
			debug("DeviceList: onItemClick Selected address = " + addr);
			Intent intent = new Intent();
			intent.putExtra(Constants.EXTRA_DEVICE_ADDRESS, addr);
			
			setResult(Activity.RESULT_OK, intent);
			finish();
			
		}
	};
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if(mBTAdapter != null) {
			debug("onDestroy: Cancelling discovery");
			mBTAdapter.cancelDiscovery();
		}
		debug("onDestroy: Unregistering broadcast receiver");
		btDevices.clear();
		btAddresses.clear();
		this.unregisterReceiver(bluetoothInqReceiver);
	}
	
	public void onClickSearchButton(View v) {
		debug("Search button pressed");
		BluetoothDevice d;
		String s;
		if(ABTActivity.getBTState() == Constants.BT_CONNECTED) {
			Toast.makeText(getApplicationContext(), "Cannot Start Scan in Connected State ",
					Toast.LENGTH_LONG).show();
			return;
		}
		if(searchButton.getText() == getResources().getText(R.string.search_devices).toString()) {
			setProgressBarIndeterminateVisibility(true);
			debug("onClickScanButton: btDevices.getCount() = " +   btDevices.size());
			mBTDevicesArrayAdapter.remove(getResources().getText(R.string.no_devices_found).toString());
			for(int i=0; i<btDevices.size();i++) {
				d = (BluetoothDevice) btDevices.elementAt(i);
				if(d.getBondState() != BluetoothDevice.BOND_BONDED) {
					s = d.getName() + "\n" + d.getAddress() + BULLET;
					debug("onClickScanButton: removing " + s + " from mBTDevicesArrayAdapter");
					mBTDevicesArrayAdapter.remove(s);
					btDevices.remove(d);
					btAddresses.remove(d.getAddress());
				}
			}
		
			if(mBTAdapter.isDiscovering()) {
				debug("onClickScanButton: Already discovering, cancel discovery first");
				mBTAdapter.cancelDiscovery();
			}
			debug("onClickScanButton: Starting Bluetooth discovery");
			mBTAdapter.startDiscovery();
			searchButton.setText(R.string.cancel_search);
		}
		else {
			if(mBTAdapter.isDiscovering()) {
				debug("onClickScanButton: Already discovering, cancel discovery first");
				mBTAdapter.cancelDiscovery();
			}
			searchButton.setText(R.string.search_devices);
		}
	}
	
	private final BroadcastReceiver bluetoothInqReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if(BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				debug("mBroadcastReceiver: Bluetooth Device found " + intent.getParcelableExtra(BluetoothDevice.EXTRA_NAME)
						+ ":" + device.getAddress());
				//if((device.getBondState() != BluetoothDevice.BOND_BONDED) && (btDevices.indexOf(device) == -1)) {
				if((device.getBondState() != BluetoothDevice.BOND_BONDED) && (btAddresses.indexOf(device.getAddress()) == -1)) {
					btDevices.add(device);
					btAddresses.add(device.getAddress());
					//mBTDevicesArrayAdapter.setBold(false);
					mBTDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress() + BULLET);
				}
			}
			else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				debug("mBroadcastReceiver: Discovery finished");
				setProgressBarIndeterminateVisibility(false);
				searchButton.setText(R.string.search_devices);
				if(mBTDevicesArrayAdapter.getCount() == 0) {
					debug("mBroadcastReceiver: No Devices Found");
					String none = getText(R.string.no_devices_found).toString();
					//mBTDevicesArrayAdapter.setBold(false);
					mBTDevicesArrayAdapter.add(none);
				}
			}
		}
	};
	
	private void debug(String msg) {
		if(DEBUG)
			 Log.d(TAG,msg);
	}
	

}
