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

package com.google.api.explorer.client.parameter.schema;

import com.google.api.explorer.client.base.Schema.Property;
import com.google.api.explorer.client.parameter.schema.SchemaForm.SchemaEditor;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link SchemaEditor} for object values. The keys/values of the object will
 * have their own editors which will provide the string value of this editor.
 * 
 * @author jasonhall@google.com (Jason Hall)
 */
class ObjectSchemaEditor extends Composite implements SchemaEditor {

  private static ObjectSchemaEditorUiBinder uiBinder = GWT.create(ObjectSchemaEditorUiBinder.class);

  interface ObjectSchemaEditorUiBinder extends UiBinder<Widget, ObjectSchemaEditor> {
  }

  private static final String ADD_PROPERTY = "-- add a property --";

  private final SchemaForm schemaForm;
  private final Map<String, Property> properties;
  private final Map<String, SchemaEditor> editors = Maps.newHashMap();
  private final List<String> availableKeys = Lists.newArrayList();

  @UiField
  ListBox listBox;
  @UiField
  HTMLPanel panel;

  ObjectSchemaEditor(SchemaForm schemaForm, Map<String, Property> properties) {
    initWidget(uiBinder.createAndBindUi(this));
    this.schemaForm = schemaForm;
    this.properties = properties;
  }

  @Override
  public Widget render(Property ignored) {
    availableKeys.clear();
    availableKeys.addAll(properties.keySet());
    Collections.sort(availableKeys);

    // Iterate over properties in this object inspecting its annotations.
    // Annotations tell us whether the parameter is required, or immutable.
    for (Map.Entry<String, Property> entry : properties.entrySet()) {
      String currentMethodIdentifier = schemaForm.appState.getCurrentMethodIdentifier();
      boolean required = entry.getValue().requiredForMethod(currentMethodIdentifier);
      boolean immutable = !entry.getValue().mutableForMethod(currentMethodIdentifier);

      if (required) {
        // Add all required fields for the selected method to the object form.
        onSelect(entry.getKey());
      }
      // TODO(jasonhall): Check if the property is immutable and remove it from
      // availableKeys, when Discovery contains this information.
    }

    buildListBox();
    listBox.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        onSelect(null);
      }
    });
    listBox.addKeyUpHandler(new KeyUpHandler() {
      @Override
      public void onKeyUp(KeyUpEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
          onSelect(null);
        }
      }
    });

    return this;
  }

  @Override
  public JSONValue getJSONValue() {
    Set<String> elems = Sets.newHashSet();
    JSONObject obj = new JSONObject();

    for (Map.Entry<String, SchemaEditor> entry : editors.entrySet()) {
      obj.put(entry.getKey(), entry.getValue().getJSONValue());
    }
    return obj;
  }

  private void onSelect(String key) {
    // Selecting the first item in the list (a placeholder) has no effect.
    if (listBox.getSelectedIndex() == 0) {
      return;
    }

    String selectedKey = key == null ? listBox.getValue(listBox.getSelectedIndex()) : key;
    Property selectedProperty = properties.get(selectedKey);

    SchemaEditor editor = schemaForm.getSchemaEditorForProperty(selectedProperty);
    final ObjectElement row = new ObjectElement(selectedKey, editor, selectedProperty);
    panel.add(row);
    editors.put(selectedKey, editor);

    // Remove the selected key from the listbox.
    availableKeys.remove(selectedKey);
    for (int i = 1; i < listBox.getItemCount(); i++) {
      if (listBox.getItemText(i).equals(selectedKey)) {
        listBox.removeItem(i);
        break;
      }
    }

    // If there aren't any keys left, hide the listbox.
    if (availableKeys.isEmpty()) {
      listBox.setVisible(false);
    }

    // If the key was explicitly set that means it is a required key.
    if (key != null) {
      // Hide the remove button for this row.
      row.remove.setVisible(false);
    } else {
      // When a row is removed, re-add its key to the list of available keys.
      row.remove.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          panel.remove(row);
          editors.remove(row.key);

          availableKeys.add(row.key);
          Collections.sort(availableKeys);

          buildListBox();
        }
      });
    }
  }

  /**
   * Resets the listbox to contain all keys in availableKeys, and the
   * placeholder, and sets the listbox visible.
   */
  private void buildListBox() {
    listBox.clear();
    listBox.addItem(ADD_PROPERTY);
    for (String key : availableKeys) {
      listBox.addItem(key);
    }
    listBox.setVisible(true);
  }
}
