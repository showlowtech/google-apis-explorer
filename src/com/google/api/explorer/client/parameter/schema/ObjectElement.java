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
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Encapsulates the key/value of one key/value pair in an
 * {@link ObjectSchemaEditor}.
 * 
 * @author jasonhall@google.com (Jason Hall)
 */
public class ObjectElement extends Composite {

  private static RowUiBinder uiBinder = GWT.create(RowUiBinder.class);

  interface RowUiBinder extends UiBinder<Widget, ObjectElement> {
  }

  @UiField InlineLabel label;
  @UiField HTMLPanel placeholder;
  @UiField Image remove;

  final String key;

  public ObjectElement(String key, SchemaEditor editor, Property property) {
    initWidget(uiBinder.createAndBindUi(this));
    this.key = key;

    label.setText("\"" + key + "\"");
    label.setTitle(property.getDescription());
    placeholder.add(editor.render(property));
  }
}
