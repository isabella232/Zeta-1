package com.ebay.dss.zds.common;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BeanUtils {

    public static String[] getNullPropertyNames (Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<String>();
        for(java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) emptyNames.add(pd.getName());
        }

        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

    public static void copyPropertiesIgnoreNull(Object src, Object target) {
        org.springframework.beans.BeanUtils
                .copyProperties(src, target, getNullPropertyNames(src));
    }

    public static void copyProperties(Object src, Object target) {
        org.springframework.beans.BeanUtils
                .copyProperties(src, target);
    }

    public static <T> void merge(T origin, T updated, List<String> ACCESSIBLE_FIELDS) {

        Field[] allFields = origin.getClass().getDeclaredFields();
        for (Field field : allFields) {
            if (!ACCESSIBLE_FIELDS.contains(field.getName())) {
                continue;
            }
            if (!field.isAccessible() && Modifier.isPrivate(field.getModifiers())) {
                field.setAccessible(true);
            }
            try {
                if (field.get(updated) != null) {
                    field.set(origin, field.get(updated));
                }
            } catch (IllegalAccessException e) {
                /* Silently fail */
            }
        }
    }
}
