package com.fuzhu8.inspector.plugin;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Base64;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.advisor.Hooker;
import com.fuzhu8.inspector.advisor.MethodHookAdapter;
import com.fuzhu8.inspector.advisor.MethodHookParam;
import com.fuzhu8.inspector.completer.ServerCommandCompleter;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.io.Console;
import com.fuzhu8.inspector.script.LuaScriptManager;
import com.fuzhu8.tcpcap.handler.SessionHandler;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author zhkl0228
 *
 */
@SuppressWarnings("unused")
public abstract class AbstractPlugin implements Plugin {
	
	protected final com.fuzhu8.inspector.Inspector inspector;
	protected final DexFileManager dexFileManager;
	protected final LuaScriptManager scriptManager;
	protected final ModuleContext context;
	protected final Hooker hooker;
	protected final PluginContext pluginContext;
	private final ServerCommandCompleter serverCommandCompleter;

	/**
	 * 在handleLoadPackage调用
	 */
	public AbstractPlugin(PluginContext pluginContext) {
		super();
		
		this.inspector = pluginContext.getInspector();
		this.dexFileManager = pluginContext.getDexFileManager();
		this.scriptManager = pluginContext.getScriptManager();
		this.context = pluginContext.getContext();
		this.hooker = pluginContext.getHooker();
		this.pluginContext = pluginContext;
		ServerCommandCompleter completer = createCommandCompleter(inspector);
		this.serverCommandCompleter = completer;

		if (completer != null) {
			String registerId = getRegisterId();
			completer.addCommandHelp(registerId + ":hookHash()", registerId + ":hookHash(); -- hook hash related class");
			completer.addCommandHelp(registerId + ":hookCrypto()", registerId + ":hookCrypto(); -- hook crypto related class");
			completer.addCommandHelp(registerId + ":hookHttp()", registerId + ":hookHttp(); -- hook http related class");
			completer.addCommandHelp(registerId + ":hookMisc()", registerId + ":hookMisc(); -- hook clipboard");
			completer.addCommandHelp(registerId + ":hookSQLite()", registerId + ":hookSQLite(); -- hook sqlite related class");
			completer.addCommandHelp(registerId + ":hookFileSystem()", registerId + ":hookFileSystem(); -- hook file related class");
		}
		
		try {
			scriptManager.registerGlobalObject(getRegisterId(), this);
		} catch(Exception e) {
			hooker.log(e);
		}
	}
	
	protected ServerCommandCompleter createCommandCompleter(Inspector inspector) {
		return null;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "_v" + pluginContext.getVersionName() + '_' + pluginContext.getVersionCode();
	}

	@Override
	public void notifySendTextMessage(String destinationAddress, String scAddress, String text, PendingIntent sentIntent,
			PendingIntent deliveryIntent, String label) {
		inspector.println(label + ": " + destinationAddress + '\n' + text);
	}

	@Override
	public void notifySendDataMessage(String destinationAddress, String scAddress, short destinationPort, byte[] data,
			PendingIntent sentIntent, PendingIntent deliveryIntent) {
		inspector.inspect(data, "sendDataMessage destinationAddress=" + destinationAddress +
				", scAddress=" + scAddress +
				", destinationPort=" + destinationPort +
				", md5=" + com.fuzhu8.inspector.DigestUtils.md5Hex(data));
	}

	@Override
	public final String getHelpContent() {
		if(serverCommandCompleter == null) {
			return "";
		}
		return serverCommandCompleter.describeHelp();
	}

	@Override
	public final void onConnected(Console console) {
		if(this.serverCommandCompleter != null) {
			this.serverCommandCompleter.commit();
		}
	}
	
	private boolean initialized;
	private Context appContext;

	@Override
	public final void initialize(Context context) {
		if(initialized) {
			return;
		}
		
		try {
			inspector.println("Initializing plugin: " + this);
			this.appContext = context;
			initializeInternal(context);
		} finally {
			initialized = true;
		}
	}

	protected abstract void initializeInternal(Context context);
	protected abstract String getRegisterId();

	@Override
	public void onClosed(Console console) {
	}

	@Override
	public SessionHandler createSessionHandler() {
		return null;
	}

	@SuppressWarnings("unused")
	public final void hookHash() {
		try {
			Method update1 = MessageDigest.class.getDeclaredMethod("update", byte[].class);
			hooker.hookMethod(update1, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					inspector.inspect((byte[]) param.args[0], param.thisObject + " update");
				}
			}, true);

			Method update2 = MessageDigest.class.getDeclaredMethod("update", byte[].class, int.class, int.class);
			hooker.hookMethod(update2, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					inspector.inspect(Arrays.copyOfRange((byte[]) param.args[0], (Integer) param.args[1], (Integer) param.args[2]), param.thisObject + " update");
				}
			}, true);

			Method update3 = MessageDigest.class.getDeclaredMethod("update", ByteBuffer.class);
			hooker.hookMethod(update3, new MethodHookAdapter() {
				@Override
				public void beforeHookedMethod(MethodHookParam param) throws Throwable {
					super.beforeHookedMethod(param);

					inspector.inspect(((ByteBuffer) param.args[0]).duplicate(), param.thisObject + " update");
				}
			}, true);

			Method digest1 = MessageDigest.class.getDeclaredMethod("digest");
			hooker.hookMethod(digest1, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					inspector.inspect((byte[]) param.getResult(), param.thisObject + " digest");
				}
			}, true);

			Method digest2 = MessageDigest.class.getDeclaredMethod("digest", byte[].class);
			hooker.hookMethod(digest2, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					inspector.inspect((byte[]) param.args[0], param.thisObject + " update");
					inspector.inspect((byte[]) param.getResult(), param.thisObject + " digest");
				}
			}, true);

			Method digest3 = MessageDigest.class.getDeclaredMethod("digest", byte[].class, int.class, int.class);
			hooker.hookMethod(digest3, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					inspector.inspect(Arrays.copyOfRange((byte[]) param.args[0], (Integer) param.args[1], (Integer) param.args[2]), param.thisObject + " update");
					inspector.inspect((byte[]) param.getResult(), param.thisObject + " digest");
				}
			}, true);

			inspector.println("hook hash successfully.");
		} catch (Throwable throwable) {
			inspector.err_println(throwable);
		}
	}

	@SuppressWarnings("unused")
	public final void hookCrypto() {
		try {
			Constructor<?> constructorSecretKeySpec = SecretKeySpec.class.getDeclaredConstructor(byte[].class, String.class);
			hooker.hookMethod(constructorSecretKeySpec, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					inspector.inspect((byte[]) param.args[0], "SecretKeySpec algorithm=" + param.args[1]);
				}
			}, true);

			Method doFinal = Cipher.class.getDeclaredMethod("doFinal", byte[].class);
			hooker.hookMethod(doFinal, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					inspector.inspect((byte[]) param.args[0], param.thisObject + " doFinal");
					inspector.inspect((byte[]) param.getResult(), param.thisObject + " doFinal Result");
				}
			}, true);

			Method getIV = Cipher.class.getDeclaredMethod("getIV");
			hooker.hookMethod(getIV, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					inspector.inspect((byte[]) param.getResult(), param.thisObject + " IV");
				}
			}, true);

			Constructor<?> constructorIvParameterSpec = IvParameterSpec.class.getDeclaredConstructor(byte[].class);
			hooker.hookMethod(constructorIvParameterSpec, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					inspector.inspect((byte[]) param.args[0], "IvParameterSpec");
				}
			}, true);

			Method setSeed = SecureRandom.class.getDeclaredMethod("setSeed", byte[].class);
			hooker.hookMethod(setSeed, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					inspector.inspect((byte[]) param.args[0], param.thisObject + " Seed");
				}
			}, true);

			Method getInstance = Cipher.class.getDeclaredMethod("getInstance", String.class);
			hooker.hookMethod(getInstance, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					inspector.println("Cipher[" + param.args[0] + "] ");
				}
			}, true);

			Constructor<?> constructorPBEKeySpec = PBEKeySpec.class.getDeclaredConstructor(char[].class, byte[].class, int.class, int.class);
			hooker.hookMethod(constructorPBEKeySpec, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					inspector.inspect((byte[]) param.args[1], "[PBEKeySpec] - Password: " + String.valueOf((char[]) param.args[0]));
				}
			}, true);

			inspector.println("hook crypto successfully.");
		} catch (Throwable throwable) {
			inspector.err_println(throwable);
		}
	}

	@SuppressLint("PrivateApi")
	@SuppressWarnings("unused")
	public final void hookHttp() {
		try {
			Constructor<?> constructorHttpURLConnection = java.net.HttpURLConnection.class.getDeclaredConstructor(URL.class);
			hooker.hookMethod(constructorHttpURLConnection, new MethodHookAdapter() {
				@Override
				public void beforeHookedMethod(MethodHookParam param) throws Throwable {
					super.beforeHookedMethod(param);

					inspector.println("HttpURLConnection: " + param.args[0]);
				}
			}, true);

			Class<?> OkHttpClient = context.getClassLoader().loadClass("com.android.okhttp.OkHttpClient");
			Method open = OkHttpClient.getDeclaredMethod("open", URL.class);
			hooker.hookMethod(open, new MethodHookAdapter() {
				@Override
				public void beforeHookedMethod(MethodHookParam param) throws Throwable {
					super.beforeHookedMethod(param);

					inspector.println("OkHttpClient: " + param.args[0]);
				}
			}, true);

			Class<?> HttpURLConnectionImpl = context.getClassLoader().loadClass("com.android.okhttp.internal.http.HttpURLConnectionImpl");
			Method getOutputStream = HttpURLConnectionImpl.getDeclaredMethod("getOutputStream");
			hooker.hookMethod(getOutputStream, new MethodHookAdapter() {
				@Override
				public void beforeHookedMethod(MethodHookParam param) throws Throwable {
					super.beforeHookedMethod(param);

					HttpURLConnection urlConn = (HttpURLConnection) param.thisObject;

					if (urlConn != null) {
						Field connectedField = URLConnection.class.getDeclaredField("connected");
						connectedField.setAccessible(true);
						boolean connected = (Boolean) connectedField.get(urlConn);

						if(!connected){
							StringBuilder sb = new StringBuilder();
							Map<String, List<String>> properties = urlConn.getRequestProperties();
							if (properties != null && properties.size() > 0) {

								for (Map.Entry<String, List<String>> entry : properties.entrySet()){
									sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(", ");
								}
							}

							inspector.println("REQUEST: method=" + urlConn.getRequestMethod() + " " + "URL=" + urlConn.getURL() + " " + "Params=" + sb);
						}
					}
				}
			}, true);

			Method getInputStream = HttpURLConnectionImpl.getDeclaredMethod("getInputStream");
			hooker.hookMethod(getInputStream, new MethodHookAdapter() {
				@Override
				public void beforeHookedMethod(MethodHookParam param) throws Throwable {
					super.beforeHookedMethod(param);

					HttpURLConnection urlConn = (HttpURLConnection) param.thisObject;

					if (urlConn != null) {
						int code = urlConn.getResponseCode();
						StringBuilder sb = new StringBuilder();
						if(code == 200){
							Map<String, List<String>> properties = urlConn.getHeaderFields();
							if (properties != null && properties.size() > 0) {
								for (Map.Entry<String, List<String>> entry : properties.entrySet()) {
									sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(", ");
								}
							}
						}

						inspector.println("RESPONSE: method=" + urlConn.getRequestMethod() + " " +
								"URL=" + urlConn.getURL() + " " + "Params=" + sb);
					}
				}
			}, true);

			inspector.println("hook http successfully.");
		} catch (Throwable throwable) {
			inspector.err_println(throwable);
		}
	}

	@SuppressWarnings("unused")
	public final void hookMisc() {
		try {
			Method setPrimaryClip = ClipboardManager.class.getDeclaredMethod("setPrimaryClip", ClipData.class);
			hooker.hookMethod(setPrimaryClip, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					ClipData cd = (ClipData) param.args[0];
					StringBuilder sb = new StringBuilder();
					if (cd != null && cd.getItemCount() > 0) {
						for (int i = 0; i < cd.getItemCount(); i++) {
							ClipData.Item item = cd.getItemAt(i);
							sb.append(item.getText());
						}
					}
					inspector.println("Copied to the clipboard: " + sb);
				}
			}, true);

			Method parse = Uri.class.getDeclaredMethod("parse", String.class);
			hooker.hookMethod(parse, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					inspector.println("URI: " + param.args[0]);
				}
			}, true);

			inspector.println("hook misc successfully.");
		} catch (Throwable throwable) {
			inspector.err_println(throwable);
		}
	}

	@SuppressWarnings("unused")
	public final void hookSQLite() {
		try {
			Method execSQL1 = SQLiteDatabase.class.getDeclaredMethod("execSQL", String.class);
			hooker.hookMethod(execSQL1, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					inspector.println("execSQL(" + param.args[0] + ")");
				}
			}, true);

			Method execSQL2 = SQLiteDatabase.class.getDeclaredMethod("execSQL", String.class, Object[].class);
			hooker.hookMethod(execSQL2, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					Object[] args = (Object[]) param.args[1];
					inspector.println("execSQL(" + param.args[0] + ") with args: " + Arrays.toString(args));
				}
			}, true);

			Method update = SQLiteDatabase.class.getDeclaredMethod("update", String.class, ContentValues.class, String.class, String[].class);
			hooker.hookMethod(update, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					SQLiteDatabase sqlitedb = (SQLiteDatabase) param.thisObject;

					ContentValues contentValues = (ContentValues) param.args[1];
					StringBuilder sb = new StringBuilder();

					Set<Map.Entry<String, Object>> s = contentValues.valueSet();
					for (Map.Entry<String, Object> entry : s) {
						sb.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
					}

					StringBuilder sbuff = new StringBuilder();
					if (param.args[3] != null) {
						for (String str : (String[]) param.args[3]) {
							sbuff.append(str).append(",");
						}
					}

					String set = "";
					if (sb.toString().length() > 1) {
						set = sb.toString().substring(0, sb.length() - 1);
					}

					String whereArgs = "";
					if (sbuff.toString().length() > 1) {
						whereArgs = sbuff.toString().substring(0, sbuff.length() - 1);
					}

					inspector.err_println("UPDATE " + param.args[0] + " SET " + set + "" + " WHERE " + param.args[2] + "" + whereArgs);
				}
			}, true);

			Method insert = SQLiteDatabase.class.getDeclaredMethod("insert", String.class, String.class, ContentValues.class);
			hooker.hookMethod(insert, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					ContentValues contentValues = (ContentValues) param.args[2];
					StringBuilder sb = new StringBuilder();

					for (Map.Entry<String, Object> entry : contentValues.valueSet()) {
						sb.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
					}
					inspector.err_println("INSERT INTO " + param.args[0] + " VALUES(" + sb.toString().substring(0, sb.length() - 1) + ")");
				}
			}, true);

			Method managedQuery = Activity.class.getDeclaredMethod("managedQuery", Uri.class, String[].class, String.class, String[].class, String.class);
			hooker.hookMethod(managedQuery, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					Uri uri = (Uri) param.args[0];

					StringBuilder projection = new StringBuilder();
					if (param.args[1] != null) {
						for (String str : (String[]) param.args[1]) {
							projection.append(str).append(",");
						}
					}

					String selection = "";
					if (param.args[2] != null) {
						selection = " WHERE " + param.args[2] + " = ";
					}

					StringBuilder selectionArgs = new StringBuilder();
					if (param.args[3] != null) {
						for (String str : (String[]) param.args[3]) {
							selectionArgs.append(str).append(",");
						}
					}

					String sortOrder = "";
					if (param.args[4] != null) {
						sortOrder = " ORDER BY " + param.args[4];
					}

					String projec = "";
					if (projection.toString().equals("")) {
						projec = "*";//projection.append("*");
					} else {
						projec = projection.toString().substring(0, projection.length() - 1);
					}

					Cursor cursor = (Cursor) param.getResult();

					StringBuilder result = new StringBuilder();

					if (cursor != null) {
						if (cursor.moveToFirst()) {
							do {
								int x = cursor.getColumnCount();
								StringBuilder sb = new StringBuilder();
								for (int i = 0; i < x; i++) {

									if (cursor.getType(i) == Cursor.FIELD_TYPE_BLOB) {
										String blob = Base64.encodeToString(cursor.getBlob(i), Base64.NO_WRAP);
										sb.append(cursor.getColumnName(i)).append("=").append(blob).append(",");
									} else {
										sb.append(cursor.getColumnName(i)).append("=").append(cursor.getString(i)).append(",");
									}
								}
								result.append(sb.toString().substring(0, sb.length() - 1)).append("\n");

							} while (cursor.moveToNext());
						}
					}

					inspector.println("SELECT " + projec + " FROM " + uri.getAuthority() + uri.getPath() + selection + selectionArgs.toString() + sortOrder + "\n   [" + result.toString() + "]");
				}
			}, true);

			Method query = SQLiteDatabase.class.getDeclaredMethod("query", String.class, String[].class, String.class, String[].class, String.class, String.class, String.class, String.class);
			hooker.hookMethod(query, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					String table = (String) param.args[0];

					StringBuilder csb = new StringBuilder();
					if (param.args[1] != null) {
						for (String str : (String[]) param.args[1]) {
							csb.append(str).append(",");
						}
					}

					String selection = "";
					if (param.args[2] != null) {
						selection = " WHERE " + param.args[2] + " = ";
					}

					StringBuilder selectionArgs = new StringBuilder();
					if (param.args[3] != null) {
						for (String str : (String[]) param.args[3]) {
							selectionArgs.append(str).append(",");
						}
					}

					String groupBy = "";
					if (param.args[4] != null) {
						groupBy = " GROUP BY " + param.args[4];
					}

					String sortOrder = "";
					if (param.args[6] != null) {
						sortOrder = " ORDER BY " + param.args[6];
					}

					if (csb.toString().equals("")) {
						csb.append("*");
					}

					Cursor cursor = (Cursor) param.getResult();

					StringBuilder result = new StringBuilder();

					if (cursor != null)
						if (cursor.moveToFirst()) {
							do {
								int x = cursor.getColumnCount();
								StringBuilder sb = new StringBuilder();
								for (int i = 0; i < x; i++) {
									if(cursor.getType(i) == Cursor.FIELD_TYPE_BLOB){
										String blob = Base64.encodeToString(cursor.getBlob(i), Base64.NO_WRAP);
										sb.append(cursor.getColumnName(i)).append("=").append(blob).append(",");
									} else {
										sb.append(cursor.getColumnName(i)).append("=").append(cursor.getString(i)).append(",");
									}
								}
								result.append(sb.toString().substring(0, sb.length() - 1)).append("\n");

							} while (cursor.moveToNext());
						}

					inspector.println("SELECT " + csb.toString() + " FROM " + table + selection + selectionArgs.toString() + sortOrder + groupBy + "\n" + result.toString() + "");
				}
			}, true);

			Method getDatabasePath = ContextWrapper.class.getDeclaredMethod("getDatabasePath", String.class);
			hooker.hookMethod(getDatabasePath, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					inspector.println("[Context] getDatabasePath(" + param.args[0] + ")");
				}
			}, true);

			try {
				Class<?> SQLiteDatabase = context.getClassLoader().loadClass("net.sqlcipher.database.SQLiteDatabase");
				Method execSQL = SQLiteDatabase.getDeclaredMethod("execSQL", String.class);
				hooker.hookMethod(execSQL, new MethodHookAdapter() {
					@Override
					public void afterHookedMethod(MethodHookParam param) throws Throwable {
						super.afterHookedMethod(param);

						inspector.println("[SQLCipher] execSQL(" + param.args[0] + ")");
					}
				}, true);

				execSQL = SQLiteDatabase.getDeclaredMethod("execSQL", String.class, Object[].class);
				hooker.hookMethod(execSQL, new MethodHookAdapter() {
					@Override
					public void afterHookedMethod(MethodHookParam param) throws Throwable {
						super.afterHookedMethod(param);

						Object[] args = (Object[]) param.args[1];
						inspector.println("[SQLCipher] execSQL(" + param.args[0] + ") with args: " + Arrays.toString(args));
					}
				}, true);

				Method openOrCreateDatabase = SQLiteDatabase.getDeclaredMethod("openOrCreateDatabase", File.class, String.class);
				hooker.hookMethod(openOrCreateDatabase, new MethodHookAdapter() {
					@Override
					public void afterHookedMethod(MethodHookParam param) throws Throwable {
						super.afterHookedMethod(param);

						File file = (File) param.args[0];
						String password = (String) param.args[1];
						inspector.println("[SQLCipher] Open or Create:" + file + " with password: " + password);
					}
				}, true);

				Method rawQuery = SQLiteDatabase.getDeclaredMethod("rawQuery", String.class, String[].class);
				hooker.hookMethod(rawQuery, new MethodHookAdapter() {
					@Override
					public void afterHookedMethod(MethodHookParam param) throws Throwable {
						super.afterHookedMethod(param);

						Object[] args = (Object[]) param.args[1];
						inspector.println("[SQLCipher] rawQuery(" + param.args[0] + ") with args: " + Arrays.toString(args));
					}
				}, true);
			} catch(Throwable ignored) {}

			inspector.println("hook sqlite successfully.");
		} catch (Throwable throwable) {
			inspector.err_println(throwable);
		}
	}

	@SuppressLint("SdCardPath")
	@SuppressWarnings("unused")
	public final void hookFileSystem() {
		try {
			Method openFileOutput = ContextWrapper.class.getDeclaredMethod("openFileOutput", String.class, int.class);
			hooker.hookMethod(openFileOutput, new MethodHookAdapter() {
				@SuppressLint({"WorldReadableFiles", "WorldWriteableFiles"})
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					String name = (String) param.args[0];
					int mode = (Integer) param.args[1];
					String m;
					switch (mode) {
						case android.content.Context.MODE_PRIVATE:
							m = "MODE_PRIVATE";
							break;
						case android.content.Context.MODE_WORLD_READABLE:
							m = "MODE_WORLD_READABLE";
							break;
						case android.content.Context.MODE_WORLD_WRITEABLE:
							m = "MODE_WORLD_WRITEABLE";
							break;
						case android.content.Context.MODE_APPEND:
							m = "MODE_APPEND";
							break;
						default:
							m = "?";
							break;
					}

					inspector.println("openFileOutput(\"" + name + "\", " + m + ")");
				}
			}, true);

			Constructor<?> constructorFile1 = File.class.getConstructor(String.class);
			hooker.hookMethod(constructorFile1, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					String str = (String) param.args[0];
					if (str != null && (str.contains("/sdcard") || str.contains("/storage"))) {
						inspector.err_println("R/W [new File(String)]: " + str);
					} else {
						inspector.println("R/W [new File(String)]: " + str);
					}
				}
			}, true);

			Constructor<?> constructorFile2 = File.class.getConstructor(String.class, String.class);
			hooker.hookMethod(constructorFile2, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					String dir = (String) param.args[0];
					String fileName = (String) param.args[1];
					if(dir != null && (dir.contains("/sdcard") || dir.contains("/storage"))) {
						inspector.err_println("R/W Dir: " + dir + '/' + fileName);
					} else {
						inspector.println("R/W Dir: " + dir + '/' + fileName);
					}
				}
			}, true);

			Constructor<?> constructorFile3 = File.class.getConstructor(File.class, String.class);
			hooker.hookMethod(constructorFile3, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					File fileDir = (File) param.args[0];
					String fileName = (String) param.args[1];
					if(fileDir != null && (fileDir.toString().contains("/sdcard") || fileDir.toString().contains("/storage"))) {
						inspector.err_println("R/W Dir: " + fileDir + '/' + fileName);
					} else {
						inspector.println("R/W Dir: " + fileDir + '/' + fileName);
					}
				}
			}, true);

			Constructor<?> constructorFile4 = File.class.getConstructor(URI.class);
			hooker.hookMethod(constructorFile4, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					URI uri = (URI) param.args[0];
					if(uri != null && (uri.toString().contains("/sdcard") || uri.toString().contains("/storage"))) {
						inspector.err_println("R/W [new File(URI)]: " + uri);
					} else {
						inspector.println("R/W [new File(URI)]: " + uri);
					}
				}
			}, true);

			inspector.println("hook file system successfully!");
		} catch (Throwable throwable) {
			inspector.err_println(throwable);
		}
	}

}
