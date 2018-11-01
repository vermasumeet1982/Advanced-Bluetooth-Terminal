package com.sumeet.apps;

import java.util.UUID;

public class Constants {
	//Shared Preferences Constants
	public static final String PREFS = "com.sumeet.apps.abt.sharedprefs";
	public static final String PREF_BT_ENABLED = "com.sumeet.apps.abt.btenabled";
	
	//request codes
	public static final int REQUEST_ENABLE_BT = 1;
	public static final int REQUEST_SETTINGS = 2;
	public static final int REQUEST_SEARCH = 3;
	public static final int REQUEST_DISCOVERABLE = 4;
	
	public static final int TOTAL_TABS = 2;
	
	//Bluetooth Constants
	public static final int BT_IDLE = 1;
	public static final int BT_LISTENING = 2;
	public static final int BT_CONNECTING = 3;
	public static final int BT_CONNECTED = 4;
	
	//Data State
	public static final int DATA_STATE_IDLE = 0;
	public static final int DATA_STATE_TX = 1;
	public static final int DATA_STATE_RX = 2;
	
	//SPP related constants
	public static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	public static final String SPP_SERVICE_NAME = "ABT_SPP_Service_1";
	public static final int DISCOVERABLE_DUR_INDEFINITE = 0;
	public static final int DISCOVERABLE_DUR_1S = 1;
	public static final int MIN_DISCOVERABLE_DUR = 10;
	public static final int MAX_DISCOVERABLE_DUR = 300;
	
	//Extras
	public static final String EXTRA_DEVICE_ADDRESS = "com.sumeet.apps.abt.device_address";
	
	//Return Codes
	public static final int STATUS_SUCCESS = 1;
	public static final int STATUS_INVALID_BDADDR = 2;
	public static final int STATUS_REMADDR_NULL = 3;
	public static final int STATUS_CONNECT_FAIL = 4;
	public static final int STATUS_OBTAIN_IS_FAIL = 5;
	public static final int STATUS_OBTAIN_OS_FAIL = 6;
	public static final int STATUS_SOCKET_NULL = 7;
	public static final int STATUS_BT_UNCONNECTED = 8;
	public static final int STATUS_CONNECTEDTASK_CANCEL = 9;
	public static final int STATUS_LISTEN_FAIL = 10;
	public static final int STATUS_ACCEPT_FAIL = 11;
	public static final int STATUS_WAITING_DISCOVERABILITY = 12;
	public static final int STATUS_SERVER_ALREADY_RUNNING = 13;
	public static final int STATUS_FS_NOT_MOUNTED = 14;
	public static final int STATUS_LOGGING_NOT_ENABLED = 15;
	public static final int STATUS_LOG_FILE_FAILED = 16;
	public static final int STATUS_STOP_LOGGING_FAILED = 17;
	
	//Misc
	public static final String LF="\n";
	public static final String ASCII_LOG_FILE_NAME="ABT_";
	public static final String HEX_LOG_FILE_NAME="ABT_HEX_";
	public static final String STORAGE_PATH = "/storage/emulated/0/ABT/";
	public static final String delimiter = "============\n";
	
}
