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

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Encapsulates one element in an {@link ArraySchemaEditor}.
 * 
 * @author jasonhall@google.com (Jason Hall)
 */
public class ArrayElement extends Composite {

  private static ArrayElementUiBinder uiBinder = GWT.create(ArrayElementUiBinder.class);

  interface ArrayElementUiBinder extends UiBinder<Widget, ArrayElement> {
  }

  @UiField InlineLabel remove;
  @UiField HTMLPanel placeholder;

  public ArrayElement(Widget rendered) {
    initWidget(uiBinder.createAndBindUi(this));
    placeholder.add(rendered);
  }
}
