package com.pmease.commons.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

public class BeanUtils {
	
	/**
	 * Check if specified method is a getter method.
	 * <p>
	 * @param method
	 * 			method to be checked
	 * @return
	 * 			whether or not the method represents a getter method
	 */
	public static boolean isGetter(Method method) {
		if ((method.getName().startsWith("get") || method.getName().startsWith("is")) && 
				!Modifier.isStatic(method.getModifiers()) && method.getParameterTypes().length == 0) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Get suffix of accessor method name for specified property. For instance, 
	 * getter suffix for property &quot;value&quot; would be &quot;Value&quot;.
	 * However if the property name starts with underscore, the accessor suffix 
	 * will be the same as property name. 
	 * <p>
	 * @param propertyName
	 * 			name of the property 
	 * @return
	 * 			accessor method name suffix of specified property 
	 */
	public static String getAccessorSuffix(String propertyName) {
		char[] chars = propertyName.toCharArray();
		if (chars[0] != '_') {
			chars[0] = Character.toUpperCase(chars[0]);
			return new String(chars);
		} else {
			return propertyName;
		}
	}
	
	/**
	 * Get property name given its accessor method name suffix. For instance, 
	 * accessor suffix &quot;Value&quot; will return property name of 
	 * &quot;value&quot;. Note that if accessor suffix starts with underscore, 
	 * the property name will be the same as accessor suffix. 
	 * 
	 * @param accessorSuffix name suffix of the getter method of the property
	 * @return property name corresponding to specified accessor suffix
	 */
	private static String getPropertyName(String accessorSuffix) {
		char[] chars = accessorSuffix.toCharArray();
		if (chars[0] != '_') {
			Preconditions.checkArgument(Character.isUpperCase(chars[0]));
			chars[0] = Character.toLowerCase(chars[0]);
			return new String(chars);
		} else {
			return accessorSuffix;
		}
	}
	
	/**
	 * Find all getter methods in specified class and all its super classes.
	 * Note that if a getter method declared in sub class overrides another 
	 * getter method declared in super class, only the getter method in 
	 * sub class will be returned.  
	 *
	 * @param clazz
	 * 			the class to find getter methods. All its super classes will 
	 * 			also be consulted
	 * @return
	 * 			list of found getter methods, with getter methods belonging 
	 * 			to sub classes coming first   
	 */
	public static List<Method> findGetters(Class<?> clazz) {
		Map<String, Method> getters = new LinkedHashMap<String, Method>();
		
		Class<?> current = clazz;
		while (current != null) {
			for (Method method: current.getDeclaredMethods()) {
				if (!method.isSynthetic() && isGetter(method) && !getters.containsKey(method.getName())) {
					getters.put(method.getName(), method);
				}
			}
			current = current.getSuperclass();
		}
		return new ArrayList<Method>(getters.values());
	}

	/**
	 * Find getter method by property name in specified class and super classes.
	 * 
	 * @param clazz
	 * 			class to search getter method. All super classes will also be 
	 * 			searched if not found in current class
	 * @param propertyName
	 * 			name of the property to search getter for
	 * @return
	 * 			getter method
	 * @throws
	 * 			RuntimeException if not found in class hierarchy
	 */
	public static Method getGetter(Class<?> clazz, String propertyName) {
		Method getter = findGetter(clazz, propertyName);
		if (getter == null) {
			throw new GeneralException(String.format("Getter not found (class: %s, property: %s)", clazz.getName(), propertyName));
		}
		return getter;
	}
	
	/**
	 * Find getter method by property name in specified class and super classes.
	 * 
	 * @param clazz
	 * 			class to search getter method. All super classes will also be 
	 * 			searched if not found in current class
	 * @param propertyName
	 * 			name of the property to search getter for
	 * @return
	 * 			getter method, or <tt>null</tt> if not found in class hierarchy
	 */
	public static Method findGetter(Class<?> clazz, String propertyName) {
		String methodSuffix = getAccessorSuffix(propertyName);
		Method getter = ReflectionUtils.findMethod(clazz, "get" + methodSuffix);
		if (getter == null)
			getter = ReflectionUtils.findMethod(clazz, "is" + methodSuffix);
		return getter;
	}

	/**
	 * Get property name associated with the getter method.
	 * 
	 * @param getter
	 * 			getter method to retrieve property name from
	 * @return
	 * 			property name associated with the getter method
	 * @throws
	 * 			RuntimeException if specified method is not a getter method
	 */
	public static String getPropertyName(Method getter) {
		if (getter.getName().startsWith("get")) {
			return getPropertyName(getter.getName().substring(3));
		} else if (getter.getName().startsWith("is")) {
			return getPropertyName(getter.getName().substring(2));
		} else {
			throw new GeneralException(String.format("Not recognized getter method (class: %s, method: %s)", 
					getter.getDeclaringClass().getName(), getter.getName()));
		}
	}
	
	/**
	 * Find corresponding setter method by getter in specified class and super classes.
	 * 
	 * @param clazz
	 * 			class to search setter method. All super classes will also be 
	 * 			searched if not found in current class
	 * @param getter
	 * 			getter method to search setter for
	 * @return
	 * 			setter method, or <tt>null</tt> if not found in class hierarchy
	 */
	public static Method findSetter(Method getter) {
		String propertyName = getPropertyName(getter);
		String setterName = "set" + getAccessorSuffix(propertyName);
		return ReflectionUtils.findMethod(getter.getDeclaringClass(), setterName, getter.getReturnType());
	}
	
	/**
	 * Find corresponding setter method by getter in specified class and super classes.
	 * <p>
	 * @param clazz
	 * 			class to search setter method. All super classes will also be 
	 * 			searched if not found in current class
	 * @param getter
	 * 			getter method to search setter for
	 * @return
	 * 			setter method
	 * @throws
	 * 			RuntimeException if not found in class hierarchy
	 */
	public static Method getSetter(Method getter) {
		Method setter = findSetter(getter);
		if (setter == null) {
			throw new RuntimeException("Can not find corresponding setter for getter '" 
					+ getter.getName() + "'.");
		}
		return setter;
	}
	
}
