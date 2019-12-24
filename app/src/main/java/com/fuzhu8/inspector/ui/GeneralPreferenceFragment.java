package com.fuzhu8.inspector.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.fuzhu8.inspector.R;
import com.fuzhu8.inspector.jni.Feature;

/**
 * This fragment shows general preferences only. It is used when the
 * activity is showing a two-pane settings UI.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class GeneralPreferenceFragment extends PreferenceFragment {
	
	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getPreferenceManager().setSharedPreferencesMode(Build.VERSION.SDK_INT >= 25 ? Context.MODE_PRIVATE : Context.MODE_WORLD_READABLE);
		
		addPreferencesFromResource(R.xml.preferences);
		
		checkSupported();
	}

	private void checkSupported() {
		if(!Feature.supportDvm()) {
			disablePref("pref_trace_anti");
			disablePref("pref_collect_bytecode_text");
		}
	}

	private void disablePref(String key) {
		Preference preference = findPreference(key);
		if(preference != null) {
			preference.setEnabled(false);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		checkSupported();
	}
}