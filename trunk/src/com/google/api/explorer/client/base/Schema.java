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

import com.google.web.bindery.autobean.shared.AutoBean.PropertyName;

import java.util.Map;

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
    STRING, @PropertyName("array")
    ARRAY, @PropertyName("object")
    OBJECT, @PropertyName("boolean")
    BOOLEAN, @PropertyName("integer")
    INTEGER, @PropertyName("number")
    NUMBER, @PropertyName("any")
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

    /** Type of this property. */
    Type getType();

    /** Default value of this property. */
    String getDefault();
  }
}
