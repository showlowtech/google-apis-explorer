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

import com.google.api.explorer.client.AppState;
import com.google.api.explorer.client.base.Schema;
import com.google.api.explorer.client.base.Schema.Property;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.SimpleCheckBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * UI for constructing a request body based on the schema of the expected
 * request.
 * 
 * @author jasonhall@google.com (Jason Hall)
 */
public class SchemaForm extends Composite {

  private static SchemaFormUiBinder uiBinder = GWT.create(SchemaFormUiBinder.class);

  interface SchemaFormUiBinder extends UiBinder<Widget, SchemaForm> {
  }

  @UiField HTMLPanel root;

  final AppState appState;
  private ObjectSchemaEditor editor;

  public SchemaForm(AppState appState) {
    this.appState = appState;
    initWidget(uiBinder.createAndBindUi(this));
  }

  /**
   * Returns the JSON string value of the current state of the object shown in
   * this form.
   */
  public String getStringValue() {
    if (editor == null) {
      return "";
    }

    String jsonValue = editor.getJSONValue().toString();
    return jsonValue.equals("{}") ? "" : jsonValue;
  }

  /** Sets the {@link Schema} to be displayed in this form. */
  public void setSchema(Schema schema) {
    root.clear();

    editor = new ObjectSchemaEditor(this, schema.getProperties());
    root.add(editor.render((Property) null));
  }

  /** Returns the SchemaEditor for the given Property value. */
  SchemaEditor getSchemaEditorForProperty(Property property) {
    if (property.getRef() != null) {
      // Properties of this object are defined elsewhere.
      Schema sch = appState.getCurrentService().getSchemas().get(property.getRef());
      return new ObjectSchemaEditor(this, sch.getProperties());
    }

    if (property.getType() != null) {
      switch (property.getType()) {
        case OBJECT:
          return new ObjectSchemaEditor(this, property.getProperties());
        case ARRAY:
          return new ArraySchemaEditor(this, property.getItems());
        case BOOLEAN:
          return new BooleanSchemaEditor();
        case INTEGER:
        case NUMBER:
          return new NumberSchemaEditor();
        case ANY:
        case STRING:
        default:
          return new StringSchemaEditor();
      }
    }
    return new StringSchemaEditor();
  }

  /** Base interface for all schema-based editors. */
  interface SchemaEditor {
    /** Returns a widget displaying the UI for the user to fill in. */
    Widget render(Property property);

    /**
     * Returns the JSON value of the single property defined displayed by this
     * editor.
     */
    JSONValue getJSONValue();
  }

  /** Editor for string values. */
  static class StringSchemaEditor implements SchemaEditor {
    private TextArea textarea;

    @Override
    public Widget render(Property property) {
      HTMLPanel panel = new HTMLPanel("");
      panel.getElement().getStyle().setDisplay(Display.INLINE);

      panel.add(new InlineLabel("\""));
      textarea = new TextArea();
      panel.add(textarea);
      panel.add(new InlineLabel("\""));

      if (property.getDefault() != null) {
        textarea.setText(property.getDefault());
      }

      return panel;
    }

    @Override
    public JSONValue getJSONValue() {
      return new JSONString(textarea.getValue());
    }
  }

  /** Editor for numerical values. */
  static class NumberSchemaEditor implements SchemaEditor {
    private TextBox textbox;

    @Override
    public Widget render(Property property) {
      textbox = new TextBox();

      if (property.getDefault() != null) {
        textbox.setValue(property.getDefault());
      }
      return textbox;
    }

    @Override
    public JSONValue getJSONValue() {
      // Try to parse the value as a number.
      double val;
      try {
        val = Double.valueOf(textbox.getValue());
      } catch (NumberFormatException nfe) {
        // If the value is not a number, pass it as a string.
        return new JSONString(textbox.getValue());
      }

      return new JSONNumber(val);
    }
  }

  /** Editor for boolean values. */
  static class BooleanSchemaEditor implements SchemaEditor {
    private SimpleCheckBox checkbox;

    @Override
    public Widget render(Property property) {
      checkbox = new SimpleCheckBox();

      // Set it to true if that is the default.
      checkbox.setValue(property.getDefault() != null && property.getDefault().equals("true"));
      return checkbox;
    }

    @Override
    public JSONValue getJSONValue() {
      return JSONBoolean.getInstance(checkbox.getValue());
    }
  }
}
