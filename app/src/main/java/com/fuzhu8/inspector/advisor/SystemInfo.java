package com.fuzhu8.inspector.advisor;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.webkit.WebSettings;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class SystemInfo {

    private static List<PackageInfo> packagesCache;
    private static PackageInfo packageInfoCache;

    @SuppressLint({"MissingPermission", "HardwareIds", "PrivateApi"})
    static JSONObject create(Context context, TelephonyManager tm, String imei, String imsi) {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifi == null ? null : wifi.getConnectionInfo();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);// context.getWindowManager();

        LocationManager lm = null;
        try {
            lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        } catch(Throwable ignored) {}

        List<PackageInfo> packages;
        try {
            packages = packagesCache == null ? context.getPackageManager().getInstalledPackages(0) : packagesCache;
            packagesCache = packages;
        } catch(Throwable t) {
            packages = Collections.emptyList();
        }
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager == null ? null : connectivityManager.getActiveNetworkInfo();
        NetworkInfo mobileNetworkInfo = connectivityManager == null ? null : connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        JSONObject systemInfo = new JSONObject();
        putQuietly(systemInfo, "imei", imei);
        putQuietly(systemInfo, "imsi", imsi);

        putQuietly(systemInfo, "brand", Build.BRAND);
        putQuietly(systemInfo, "manufacturer", Build.MANUFACTURER);
        putQuietly(systemInfo, "model", Build.MODEL);
        putQuietly(systemInfo, "release", Build.VERSION.RELEASE);
        putQuietly(systemInfo, "sdk", Build.VERSION.SDK_INT);

        putQuietly(systemInfo, "fingerprint", Build.FINGERPRINT);
        putQuietly(systemInfo, "board", Build.BOARD);

        try {
            putQuietly(systemInfo, "serial", getSerial());
        } catch(Throwable ignored) {}
        putQuietly(systemInfo, "display", Build.DISPLAY);
        putQuietly(systemInfo, "id", Build.ID);
        try {
            putQuietly(systemInfo, "androidId", Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
        } catch(Throwable ignored) {}

        try {
            putQuietly(systemInfo, "radio", getRadioVersion());
        } catch(Throwable ignored) {}
        putQuietly(systemInfo, "host", Build.HOST);

        putQuietly(systemInfo, "bootloader", Build.BOOTLOADER);
        putQuietly(systemInfo, "cpuAbi", Build.CPU_ABI);
        putQuietly(systemInfo, "cpuAbi2", Build.CPU_ABI2);
        putQuietly(systemInfo, "device", Build.DEVICE);
        putQuietly(systemInfo, "hardware", Build.HARDWARE);
        putQuietly(systemInfo, "product", Build.PRODUCT);
        putQuietly(systemInfo, "tags", Build.TAGS);
        putQuietly(systemInfo, "time", Build.TIME);
        putQuietly(systemInfo, "type", Build.TYPE);
        putQuietly(systemInfo, "user", Build.USER);
        putQuietly(systemInfo, "codename", Build.VERSION.CODENAME);
        putQuietly(systemInfo, "incremental", Build.VERSION.INCREMENTAL);
        putQuietly(systemInfo, "unknown", Build.UNKNOWN);

        InputStream input = null;
        try {
            input = new FileInputStream(new File("/proc/meminfo"));
            putQuietly(systemInfo, "meminfo", IOUtils.toString(input, "UTF-8"));
        } catch (Throwable ignored) {
        } finally {
            IOUtils.closeQuietly(input);
        }
        input = null;
        try {
            input = new FileInputStream(new File("/proc/cpuinfo"));
            putQuietly(systemInfo, "cpuinfo", IOUtils.toString(input, "UTF-8"));
        } catch (Throwable ignored) {
        } finally {
            IOUtils.closeQuietly(input);
        }

        try {
            putQuietly(systemInfo, "networkCountryIso", tm.getNetworkCountryIso());
            putQuietly(systemInfo, "networkOperator", tm.getNetworkOperator());
            putQuietly(systemInfo, "networkOperatorName", tm.getNetworkOperatorName());
            putQuietly(systemInfo, "simCountryIso", tm.getSimCountryIso());
            putQuietly(systemInfo, "simOperatorName", tm.getSimOperatorName());
            putQuietly(systemInfo, "simOperator", tm.getSimOperator());
            putQuietly(systemInfo, "deviceSoftwareVersion", tm.getDeviceSoftwareVersion());
            putQuietly(systemInfo, "line1Number", tm.getLine1Number());
            putQuietly(systemInfo, "simSerialNumber", tm.getSimSerialNumber());

            CellLocation cellLocation = tm.getCellLocation();
            if (cellLocation instanceof GsmCellLocation) {
                GsmCellLocation gsm = (GsmCellLocation) cellLocation;
                JSONObject obj = new JSONObject();
                putQuietly(obj, "cid", gsm.getCid());
                putQuietly(obj, "lac", gsm.getLac());
                putQuietly(obj, "psc", gsm.getPsc());
                putQuietly(systemInfo, "gsm", obj);
            } else if (cellLocation instanceof CdmaCellLocation) {
                CdmaCellLocation cdma = (CdmaCellLocation) cellLocation;
                JSONObject obj = new JSONObject();
                putQuietly(obj, "baseStationId", cdma.getBaseStationId());
                putQuietly(obj, "baseStationLatitude", cdma.getBaseStationLatitude());
                putQuietly(obj, "baseStationLongitude", cdma.getBaseStationLongitude());
                putQuietly(obj, "networkId", cdma.getNetworkId());
                putQuietly(obj, "systemId", cdma.getSystemId());
                putQuietly(systemInfo, "cdma", obj);
            } else if(cellLocation != null) {
                putQuietly(systemInfo, "cellLocation", cellLocation);
            }
        } catch(Throwable ignored) {}

        if(wifiInfo != null) {
            putQuietly(systemInfo, "macAddress", wifiInfo.getMacAddress());
            putQuietly(systemInfo, "ssid", wifiInfo.getSSID());
            putQuietly(systemInfo, "bssid", wifiInfo.getBSSID());
            int ipAddress = wifiInfo.getIpAddress();
            putQuietly(systemInfo, "ipAddress", ((ipAddress & 0xFF) + "." + (ipAddress >> 8 & 0xFF) + "." + (ipAddress >> 16 & 0xFF) + "." + (ipAddress >> 24 & 0xFF)));
        }
        try {
            if(activeNetworkInfo != null) {
                putQuietly(systemInfo, "activeNetInfo", activeNetworkInfo.getTypeName());
                putQuietly(systemInfo, "activeNetworkInfo", createNetworkInfo(activeNetworkInfo));
            }
            if (mobileNetworkInfo != null) {
                putQuietly(systemInfo, "mobileNetworkInfo", createNetworkInfo(mobileNetworkInfo));
            }

            JSONArray networkInterfaces = new JSONArray();
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                NetworkInterface networkInterface = enumeration.nextElement();
                JSONObject networkInterfaceObj = new JSONObject();
                networkInterfaceObj.put("name", networkInterface.getName());
                byte[] hardwareAddress = networkInterface.getHardwareAddress();
                if (hardwareAddress != null) {
                    networkInterfaceObj.put("hardwareAddress", Base64.encodeToString(hardwareAddress, Base64.NO_WRAP));
                }
                networkInterfaceObj.put("mtu", networkInterface.getMTU());

                Enumeration<InetAddress> addressEnumeration = networkInterface.getInetAddresses();
                JSONArray array = new JSONArray();
                while (addressEnumeration.hasMoreElements()) {
                    InetAddress address = addressEnumeration.nextElement();
                    if (address.isLoopbackAddress()) {
                        continue;
                    }

                    String host = address.getHostAddress();
                    if (host != null) {
                        array.put(host);
                    }
                }
                networkInterfaceObj.put("addresses", array);

                if (array.length() > 0) {
                    networkInterfaces.put(networkInterfaceObj);
                }
            }
            putQuietly(systemInfo, "networkInterfaces", networkInterfaces);
        } catch(Throwable ignored) {}

        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        putQuietly(systemInfo, "height", displayMetrics.heightPixels);
        putQuietly(systemInfo, "width", displayMetrics.widthPixels);
        putQuietly(systemInfo, "density", displayMetrics.densityDpi);

        JSONArray installed = new JSONArray();
        for(PackageInfo packageInfo : packages) {
            if(packageInfo.applicationInfo == null) {
                continue;
            }
            if(packageInfo.packageName.equals(context.getPackageName())) {
                continue;
            }

            try {
                String name = packageInfo.applicationInfo.loadLabel(context.getPackageManager()).toString();
                String packageName = packageInfo.packageName;
                String versionName = packageInfo.versionName;
                int versionCode = packageInfo.versionCode;

                JSONObject installedPackage = new JSONObject();
                putQuietly(installedPackage, "name", name);
                putQuietly(installedPackage, "packageName", packageName);
                putQuietly(installedPackage, "versionName", versionName);
                putQuietly(installedPackage, "versionCode", versionCode);
                putQuietly(installedPackage, "flags", packageInfo.applicationInfo.flags);
                putQuietly(installedPackage, "firstInstallTime", packageInfo.firstInstallTime);
                putQuietly(installedPackage, "lastUpdateTime", packageInfo.lastUpdateTime);
                installed.put(installedPackage);
            } catch(Throwable ignored) {}
        }
        putQuietly(systemInfo, "installed", installed);

        String userAgent = null;
        if (userAgent == null) {
            try {
                Method getDefaultUserAgent = WebSettings.class.getDeclaredMethod("getDefaultUserAgent", Context.class);
                getDefaultUserAgent.setAccessible(true);
                Object obj = getDefaultUserAgent.invoke(null, context);
                if (obj != null) {
                    userAgent = String.valueOf(obj);
                }
            } catch(Throwable ignored) {}
        }
        if (userAgent == null) {
            userAgent = System.getProperty("http.agent");
        }
        if(userAgent == null) {
            userAgent = getDefaultUserAgent();
        }
        putQuietly(systemInfo, "userAgent", userAgent);
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo info = packageInfoCache == null ? packageManager.getPackageInfo(context.getPackageName(), 0) : packageInfoCache;
            packageInfoCache = info;

            String name = info.applicationInfo.loadLabel(packageManager).toString();
            putQuietly(systemInfo, "name", name);
            putQuietly(systemInfo, "packageName", context.getPackageName());
            putQuietly(systemInfo, "versionName", info.versionName);
            putQuietly(systemInfo, "versionCode", info.versionCode);

            String[] systemSharedLibraryNames = packageManager.getSystemSharedLibraryNames();
            if (systemSharedLibraryNames != null) {
                putQuietly(systemInfo, "systemSharedLibraryNames", new JSONArray(systemSharedLibraryNames));
            }
            FeatureInfo[] systemAvailableFeatures = packageManager.getSystemAvailableFeatures();
            if (systemAvailableFeatures != null) {
                JSONArray array = new JSONArray();
                for (FeatureInfo featureInfo : systemAvailableFeatures) {
                    if (featureInfo != null && featureInfo.name != null) {
                        array.put(featureInfo.name);
                    }
                }
                putQuietly(systemInfo, "systemAvailableFeatures", array);
            }
        } catch(Throwable ignored) {}

        input = null;
        try {
            input = new FileInputStream(new File("/proc/version"));
            putQuietly(systemInfo, "kernel", IOUtils.toString(input, "UTF-8"));
        } catch (Throwable ignored) {
        } finally {
            IOUtils.closeQuietly(input);
        }

        try {
            putQuietly(systemInfo, "properties", getProperties());
        } catch(Throwable ignored) {}

        input = null;
        try {
            input = new FileInputStream(new File("/sys/class/android_usb/android0/iSerial"));
            putQuietly(systemInfo, "iSerial", IOUtils.toString(input, "UTF-8"));
        } catch (Throwable ignored) {
        } finally {
            IOUtils.closeQuietly(input);
        }

        input = null;
        try {
            input = new FileInputStream(new File("/sys/class/android_usb/android0/idProduct"));
            putQuietly(systemInfo, "idProduct", IOUtils.toString(input, "UTF-8"));
        } catch (Throwable ignored) {
        } finally {
            IOUtils.closeQuietly(input);
        }

        input = null;
        try {
            input = new FileInputStream(new File("/sys/class/android_usb/android0/idVendor"));
            putQuietly(systemInfo, "idVendor", IOUtils.toString(input, "UTF-8"));
        } catch (Throwable ignored) {
        } finally {
            IOUtils.closeQuietly(input);
        }

        input = null;
        try {
            input = new FileInputStream(new File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"));
            putQuietly(systemInfo, "cpuinfo_max_freq", IOUtils.toString(input, "UTF-8"));
        } catch (Throwable ignored) {
        } finally {
            IOUtils.closeQuietly(input);
        }

        input = null;
        try {
            input = new FileInputStream(new File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq"));
            putQuietly(systemInfo, "cpuinfo_min_freq", IOUtils.toString(input, "UTF-8"));
        } catch (Throwable ignored) {
        } finally {
            IOUtils.closeQuietly(input);
        }

        input = null;
        try {
            input = new FileInputStream(new File("/sys/devices/system/cpu/present"));
            putQuietly(systemInfo, "cpu_present", IOUtils.toString(input, "UTF-8"));
        } catch (Throwable ignored) {
        } finally {
            IOUtils.closeQuietly(input);
        }

        input = null;
        try {
            input = new FileInputStream(new File("/sys/devices/system/cpu/possible"));
            putQuietly(systemInfo, "cpu_possible", IOUtils.toString(input, "UTF-8"));
        } catch (Throwable ignored) {
        } finally {
            IOUtils.closeQuietly(input);
        }

        input = null;
        try {
            input = new FileInputStream(new File("/system/build.prop"));
            putQuietly(systemInfo, "buildProp", IOUtils.toString(input, "UTF-8"));
        } catch (Throwable ignored) {
        } finally {
            IOUtils.closeQuietly(input);
        }

        input = null;
        try {
            input = new FileInputStream(new File("/default.prop"));
            putQuietly(systemInfo, "defaultProp", IOUtils.toString(input, "UTF-8"));
        } catch (Throwable ignored) {
        } finally {
            IOUtils.closeQuietly(input);
        }

        try {
            SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
                JSONArray array = new JSONArray();
                for (Sensor sensor : sensors) {
                    JSONObject obj = new JSONObject();
                    putQuietly(obj, "name", sensor.getName());
                    putQuietly(obj, "vendor", sensor.getVendor());
                    putQuietly(obj, "maximumRange", sensor.getMaximumRange());
                    putQuietly(obj, "minDelay", sensor.getMinDelay());
                    putQuietly(obj, "power", sensor.getPower());
                    putQuietly(obj, "resolution", sensor.getResolution());
                    putQuietly(obj, "type", sensor.getType());
                    putQuietly(obj, "version", sensor.getVersion());
                    array.put(obj);
                }
                putQuietly(systemInfo, "sensors", array);
            }
        } catch (Throwable ignored) {}

        try {
            putStorageSize(systemInfo, "rootStatFs", Environment.getRootDirectory());
            putStorageSize(systemInfo, "dataStatFs", Environment.getDataDirectory());
            putStorageSize(systemInfo, "externalStatFs", Environment.getExternalStorageDirectory());
            putQuietly(systemInfo, "externalStorageState", Environment.getExternalStorageState());
        } catch (Throwable ignored) {}

        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                putQuietly(systemInfo, "bluetoothName", adapter.getName());
                putQuietly(systemInfo, "bluetoothAddress", adapter.getAddress());
            }
        } catch(Throwable ignored) {}

        if(lm != null) {
            putLocation(lm, systemInfo, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.NETWORK_PROVIDER);
        }

        try {
            int cameras = Camera.getNumberOfCameras();
            JSONArray cameraInfos = new JSONArray();
            for(int i = 0; i < cameras; i++) {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(i, cameraInfo);
                JSONObject obj = new JSONObject();
                obj.put("facing", cameraInfo.facing);
                obj.put("orientation", cameraInfo.orientation);
                Camera camera = null;
                try {
                    camera = Camera.open(i);
                    camera.cancelAutoFocus();
                    List<Camera.Size> sizes = camera.getParameters().getSupportedPictureSizes();
                    JSONArray array = new JSONArray();
                    for (Camera.Size cs : sizes) {
                        JSONObject size = new JSONObject();
                        putQuietly(size, "width", cs.width);
                        putQuietly(size, "height", cs.height);
                        array.put(size);
                    }
                    putQuietly(obj, "supportedPictureSizes", array);
                } catch(Exception ignored) {} finally {
                    if (camera != null) {
                        camera.release();
                    }
                }
                cameraInfos.put(obj);
            }
            putQuietly(systemInfo, "cameraInfos", cameraInfos);
        } catch(Throwable ignored) {}

        try {
            JSONObject audio = new JSONObject();
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            putQuietly(audio, "wiredHeadsetOn", audioManager.isWiredHeadsetOn());
            putQuietly(audio, "musicActive", audioManager.isMusicActive());
            putQuietly(audio, "speakerphoneOn", audioManager.isSpeakerphoneOn());

            systemInfo.put("audio", audio);
        } catch(Throwable ignored) {}

        return systemInfo;
    }

    private static JSONObject createNetworkInfo(NetworkInfo networkInfo) {
        JSONObject obj = new JSONObject();
        putQuietly(obj, "type", networkInfo.getType());
        putQuietly(obj, "typeName", networkInfo.getTypeName());
        putQuietly(obj, "state", networkInfo.getState());
        putQuietly(obj, "reason", networkInfo.getReason());
        putQuietly(obj, "extraInfo", networkInfo.getExtraInfo());
        putQuietly(obj, "roaming", networkInfo.isRoaming());
        putQuietly(obj, "failover", networkInfo.isFailover());
        putQuietly(obj, "available", networkInfo.isAvailable());
        putQuietly(obj, "connected", networkInfo.isConnected());
        putQuietly(obj, "subtype", networkInfo.getSubtype());
        putQuietly(obj, "subtypeName", networkInfo.getSubtypeName());
        return obj;
    }

    private static void putStorageSize(JSONObject systemInfo, String name, File directory) {
        StatFs statFs = new StatFs(directory.getAbsolutePath());
        JSONObject obj = new JSONObject();
        putQuietly(obj, "availableBlocks", statFs.getAvailableBlocks());
        putQuietly(obj, "blockCount", statFs.getBlockCount());
        putQuietly(obj, "blockSize", statFs.getBlockSize());
        putQuietly(obj, "freeBlocks", statFs.getFreeBlocks());
        putQuietly(systemInfo, name, obj);
    }

    @SuppressLint("MissingPermission")
    private static void putLocation(LocationManager lm, JSONObject systemInfo, String...providers) {
        for(String provider : providers) {
            try {
                Location location = lm.getLastKnownLocation(provider);
                if(location != null && location.getLatitude() > 0 && location.getLongitude() > 0) {
                    putQuietly(systemInfo, "latitude", location.getLatitude());
                    putQuietly(systemInfo, "longitude", location.getLongitude());
                    return;
                }
            } catch(Throwable ignored) {}
        }
    }

    private static String getProperties() {
        Process process = null;
        DataOutputStream outStream = null;
        BufferedReader inputStream = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("sh");
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();

            outStream = new DataOutputStream(process.getOutputStream());
            inputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));

            outStream.writeBytes("echo rg_cmd_start_magic\n");
            outStream.writeBytes("getprop" + "\n");
            outStream.flush();
            outStream.writeBytes("echo rg_cmd_end_magic\n");
            outStream.flush();

            int execSucc = 0;
            int acquireSh = 0;
            StringBuilder strRet = new StringBuilder();
            String strRead;
            long curTime = System.currentTimeMillis();

            do {
                if (System.currentTimeMillis() >= curTime + (long) 2000) {
                    break;
                }

                do {
                    strRead = inputStream.readLine();
                    if(strRead == null) {
                        break;
                    }

                    if (acquireSh == 0 && strRead.toLowerCase().contains("permission denied")) {
                        execSucc = 1;
                        break;
                    } else {
                        if (acquireSh == 0) {
                            acquireSh = 1;
							/*if (strRead.contains("rg_cmd_start_magic")) {
								continue;
							}*/
                        } else {
                            if (strRead.contains("rg_cmd_end_magic")) {
                                execSucc = 1;
                                break;
                            } else {
                                if (strRet.length() < 1) {
                                    strRet = new StringBuilder(strRead);
                                } else {
                                    strRet.append("\n");
                                    strRet.append(strRead);
                                }
                            }
                        }
                    }
                } while (true);

                if (execSucc == 1) {
                    break;
                }
            } while (true);

            return strRet.toString();

        } catch (Exception e) {
            return "execCmd exception: " + e.getMessage() + ", isSuExec: " + false + ", cmd: " + "getprop";
        } finally {
            IOUtils.closeQuietly(outStream);
            IOUtils.closeQuietly(inputStream);
            if(process != null) {
                process.destroy();
            }
        }
    }

    private static Method getRadioVersion;

    @SuppressLint("ObsoleteSdkInt")
    private static String getRadioVersion() {
        try {
            if(Build.VERSION.SDK_INT < 14) {
                return Build.RADIO;
            }

            if(getRadioVersion == null) {
                getRadioVersion = Build.class.getDeclaredMethod("getRadioVersion");
            }
            return (String) getRadioVersion.invoke(null);
        } catch(Throwable e) {
            return "radio_fail";
        }
    }

    private static Field SERIAL;

    @SuppressLint("ObsoleteSdkInt")
    private static String getSerial() {
        try {
            if(Build.VERSION.SDK_INT < 9) {
                return "serial_" + Build.VERSION.SDK_INT;
            }

            if(SERIAL == null) {
                SERIAL = Build.class.getDeclaredField("SERIAL");
            }
            return (String) SERIAL.get(null);
        } catch(Throwable e) {
            return "serial_fail";
        }
    }

    private static String getDefaultUserAgent() {
        StringBuilder result = new StringBuilder(64);
        result.append("Dalvik/");
        result.append(System.getProperty("java.vm.version")); // such as 1.1.0
        result.append(" (Linux; U; Android ");

        String version = Build.VERSION.RELEASE; // "1.0" or "3.4b5"
        result.append(version.length() > 0 ? version : "1.0");

        // add the model for the release build
        if ("REL".equals(Build.VERSION.CODENAME)) {
            String model = Build.MODEL;
            if (model.length() > 0) {
                result.append("; ");
                result.append(model);
            }
        }
        String id = Build.ID; // "MASTER" or "M4-rc20"
        if (id.length() > 0) {
            result.append(" Build/");
            result.append(id);
        }
        result.append(")");
        return result.toString();
    }

    private static void putQuietly(JSONObject obj, String name, Object value) {
        if(obj == null ||
                name == null ||
                value == null) {
            return;
        }

        try {
            obj.put(name, value);
        } catch(Throwable ignored) {}
    }

}
