/*
 * Copyright (C) 2010 Google Inc.
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

package com.google.api.explorer.client.embedded;

import com.google.api.explorer.client.base.ApiRequest;
import com.google.api.explorer.client.base.ApiResponse;
import com.google.api.explorer.client.history.HistoryItem;
import com.google.api.explorer.client.history.PrettyDate;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Widget;

/**
 * Embedded version of a history item.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class EmbeddedHistoryItem extends HistoryItem {

  private static EmbeddedHistoryItemUiBinder uiBinder = GWT
      .create(EmbeddedHistoryItemUiBinder.class);

  @UiTemplate("EmbeddedHistoryItem.ui.xml")
  interface EmbeddedHistoryItemUiBinder extends UiBinder<Widget, EmbeddedHistoryItem> {
  }

  EmbeddedHistoryItem(String methodIdentifier, long timeMillis, ApiRequest request,
      ApiResponse response) {
    super(methodIdentifier, timeMillis, request, response);

    title.setInnerText(methodIdentifier);
    PrettyDate.stopMakingPretty(title);
  }

  @Override
  protected void initWidget() {
    initWidget(uiBinder.createAndBindUi(this));
  }

  @Override
  public void expandCollapse(ClickEvent event) {
    // Do nothing -- embedded history items don't expand/collapse
  }
}
