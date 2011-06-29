/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.api.explorer.client.base;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBean.PropertyName;

import java.util.Map;
import java.util.Set;

/**
 * Represents a description of an request or response, including its type, and,
 * if it's an object, its fields.
 * 
 * @author jasonhall@google.com (Jason Hall)
 */
public interface Schema {

  /** Possible types. */
  public enum Type {
    @PropertyName("string")
    STRING,

    @PropertyName("array")
    ARRAY,

    @PropertyName("object")
    OBJECT,

    @PropertyName("boolean")
    BOOLEAN,

    @PropertyName("integer")
    INTEGER,

    @PropertyName("number")
    NUMBER,

    @PropertyName("any")
    ANY;
  }

  /** Uniquely-identifying name of this schema. */
  String getId();

  /** Type of this data. */
  Type getType();

  /** All properties of this object, or {@code null} if this is not an object. */
  Map<String, Property> getProperties();

  /** Definition of a property of an object. */
  public interface Property {

    /**
     * Reference to an object defining this property, or {@code null} if this is
     * not an otherwise-referenced object.
     */
    @PropertyName("$ref")
    String getRef();

    /** Default value of this property. */
    String getDefault();

    /** Type of this property. */
    Type getType();

    /**
     * If this is an object, a mapping of key names to property definitions of
     * values in this object. Otherwise {@code null}.
     */
    Map<String, Property> getProperties();

    /**
     * If this is an array, Property definition of items in this array.
     * Otherwise {@code null}.
     */
    Property getItems();

    /** Description of this property, {@code null} if none is given. */
    String getDescription();

    /**
     * Mapping of an annotation and a set of method identifiers for which that
     * annotation applies to this property.
     */
    Map<String, Set<String>> getAnnotations();

    /**
     * Returns true if this property is required for the method identified by
     * the given method identifier.
     */
    boolean requiredForMethod(String methodIdentifier);

    /**
     * Returns true if this property is mutable (or required) for the method
     * identified by the given method identifier.
     */
    boolean mutableForMethod(String methodIdentifier);

    /**
     * Wrapper class used by the AutoBeanFactory to provide the implementation
     * of some methods.
     * 
     * <p>
     * All of these methods map to a method in {@link Property} which delegates
     * to the method in this class to provide its implementation.
     * </p>
     */
    static class PropertyWrapper {
      private static final String REQUIRED = "required";
      private static final String MUTABLE = "mutable";

      private static boolean hasAnnotationForMethod(Property property, String annotation,
          String methodIdentifier) {
        return property.getAnnotations() != null
            && property.getAnnotations().containsKey(annotation)
            && property.getAnnotations().get(annotation).contains(methodIdentifier);
      }

      /**
       * Returns true if this property is required for the method identified by
       * the given method identifier.
       */
      public static boolean requiredForMethod(AutoBean<Property> instance,
          String methodIdentifier) {
        return hasAnnotationForMethod(instance.as(), REQUIRED, methodIdentifier);
      }

      /**
       * Returns true if this property is mutable (or required) for the method
       * identified by the given method identifier.
       */
      public static boolean mutableForMethod(AutoBean<Property> instance,
          String methodIdentifier) {
        // Required properties will not be explicitly marked mutable, since
        // mutablility is assumed for required properties.
        return requiredForMethod(instance, methodIdentifier)
            || hasAnnotationForMethod(instance.as(), MUTABLE, methodIdentifier);
      }
    }
  }
}
