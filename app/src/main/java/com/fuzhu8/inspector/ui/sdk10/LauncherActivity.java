/**
 * 
 */
package com.fuzhu8.inspector.ui.sdk10;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

/**
 * @author zhkl0228
 *
 */
public class LauncherActivity extends Activity {
	
	protected SharedPreferences preference;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		preference = getSharedPreferences("launcher", MODE_PRIVATE);
	}

}
