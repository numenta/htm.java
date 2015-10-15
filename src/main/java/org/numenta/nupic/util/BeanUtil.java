/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero Public License for more details.
 *
 * You should have received a copy of the GNU Affero Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */

package org.numenta.nupic.util;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *Singleton helper for reading/writing beans properties
 *@author Kirill Solovyev
 */
public class BeanUtil {
  //TODO introduce proper log in future
  //private static final Log LOG = LogFactory.getLog(BeanUtil.class);
  private final Map<Class<?>, PropertyInfo[]> properties = new ConcurrentHashMap<Class<?>, PropertyInfo[]>();
  private static final PropertyDescriptor[] EMPTY_PROPERTY_DESCRIPTOR = new PropertyDescriptor[0];
  private static BeanUtil INSTANCE = new BeanUtil();

  public static BeanUtil getInstance() {
    return INSTANCE;
  }

  private BeanUtil() {
  }

  /**
  * Write value to bean's property
  * @param bean
  * @param name
  * @param value
  * @return
  */
  public boolean setSimpleProperty(Object bean, String name, Object value) {
    PropertyInfo pi = getPropertyInfo(bean, name);
    if (pi != null) {
      setSimpleProperty(bean, pi, value);
      return true;
    } else {
      return false;
    }
  }

  public void setSimplePropertyRequired(Object bean, String name, Object value) {
    setSimpleProperty(bean, getPropertyInfoRequired(bean, name), value);
  }

    /**
     * Return bean's property value
     * @param bean
     * @param name
     * @return
     */
  public Object getSimpleProperty(Object bean, String name) {
    return getSimpleProperty(bean, getPropertyInfo(bean, name));
  }


  private Object getSimpleProperty(Object bean, PropertyInfo info) {
    if (info.getReadMethod() == null) {
      throw new IllegalArgumentException("Property '" + info.name + "' of bean " + bean.getClass().getName() +
                                         " does not have getter method");
    }
    return invokeMethod(info.getReadMethod(), bean);
  }

  private void setSimpleProperty(Object bean, PropertyInfo info, Object value) {
    if (info.getWriteMethod() == null) {
      throw new IllegalArgumentException("Property '" + info.name + "' of bean " + bean.getClass().getName() +
                                         " does not have setter method");
    }
    invokeMethod(info.getWriteMethod(), bean, value);
  }

  private Object invokeMethod(Method m, Object instance, Object... args) {
    if (instance == null) {
      throw new IllegalArgumentException("Can not invole Method '" + m + "' on null instance");
    }
    try {
      return m.invoke(instance, args);
    } catch (IllegalArgumentException e) {
      final String msg = "Cannot invoke " + m.getDeclaringClass().getName() + "." + m.getName() + " - " + e.getMessage();
      //LOG.error(msg, e);
      throw new IllegalArgumentException(msg, e);
    } catch (IllegalAccessException e) {
      final String msg = "Cannot invoke " + m.getDeclaringClass().getName() + "." + m.getName() + " - " + e.getMessage();
      //LOG.error(msg, e);
      throw new RuntimeException(msg, e);
    } catch (InvocationTargetException e) {
      Throwable te = e.getTargetException() == null ? e : e.getTargetException();
      final String msg = "Error invoking " + m.getDeclaringClass().getName() + "." + m.getName() + " - " + te.getMessage();
      //LOG.error(msg, e);
      throw new RuntimeException(msg, te);
    }
  }

  public PropertyInfo getPropertyInfo(Object bean, String name) {
    if (bean == null) {
      throw new IllegalArgumentException("Bean can not be null");
    }
    return getPropertyInfo(bean.getClass(), name);
  }

  public PropertyInfo getPropertyInfoRequired(Object bean, String name) {
    PropertyInfo result = getPropertyInfo(bean, name);
    if (result == null) {
      throw new IllegalArgumentException(
              "Bean " + bean.getClass().getName() + " does not have property '" + name + "'");
    }
    return result;
  }

  public PropertyInfo getPropertyInfo(Class<?> beanClass, String name) {
    if (name == null) {
      throw new IllegalArgumentException("Property name is required and can not be null");
    }
    PropertyInfo infos[] = getPropertiesInfoForBean(beanClass);
    for (PropertyInfo info : infos) {
      if (name.equals(info.getName())) {
        return info;
      }
    }
    return null;
  }


  public PropertyInfo[] getPropertiesInfoForBean(Class<?> beanClass) {
    if (beanClass == null) {
      throw new IllegalArgumentException("Bean class is required and can not be null");
    }

    PropertyInfo infos[] = properties.get(beanClass);
    if (infos != null) {
      return infos;
    }

    PropertyDescriptor descriptors[];
    try {
      descriptors = Introspector.getBeanInfo(beanClass).getPropertyDescriptors();
      if (descriptors == null) {
        descriptors = EMPTY_PROPERTY_DESCRIPTOR;
      }
    } catch (IntrospectionException e) {
      descriptors = EMPTY_PROPERTY_DESCRIPTOR;
    }
    infos = new PropertyInfo[descriptors.length];
    for (int i = 0; i < descriptors.length; i++) {
      infos[i] = createPropertyInfo(beanClass, descriptors[i]);
    }
    properties.put(beanClass, infos);
    return infos;
  }

  private PropertyInfo createPropertyInfo(Class<?> beanClass, PropertyDescriptor d) {
    return new PropertyInfo(beanClass, d.getName(), d.getPropertyType(), d.getReadMethod(), d.getWriteMethod());
  }

  @SuppressWarnings("unused")
  private PropertyInfo createPropertyInfo(Class<?> beanClass, String propertyName, Class<?> propertyType) {
    return new PropertyInfo(beanClass, propertyName, propertyType, null, null);
  }

  public static class PropertyInfo {
    private final Class<?> beanClass;
    private final String name;
    private final Class<?> type;
    private final Method readMethod;
    private final Method writeMethod;

    PropertyInfo(Class<?> beanClass, String name, Class<?> type, Method readMethod, Method writeMethod) {
      this.beanClass = beanClass;
      this.name = name;
      this.type = type;
      this.readMethod = readMethod;
      this.writeMethod = writeMethod;
    }

    public Class<?> getBeanClass() {
      return beanClass;
    }

    public String getName() {
      return name;
    }

    public Class<?> getType() {
      return type;
    }

    public Method getReadMethod() {
      return readMethod;
    }

    public Method getWriteMethod() {
      return writeMethod;
    }


    public String toString() {
      return "PropertyInfo{" +
             "beanClass=" + beanClass +
             ", name='" + name + "'" +
             ", type=" + type +
             ", readMethod=" + readMethod +
             ", writeMethod=" + writeMethod +
             "}";
    }
  }


}
