/*
 * $Id: LuaJavaAPI.java,v 1.4 2006/12/22 14:06:40 thiago Exp $
 * Copyright (C) 2003-2007 Kepler Project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.keplerproject.luajava;

import android.util.Log;

import com.fuzhu8.inspector.script.MethodHashUtils;
import com.taobao.android.dexposed.XposedHelpers;
import com.taobao.android.dexposed.callbacks.XCMethodPointer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that contains functions accessed by lua.
 * 
 * @author Thiago Ponte
 */
public final class LuaJavaAPI
{

  private LuaJavaAPI()
  {
	  super();
  }
  
  static void objectNewIndex(int luaState, Object obj, String fieldName) {
	  LuaState L = LuaStateFactory.getExistingState(luaState);

	    synchronized (L)
	    {
	      Class objClass = obj.getClass();
	      
	      /*
	       * if obj is class, then search static field first, else search the instance field first.
	       */
	      
	      Field field = fieldName.length() != 32 ? null : findHashField(objClass, fieldName);
	      if(field == null && Class.class.isInstance(obj)) {
	    	  field = findField(objClass, fieldName, true);
	      }
	      if(field == null) {
	    	  field = findField(objClass, fieldName, false);
	      }

	      if (field == null)
	      {
	        return;
	      }
	      
			try {
				Object value = compareTypes(L, field.getType(), 3);
				if(!field.isAccessible()) {
					field.setAccessible(true);
				}
				if(Modifier.isStatic(field.getModifiers())) {
					field.set(null, value);
				} else {
					field.set(obj, value);
				}
			} catch (Exception e) {
				Log.d("luajava", e.getMessage(), e);
			}
	    }
  }

  /**
   * Java implementation of the metamethod __index
   * 
   * @param luaState int that indicates the state used
   * @param obj Object to be indexed
   * @param methodName the name of the method
   * @return number of returned objects
   */
  public static int objectIndex(int luaState, Object obj, String methodName)
      throws LuaException, IllegalAccessException, InvocationTargetException, InstantiationException
  {
	    LuaState L = LuaStateFactory.getExistingState(luaState);

	    synchronized (L)
	    {
	      int top = L.getTop();

	      Object[] objs = new Object[top - 1];
	      Method method = null;
	      
	      if(obj instanceof Class) {
	    	  Class clazz = (Class) obj;
	    	  
	    	  if("new".equals(methodName)) {
		    	  L.pushJavaObject(getObjInstance(L, clazz));
		    	  return 1;
	    	  }
	    	  
	    	  method = findMethod(clazz, methodName, L, top, false, objs, obj, true);
	      }

	      Class clazz = obj.getClass();
	      
	      if(method == null) {
	    	  boolean isXC_invoked = isXC_invoked(clazz, methodName);
		      if(isXC_invoked) {
		    	  objs = new Object[2];
		      }
		      
		      method = findMethod(clazz, methodName, L, top, isXC_invoked, objs, obj, false);
	      }

	      // If method is null means there isn't one receiving the given arguments
	      if (method == null)
	      {
	        throw new LuaException("Invalid method call. No such method: obj=" + obj + ", methodName=" + methodName + ", top=" + top);
	      }

	      Object ret;
	      if(!method.isAccessible())
	      {
	        method.setAccessible(true);
	      }
	      
	      if (Modifier.isStatic(method.getModifiers()))
	      {
	        ret = method.invoke(null, objs);
	      }
	      else
	      {
	        ret = method.invoke(obj, objs);
	      }

	      // Void function returns null
	      if (ret == null)
	      {
	        return 0;
	      }

	      // push result
	      L.pushObjectValue(ret);

	      return 1;
	    }
  }

	private static Method findMethod(Class clazz, String methodName, LuaState L, int top, boolean isXC_invoked, Object[] objs, Object obj, boolean staticOnley) {
		if(clazz == null) {
			return null;
		}
		
		Method method = null;
		Method[] methods = clazz.getDeclaredMethods();
		List<Method> staticMethods = new ArrayList<Method>(methods.length);
		List<Method> instanceMethods = new ArrayList<Method>(methods.length);
		for(Method m : methods) {
			if(Modifier.isStatic(m.getModifiers())) {
				staticMethods.add(m);
			} else {
				instanceMethods.add(m);
			}
		}

	      // gets method and arguments
	      for (Method m : (staticOnley ? staticMethods : instanceMethods))
	      {
	    	  boolean matchesName = m.getName().equals(methodName);
	    	  if(!matchesName && methodName.length() == 32 && MethodHashUtils.hashMethod(m).equals(methodName)) {
	    		  matchesName = true;
	    	  }
	    	  
	        if (!matchesName) {
	          continue;
	        }
	        
	        if(isXC_invoked) {
	        	Class[] parameters = (Class[]) XposedHelpers.callMethod(obj, "getParameterTypes");
	            if (parameters.length != top - 2) {
	              continue;
	            }
	            
	            boolean okMethod = true;
	            try {
	            	objs[0] = compareTypes(L, (Class) XposedHelpers.callMethod(obj, "getDeclaringClass"), 2);
	            } catch(Exception e) {
	            	Log.d(LuaJavaAPI.class.getSimpleName(), e.getMessage(), e);
					break;
	            }
	            Object[] args = new Object[parameters.length];
	            for (int j = 0; j < parameters.length; j++) {
	              try {
	            	 args[j] = compareTypes(L, parameters[j], j + 3);
	              } catch (Exception e) {
		            Log.d(LuaJavaAPI.class.getSimpleName(), e.getMessage(), e);
	                okMethod = false;
	                break;
	              }
	            }

	            if (okMethod) {
	              objs[1] = args;
	              method = m;
	              break;
	            }
	            continue;
	        }

	        Class[] parameters = m.getParameterTypes();
	        if (parameters.length != top - 1) {
	          continue;
	        }

	        boolean okMethod = true;

	        for (int j = 0; j < parameters.length; j++)
	        {
	          try
	          {
	            objs[j] = compareTypes(L, parameters[j], j + 2);
	          }
	          catch (Exception e)
	          {
	            	Log.d(LuaJavaAPI.class.getSimpleName(), e.getMessage(), e);
	            okMethod = false;
	            break;
	          }
	        }

	        if (okMethod)
	        {
	          method = m;
	          break;
	        }

	      }
	      
	      if(method != null) {
	    	  return method;
	      }
	      
	      return findMethod(clazz.getSuperclass(), methodName, L, top, isXC_invoked, objs, obj, staticOnley);
	}

  private static boolean isXC_invoked(Class<?> clazz, String methodName) {
	  Class<?> XCMethodPointer = XCMethodPointer.class;
	  if(XCMethodPointer.isAssignableFrom(clazz)) {
		  return true;
	  }
	  
	  if(!"invoke".equals(methodName) && !"call".equals(methodName)) {
		  return false;
	  }
	  
	  try {
		  XCMethodPointer = clazz.getClassLoader().loadClass("de.robv.android.xposed.callbacks.XCMethodPointer");
		  return XCMethodPointer.isAssignableFrom(clazz);
	  } catch(ClassNotFoundException e) {
		  return false;
	  }
}

/**
   * Java function to be called when a java Class metamethod __index is called.
   * This function returns 1 if there is a field with searchName and 2 if there
   * is a method if the searchName
   * 
   * @param luaState int that represents the state to be used
   * @param clazz class to be indexed
   * @param searchName name of the field or method to be accessed
   * @return number of returned objects
   */
  public static int classIndex(int luaState, Class clazz, String searchName)
      throws LuaException
  {
    synchronized (LuaStateFactory.getExistingState(luaState))
    {
      int res;

      res = checkField(luaState, clazz, searchName);

      if (res != 0)
      {
        return 1;
      }

      res = checkMethod(luaState, clazz, searchName);

      if (res != 0)
      {
        return 2;
      }

      return 0;
    }
  }

  /**
   * Pushes a new instance of a java Object of the type className
   * 
   * @param luaState int that represents the state to be used
   * @param className name of the class
   * @return number of returned objects
   */
  public static int javaNewInstance(int luaState, String className)
      throws LuaException, InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException
  {
    LuaState L = LuaStateFactory.getExistingState(luaState);

    synchronized (L)
    {
      Class clazz = Class.forName(className);
      Object ret = getObjInstance(L, clazz);

      L.pushJavaObject(ret);

      return 1;
    }
  }

  /**
   * javaNew returns a new instance of a given clazz
   * 
   * @param luaState int that represents the state to be used
   * @param clazz class to be instanciated
   * @return number of returned objects
   */
  public static int javaNew(int luaState, Class clazz) throws LuaException, InstantiationException, IllegalAccessException, InvocationTargetException
  {
    LuaState L = LuaStateFactory.getExistingState(luaState);

    synchronized (L)
    {
      Object ret = getObjInstance(L, clazz);

      L.pushJavaObject(ret);

      return 1;
    }
  }

  /**
   * Calls the static method <code>methodName</code> in class <code>className</code>
   * that receives a LuaState as first parameter.
   * @param luaState int that represents the state to be used
   * @param className name of the class that has the open library method
   * @param methodName method to open library
   * @return number of returned objects
   */
  public static int javaLoadLib(int luaState, String className, String methodName)
  	throws LuaException, ClassNotFoundException
  {
    LuaState L = LuaStateFactory.getExistingState(luaState);
    
    synchronized (L)
    {
      Class clazz = Class.forName(className);

      try
      {
        Method mt = clazz.getMethod(methodName, LuaState.class);
        Object obj = mt.invoke(null, L);
        
        if (obj != null && obj instanceof Integer)
        {
          return (Integer) obj;
        }
        else
          return 0;
      }
      catch (Exception e)
      {
        throw new LuaException("Error on calling method. Library could not be loaded. " + e.getMessage());
      }
    }
  }

  private static Object getObjInstance(LuaState L, Class clazz)
      throws LuaException, InstantiationException, IllegalAccessException, InvocationTargetException
  {
    synchronized (L)
    {
	    int top = L.getTop();
	
	    Object[] objs = new Object[top - 1];
	
	    Constructor[] constructors = clazz.getConstructors();
	    Constructor constructor = null;
	
	    // gets method and arguments
		for (Constructor ctor : constructors) {
			Class[] parameters = ctor.getParameterTypes();
			if (parameters.length != top - 1) {
				continue;
			}

			boolean okConstructor = true;

			for (int j = 0; j < parameters.length; j++) {
				try {
					objs[j] = compareTypes(L, parameters[j], j + 2);
				} catch (Exception e) {
					Log.d(LuaJavaAPI.class.getSimpleName(), e.getMessage(), e);
					okConstructor = false;
					break;
				}
			}

			if (okConstructor) {
				constructor = ctor;
				break;
			}

		}
	
	    // If method is null means there isn't one receiving the given arguments
	    if (constructor == null)
	    {
	      throw new LuaException("Invalid method call. No such method.");
	    }

		return constructor.newInstance(objs);
    }
  }
  
  /**
   * 如果searchStaticOnly为true，只查找静态变量，否则优先查找实例变量，再查找静态变量
   * @return field
   */
  private static Field findField(Class<?> clazz, String fieldName, boolean searchStaticOnly) {
	  if(clazz == null) {
		  return null;
	  }
	  
	  Field[] fields = clazz.getDeclaredFields();
	  
	  List<Field> staticFields = new ArrayList<Field>(fields.length);
	  List<Field> instanceFields = new ArrayList<Field>(fields.length);
	  
	  for(Field field : fields) {
		  if(Modifier.isStatic(field.getModifiers())) {
			  staticFields.add(field);
		  } else {
			  instanceFields.add(field);
		  }
	  }
	  
	  if(searchStaticOnly) {
		  for(Field field : staticFields) {
			  if(field.getName().equals(fieldName)) {
				  if(!field.isAccessible()) {
					  field.setAccessible(true);
				  }
				  return field;
			  }
		  }
		  return findField(clazz.getSuperclass(), fieldName, true);
	  }
	  
	  for(Field field : instanceFields) {
		  if(field.getName().equals(fieldName)) {
			  if(!field.isAccessible()) {
				  field.setAccessible(true);
			  }
			  return field;
		  }
	  }
	  for(Field field : staticFields) {
		  if(field.getName().equals(fieldName)) {
			  if(!field.isAccessible()) {
				  field.setAccessible(true);
			  }
			  return field;
		  }
	  }
	  
	  return findField(clazz.getSuperclass(), fieldName, false);
  }
  
  private static Field findHashField(Class<?> clazz, String hash) {
	  if(clazz == null) {
		  return null;
	  }
	  
	  for(Field field : clazz.getDeclaredFields()) {
		  if(MethodHashUtils.hashField(field).equals(hash)) {
			  return field;
		  }
	  }
	  return findHashField(clazz.getSuperclass(), hash);
  }

  /**
   * Checks if there is a field on the obj with the given name
   * 
   * @param luaState int that represents the state to be used
   * @param obj object to be inspected
   * @param fieldName name of the field to be inpected
   * @return number of returned objects
   */
  private static int checkField(int luaState, Object obj, String fieldName)
  	throws LuaException
  {
	  if("new".equals(fieldName)) {
		  return 0;
	  }
	  
    LuaState L = LuaStateFactory.getExistingState(luaState);

    synchronized (L)
    {
      Class objClass = obj.getClass();
      
      /*
       * if obj is class, then search static field first, else search the instance field first.
       */
      
      Field field = fieldName.length() != 32 ? null : findHashField(objClass, fieldName);
      if(field == null && Class.class.isInstance(obj)) {
    	  field = findField(objClass, fieldName, true);
      }
      if(field == null) {
    	  field = findField(objClass, fieldName, false);
      }

      if (field == null)
      {
        return 0;
      }

      Object ret = null;
      try
      {
        ret = field.get(obj);
      }
      catch (Exception e)
      {
    	  Log.d("luajava", e.getMessage(), e);
        return 0;
      }

      L.pushObjectValue(ret);

      return 1;
    }
  }

  /**
   * Checks to see if there is a method with the given name.
   * 
   * @param luaState int that represents the state to be used
   * @param obj object to be inspected
   * @param methodName name of the field to be inpected
   * @return number of returned objects
   */
  private static int checkMethod(int luaState, Object obj, String methodName)
  {
    LuaState L = LuaStateFactory.getExistingState(luaState);
    
    if(obj instanceof Class && "new".equals(methodName)) {
    	return 1;
    }

    synchronized (L)
    {
      Class clazz = obj.getClass();

      Method[] methods = clazz.getMethods();

      for (int i = 0; i < methods.length; i++)
      {
        if (methods[i].getName().equals(methodName))
          return 1;
      }
      
      methods = clazz.getDeclaredMethods();
      for(Method method : methods) {
    	  if(method.getName().equals(methodName)) {
    		  if(!method.isAccessible())
    	      {
    	        method.setAccessible(true);
    	      }
    		  return 1;
    	  }
      }

      return 0;
    }
  }

  /**
   * Function that creates an object proxy and pushes it into the stack
   * 
   * @param luaState int that represents the state to be used
   * @param implem interfaces implemented separated by comma (<code>,</code>)
   * @return number of returned objects
   */
  public static int createProxyObject(int luaState, String implem)
    throws LuaException, ClassNotFoundException
  {
    LuaState L = LuaStateFactory.getExistingState(luaState);

    synchronized (L)
    {
    	if (!(L.isTable(2)))
            throw new LuaException(
                "Parameter is not a table. Can't create proxy.");

          LuaObject luaObj = L.getLuaObject(2);

          Object proxy = luaObj.createProxy(implem);
          L.pushJavaObject(proxy);

      return 1;
    }
  }

  private static Object compareTypes(LuaState L, Class parameter, int idx)
    throws LuaException
  {
    boolean okType = true;
    Object obj = null;

    if (L.isBoolean(idx))
    {
      if (parameter.isPrimitive())
      {
        if (parameter != Boolean.TYPE)
        {
          okType = false;
        }
      }
      else if (!parameter.isAssignableFrom(Boolean.class))
      {
        okType = false;
      }
      obj = L.toBoolean(idx);
    }
    else if (L.type(idx) == LuaState.LUA_TSTRING)
    {
    	if(byte[].class == parameter) {
    		obj = L.toByteArray(idx);
    	} else {
    	
	      if (!parameter.isAssignableFrom(String.class))
	      {
	        okType = false;
	      }
	      else
	      {
	        obj = L.toString(idx);
	      }
	      
    	}
    }
    else if (L.isFunction(idx))
    {
      if (!parameter.isAssignableFrom(LuaObject.class))
      {
        okType = false;
      }
      else
      {
        obj = L.getLuaObject(idx);
      }
    }
    else if (L.isTable(idx))
    {
      if (!parameter.isAssignableFrom(LuaObject.class))
      {
        okType = false;
      }
      else
      {
        obj = L.getLuaObject(idx);
      }
    }
    else if (L.type(idx) == LuaState.LUA_TNUMBER)
    {
      Double db = L.toNumber(idx);
      
      obj = LuaState.convertLuaNumber(db, parameter);
      if (obj == null)
      {
        okType = false;
      }
    }
    else if (L.isUserdata(idx))
    {
      if (L.isObject(idx))
      {
        Object userObj = L.getObjectFromUserdata(idx);
        if (!parameter.isAssignableFrom(userObj.getClass()))
        {
          okType = false;
        }
        else
        {
          obj = userObj;
        }
      }
      else
      {
        if (!parameter.isAssignableFrom(LuaObject.class))
        {
          okType = false;
        }
        else
        {
          obj = L.getLuaObject(idx);
        }
      }
    }
    else if (L.isNil(idx))
    {
      obj = null;
    }
    else
    {
      okType = false;
    }

    if (!okType)
    {
      throw new LuaException("Invalid Parameter: class=" + parameter + ", lua_type=" + L.type(idx) + ", idx=" + idx);
    }

    return obj;
  }

}