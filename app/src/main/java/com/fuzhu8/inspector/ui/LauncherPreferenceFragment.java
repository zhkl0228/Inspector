package com.fuzhu8.inspector.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;

import com.fuzhu8.inspector.DexposedLoader;
import com.fuzhu8.inspector.DigestUtils;
import com.fuzhu8.inspector.R;
import com.fuzhu8.inspector.content.InspectorBroadcastListener;
import com.fuzhu8.inspector.root.RootUtil;
import com.fuzhu8.inspector.root.SuperUserRootUtil;
import com.fuzhu8.inspector.vpn.InspectVpnService;
import com.jaredrummler.android.processes.AndroidProcesses;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.banny.utils.StringUtils;
import eu.faircode.netguard.ServiceSinkhole;

/**
 * This fragment shows general preferences only. It is used when the
 * activity is showing a two-pane settings UI.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class LauncherPreferenceFragment extends PreferenceFragment {
	
	private ListPreference launcherApps;
	private CheckBoxPreference vpnDebug;
	
	private final Map<String, MyPackageInfo> packageMap = new HashMap<>();

	// private EditTextPreference vpnHost, vpnPort, vpnPassword;
	private CheckBoxPreference enabledSocks;
	private EditTextPreference socksHost;
	private EditTextPreference socksPort;

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
			Intent intent = new Intent(getActivity(), ServiceSinkhole.class);
			Bundle bundle = new Bundle();
			bundle.putString(InspectVpnService.PACKAGE_NAME_KEY, getActivity().getPackageName());
			bundle.putInt(InspectVpnService.UID_KEY, -1);
			bundle.putBoolean(InspectVpnService.DEBUG_KEY, vpnDebug != null && vpnDebug.isChecked());
			/*bundle.putString(ToyVpnService.SERVER_ADDRESS_KEY, vpnHost.getText());
			bundle.putString(ToyVpnService.SERVER_PORT_KEY, vpnPort.getText());
			bundle.putString(ToyVpnService.SERVER_PASSWORD_KEY, vpnPassword.getText());*/
			if (socksHost != null && socksPort != null && enabledSocks != null && enabledSocks.isChecked()) {
				bundle.putString(InspectVpnService.SOCKS_HOST_KEY, socksHost.getText());
				int port;
				try {
					port = Integer.parseInt(socksPort.getText());
				} catch(NumberFormatException e) {
					port = 0;
				}
				bundle.putInt(InspectVpnService.SOCKS_PORT_KEY, port);
			}
			intent.putExtra(Bundle.class.getCanonicalName(), bundle);
			getActivity().startService(intent);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
		
		addPreferencesFromResource(R.xml.launcher);
		
		launcherApps = (ListPreference) findPreference("pref_launcher_app");
		vpnDebug = (CheckBoxPreference) findPreference("pref_vpn_debug");
		
		final PackageManager pm = syncApps();
		
		OnPreferenceChangeListener launcherAppsChangeListener = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String old = launcherApps.getValue();
				String packageName = String.valueOf(newValue);
				Log.d("Inspector", "onPreferenceChange old=" + old + ", packageName=" + packageName);
				
				MyPackageInfo pi = packageMap.get(packageName);
				if(StringUtils.isEmpty(packageName) || "pleaseSelect".equals(packageName)) {
					launcherApps.setSummary(getText(R.string.pref_launcher_app_sum));
				} else if(pi != null) {
					if(startInspect(pm, packageName, pi.getUid(), !pi.isService())) {
						launcherApps.setSummary(pi.getLabel());
						return true;
					} else {
						launcherApps.setSummary("注入" + pi.getLabel() + "失败");
						return false;
					}
				}
				return true;
			}
		};
		launcherApps.setOnPreferenceChangeListener(launcherAppsChangeListener);

		/*vpnHost = (EditTextPreference) findPreference("pref_vpn_host");
		vpnPort = (EditTextPreference) findPreference("pref_vpn_port");
		vpnPassword = (EditTextPreference) findPreference("pref_vpn_password");*/

		enabledSocks = (CheckBoxPreference) findPreference("pref_socks");
		socksHost = (EditTextPreference) findPreference("pref_socks_host");
		socksPort = (EditTextPreference) findPreference("pref_socks_port");
		SwitchPreference vpn = (SwitchPreference) findPreference("switch_vpn");

		vpn.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean b = (Boolean) newValue;
				if (!b) {
					Intent intent = new Intent(InspectorBroadcastListener.REQUEST_STOP_VPN);
					getActivity().sendBroadcast(intent);
					return true;
				}

				Intent vpnIntent = VpnService.prepare(getActivity());
				if (vpnIntent != null) {
					startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
				} else {
					onActivityResult(VPN_REQUEST_CODE, Activity.RESULT_OK, null);
				}
				return true;
			}
		});
	}

	public static final int VPN_REQUEST_CODE = 0x7b;

	@Override
	public void onStart() {
		super.onStart();
		
		syncApps();
	}

	private PackageManager syncApps() {
		packageMap.clear();

		final PackageManager pm = getActivity().getPackageManager();
		final ActivityManager am = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
		List<String> packageNames = new ArrayList<>();
		List<CharSequence> appNames = new ArrayList<>();
		packageNames.add("pleaseSelect");
		appNames.add("选择程序");
		Set<Integer> processes = new HashSet<>();
		List<RunningAppProcessInfo> runningAppProcessInfos = AndroidProcesses.getRunningAppProcessInfo(getActivity());
		if(runningAppProcessInfos != null) {
			for(RunningAppProcessInfo app : runningAppProcessInfos) {
				try {
					PackageInfo pi = pm.getPackageInfo(app.processName, PackageManager.GET_META_DATA | PackageManager.GET_PERMISSIONS);
					if(!pi.applicationInfo.enabled ||
							// (pi.applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0 ||
							DexposedLoader.class.getPackage().getName().equals(app.processName) ||
							app.pid < 1) {
						continue;
					}
					boolean network = false;
					if(pi.requestedPermissions != null) {
						for(String perm : pi.requestedPermissions) {
							if("android.permission.INTERNET".equals(perm)) {
								network = true;
								break;
							}
						}
					}
					
					CharSequence label = pm.getApplicationLabel(pi.applicationInfo);
					packageNames.add(pi.packageName);
					appNames.add((network ? "" : "@") + label + "[" + app.pid + ']');
					packageMap.put(pi.packageName, new MyPackageInfo(label, app.pid, false));
					processes.add(app.pid);
				} catch (NameNotFoundException e) {
					// ignore
				}
			}
		}
		List<RunningServiceInfo> runningServiceInfos = am == null ? Collections.<RunningServiceInfo>emptyList() : am.getRunningServices(Short.MAX_VALUE);
		for(RunningServiceInfo service : runningServiceInfos) {
			String packageName = service.service.getPackageName();
			try {
				PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA | PackageManager.GET_PERMISSIONS);
				if(!pi.applicationInfo.enabled ||
						// (pi.applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0 ||
						DexposedLoader.class.getPackage().getName().equals(packageName) ||
						service.pid < 1 ||
						processes.contains(service.pid)) {
					continue;
				}
				boolean network = false;
				if(pi.requestedPermissions != null) {
					for(String perm : pi.requestedPermissions) {
						if("android.permission.INTERNET".equals(perm)) {
							network = true;
							break;
						}
					}
				}

				String process = service.process;
				Log.d("Inspector", "process=" + service.process + ", pid=" + service.pid +
						", foreground=" + service.foreground +
						", className=" + service.service.getClassName() +
						", uid=" + service.uid +
						", network=" + network);
				
				CharSequence label = pm.getApplicationLabel(pi.applicationInfo);
				StringBuilder name = new StringBuilder();
				name.append('*').append(pi.packageName);
				packageNames.add(name.toString());
				
				int index;
				if(StringUtils.isEmpty(process)) {
					process = service.service.getClassName().indexOf('.') == -1 ? service.service.getClassName() : FilenameUtils.getExtension(service.service.getClassName());
				} else if((index = process.indexOf(':')) != -1) {
					process = process.substring(index + 1);
				}
				
				appNames.add((network ? "" : "@") + label + "[" + process + "][" + service.pid + ']');
				packageMap.put(name.toString(), new MyPackageInfo(label, service.pid, true));
				processes.add(service.pid);
			} catch (NameNotFoundException e) {
				// ignore
			}
		}
		
		launcherApps.setEntries(appNames.toArray(new CharSequence[0]));
		launcherApps.setEntryValues(packageNames.toArray(new String[0]));
		launcherApps.setValueIndex(0);
		launcherApps.setSummary(getText(R.string.pref_launcher_app_sum));
		return pm;
	}
	
	private static File moveExecutable(Context context, String executable) {
		File filesDir = context.getFilesDir();
		filesDir.mkdirs();

		File target = new File(filesDir, executable);
		InputStream inputStream = null;
		OutputStream outputStream = null;
		File tmp = null;
		try {
			try {
				inputStream = context.getAssets().open(Build.CPU_ABI + "/" + executable);
			} catch(IOException e) {
				String cpuAbi2 = Build.CPU_ABI2;
				Log.d("Inspector", "cpu_api=" + Build.CPU_ABI + ", cpu_abi2=" + cpuAbi2);
				if(StringUtils.isEmpty(cpuAbi2)) {
					cpuAbi2 = "armeabi";
				}
				
				inputStream = context.getAssets().open(cpuAbi2 + "/" + executable);
			}
			tmp = File.createTempFile(executable, "", filesDir);
			outputStream = new FileOutputStream(tmp);
			org.apache.commons.io.IOUtils.copy(inputStream, outputStream);
			
			if(!target.canExecute() || !DigestUtils.md5Hex(FileUtils.readFileToByteArray(target)).equals(DigestUtils.md5Hex(FileUtils.readFileToByteArray(tmp)))) {
				target.delete();
				FileUtils.moveFile(tmp, target);
				target.setExecutable(true);
			}
			return target;
		} catch(IOException e) {
			Log.d("Inspector", e.getMessage(), e);
			return null;
		} finally {
			FileUtils.deleteQuietly(tmp);
			IOUtils.closeQuietly(outputStream);
			IOUtils.closeQuietly(inputStream);
		}
	}

	private static File moveAssets(Context context, String executable) {
		File filesDir = context.getFilesDir();
		filesDir.mkdirs();

		File target = new File(filesDir, executable);
		InputStream inputStream = null;
		OutputStream outputStream = null;
		File tmp = null;
		try {
			inputStream = context.getAssets().open(executable);
			tmp = File.createTempFile(executable, "", filesDir);
			outputStream = new FileOutputStream(tmp);
			org.apache.commons.io.IOUtils.copy(inputStream, outputStream);

			if(!target.canExecute() || !DigestUtils.md5Hex(FileUtils.readFileToByteArray(target)).equals(DigestUtils.md5Hex(FileUtils.readFileToByteArray(tmp)))) {
				target.delete();
				FileUtils.moveFile(tmp, target);
				target.setExecutable(true);
			}
			return target;
		} catch(IOException e) {
			Log.d("Inspector", e.getMessage(), e);
			return null;
		} finally {
			FileUtils.deleteQuietly(tmp);
			IOUtils.closeQuietly(outputStream);
			IOUtils.closeQuietly(inputStream);
		}
	}

	protected boolean startInspect(PackageManager pm, String packageName, int pid, boolean launchApp) {
		File hijack;
		if((hijack = moveExecutable(getActivity(), "hijack")) == null) {
			return false;
		}

		moveAssets(getActivity(), "hijack.sh");

		if(!rootUtil.startShell()) {
			return false;
		}
		
		if(Build.VERSION.SDK_INT > 19) { // KITKAT
			rootUtil.execute("setenforce 0", null);
		}
		String commandLine = hijack.getAbsolutePath() + " -p " + pid + " -l " + new File(Environment.getDataDirectory(), "data/" + DexposedLoader.class.getPackage().getName() + "/lib/liblauncher.so").getAbsolutePath();
		Log.d("Inspector", "inspect commandLine: " + commandLine);
		if(rootUtil.execute(commandLine, null) == 0) {
			Intent launchIntent;
			if(launchApp && (launchIntent = pm.getLaunchIntentForPackage(packageName)) != null) {
				getActivity().startActivity(launchIntent);
			}
			return true;
		}
		return false;
	}

	private final RootUtil rootUtil = new SuperUserRootUtil();
}