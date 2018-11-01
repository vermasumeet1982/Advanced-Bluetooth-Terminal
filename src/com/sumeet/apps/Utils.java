package com.sumeet.apps;

public class Utils {
	
	public static final String toHexStringUsingCharArray(byte input[]) {
        int i = 0;
        if (input == null || input.length <= 0)
            return null;

        char lookupArray[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        char[] result = new char[input.length * 3];

        while (i < input.length) {
            result[3*i] = lookupArray[(input[i]>>4) & 0x0F];
            result[3*i+1] = lookupArray[(input[i] & 0x0F)];
            result[3*i+2] = ' ';
            i++;
        }
        return String.valueOf(result);
    }
	
	public static String getResult(int resultCode) {
		switch (resultCode) {
		case Constants.STATUS_SUCCESS:
			return "STATUS_SUCCESS";
		case Constants.STATUS_INVALID_BDADDR:
			return "STATUS_INVALID_BLUETOOTH_ADDRESS";
		case Constants.STATUS_REMADDR_NULL:
			return "STATUS_NULL_REMOTE_ADDRESS";
		case Constants.STATUS_CONNECT_FAIL:
			return "STATUS_CONNECT_FAIL";
		case Constants.STATUS_OBTAIN_IS_FAIL:
			return "STATUS_OBTAIN_INPUT_STREAM_FAILED";
		case Constants.STATUS_OBTAIN_OS_FAIL:
			return "STATUS_OBTAIN_OUTPUT_STREAM_FAILED";
		case Constants.STATUS_SOCKET_NULL:
			return "STATUS_NULL_BLUETOOTH_SOCKET";
		case Constants.STATUS_BT_UNCONNECTED:
			return "STATUS_BLUETOOTH_UNCONNECTED";
		case Constants.STATUS_CONNECTEDTASK_CANCEL:
			return "STATUS_CONNECTED_TASK_CANCELLED";
		case Constants.STATUS_LISTEN_FAIL:
			return "STATUS_LISTEN_FAILED";
		case Constants.STATUS_ACCEPT_FAIL:
			return "STATUS_SERVER_SOCKET_ACCEPT_FAIL";
		case Constants.STATUS_WAITING_DISCOVERABILITY:
			return "STATUS_WAITING_DISCOVERABILITY";
		case Constants.STATUS_SERVER_ALREADY_RUNNING:
			return "STATUS_SERVER_ALREADY_RUNNING";
		case Constants.STATUS_FS_NOT_MOUNTED:
			return "STATUS_FS_NOT_MOUNTED";
		case Constants.STATUS_LOGGING_NOT_ENABLED:
			return "STATUS_LOGGING_NOT_ENABLED";
		case Constants.STATUS_LOG_FILE_FAILED:
			return "STATUS_LOG_FILE_FAILED";
		case Constants.STATUS_STOP_LOGGING_FAILED:
			return "STATUS_STOP_LOGGING_FAILED";
		default:
			return "STATUS_UNKNOWN";
				
		}
	}
	
	public static String getBTState(int state) {
		switch(state) {
		case Constants.BT_CONNECTED:
			return "BT_CONNECTED";
		case Constants.BT_CONNECTING:
			return "BT_CONNECTING";
		case Constants.BT_LISTENING:
			return "BT_LISTENING";
		case Constants.BT_IDLE:
			return "BT_IDLE";
		default:
			return "BT_STATE_UNKNOWN";
		}
	}

}
