package com.sumeet.apps;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

public class AsciiFragment extends Fragment {
	/**
	 * The fragment argument representing the section number for this
	 * fragment.
	 */
	public static final String ARG_SECTION_NUMBER = "ascii_section_number";
	private TextView asciiTV;
	private ScrollView asciiSV;
	public static final boolean DEBUG = true;
	public static final String TAG = "AsciiFragment";
	

	public AsciiFragment() {
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
		return rootView;
	}
	
	
	public void setTxText(String data) {
		if(DEBUG)
			Log.d(TAG,"+setTxText: data = " + data);
		//TextView tv = (TextView)asciiRootView.findViewById(R.id.ascii_content);
		//ScrollView sv = (ScrollView)asciiRootView.findViewById(R.id.ascii_content_scroll);
		if(asciiTV == null) {
			Log.e(TAG, "asciiTV = null");
		}
		if(asciiSV == null) {
			Log.e(TAG,"asciiSV = null");
		}
		asciiTV.append(data.toString());
		asciiSV.fullScroll(View.FOCUS_DOWN);
		if(DEBUG)
			Log.d(TAG,"AsciiSectionFragment: appended text "+ data);

		
	}
}
