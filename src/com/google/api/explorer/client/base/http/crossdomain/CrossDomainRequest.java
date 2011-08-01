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

package com.google.api.explorer.client.base.http.crossdomain;

import com.google.api.explorer.client.base.ApiRequest;
import com.google.api.explorer.client.base.ApiResponse;
import com.google.api.explorer.client.base.Config;
import com.google.api.explorer.client.base.dynamicjso.DynamicJso;
import com.google.api.explorer.client.base.http.HttpException;
import com.google.api.explorer.client.base.http.TimeoutException;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.Map;

/**
 * Returned by requests made by a {@link CrossDomainRequestBuilder}.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public final class CrossDomainRequest {

  private final AsyncCallback<ApiResponse> callback;

  private Timer timer;
  private boolean canceled = false;

  CrossDomainRequest(final AsyncCallback<ApiResponse> callback, int timeoutMillis) {
    this.callback = callback;

    if (timeoutMillis > 0) {
      this.timer = new Timer() {
        @Override
        public void run() {
          if (callback != null) {
            CrossDomainRequest.this.cancel();
            callback.onFailure(new TimeoutException());
          }
        }
      };
      timer.schedule(timeoutMillis);
    }
  }

  native void sendRequest(JavaScriptObject requestObj) /*-{
    var self = this;
    var callback = $entry(function(response) {
      self.
      @com.google.api.explorer.client.base.http.crossdomain.CrossDomainRequest::onresponse(Lcom/google/gwt/core/client/JavaScriptObject;)
      (response);
    });

    $wnd.googleapis.newHttpRequest(requestObj).execute(callback);
  }-*/;

  @SuppressWarnings("unused") // Used in JSNI
  private void onresponse(JavaScriptObject response) {
    if (callback != null && !canceled) {
      handleResponse(response.<DynamicJso>cast());
    }
  }

  public void cancel() {
    if (timer != null) {
      timer.cancel();
    }
    canceled = true;
  }

  static JavaScriptObject convertRequest(ApiRequest request) {
    DynamicJso headers = DynamicJso.createObject().cast();
    for (Map.Entry<String, String> entry : request.headers.entrySet()) {
      headers.set(entry.getKey(), entry.getValue());
    }

    // If the base URL contains a path segment, prepend that to the request URL
    // (the JS client ignores the path)
    // TODO(jasonhall): When the JS client fixes this bug, use a version with
    // the fix and don't handle it ourselves.
    String baseUrl = Config.getBaseUrl();
    int pathStart = baseUrl.indexOf('/', "https://".length());
    String path = pathStart == -1 ? "" : baseUrl.substring(pathStart);

    return DynamicJso
        .createObject()
        .<DynamicJso>cast()
        .set("url", path + request.getRequestPath())
        .set("headers", headers)
        .set("body", request.body)
        .set("httpMethod", request.httpMethod.name());
  }

  void handleResponse(DynamicJso response) {
    try {
      callback.onSuccess(ApiResponse.fromData(response));
    } catch (JavaScriptException e) {
      callback.onFailure(new HttpException("Unknown error"));
    }
  }
}
