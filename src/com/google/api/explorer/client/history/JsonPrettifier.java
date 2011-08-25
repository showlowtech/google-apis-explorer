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

package com.google.api.explorer.client.history;

import com.google.api.explorer.client.AppState;
import com.google.api.explorer.client.Resources.Css;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiMethod.HttpMethod;
import com.google.api.explorer.client.base.Config;
import com.google.api.explorer.client.base.dynamicjso.DynamicJsArray;
import com.google.api.explorer.client.base.dynamicjso.DynamicJso;
import com.google.api.explorer.client.base.dynamicjso.JsType;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineHyperlink;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

/**
 * A simple syntax highlighter for JSON data.
 *
 */
public class JsonPrettifier {
  private static final String PLACEHOLDER_TEXT = "...";
  private static final String SEPARATOR_TEXT = ",";
  private static final String OPEN_IN_NEW_WINDOW = "_blank";

  public static AppState appState;
  public static Css style;

  private static class Collapser implements ClickHandler {
    private final Widget toHide;
    private final Widget placeHolder;
    private final Widget clicker;

    public Collapser(Widget toHide, Widget placeHolder, Widget clicker) {
      this.toHide = toHide;
      this.placeHolder = placeHolder;
      this.clicker = clicker;
    }

    @Override
    public void onClick(ClickEvent arg0) {
      boolean makeVisible = !toHide.isVisible();
      decorateCollapserControl(clicker, makeVisible);
      toHide.setVisible(makeVisible);
      placeHolder.setVisible(!makeVisible);
    }

    public static void decorateCollapserControl(Widget collapser, boolean visible) {
      if (visible) {
        collapser.addStyleName(style.jsonExpanded());
        collapser.removeStyleName(style.jsonCollapsed());
      } else {
        collapser.addStyleName(style.jsonCollapsed());
        collapser.removeStyleName(style.jsonExpanded());
      }
    }
  }

  /**
   * This abstraction of an array creates formatted widgets from all children.
   */
  private static class JsArrayIterable implements Iterable<Widget> {
    private final DynamicJsArray backingObj;
    private final int depth;

    public JsArrayIterable(DynamicJsArray array, int depth) {
      this.backingObj = array;
      this.depth = depth;
    }

    @Override
    public Iterator<Widget> iterator() {
      return new Iterator<Widget>() {
        private int nextOffset = 0;

        @Override
        public boolean hasNext() {
          return nextOffset < backingObj.length();
        }

        @Override
        public Widget next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          Widget next = formatArrayValue(
              backingObj, nextOffset, depth, nextOffset + 1 < backingObj.length());
          nextOffset++;
          return next;
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }
  }

  /**
   * This abstraction of an object creates formatted widgets from all children.
   */
  private static class JsObjectIterable implements Iterable<Widget> {
    private final DynamicJso backingObj;
    private final int depth;

    public JsObjectIterable(DynamicJso obj, int depth) {
      this.backingObj = obj;
      this.depth = depth;
    }

    @Override
    public Iterator<Widget> iterator() {
      return new Iterator<Widget>() {
         int nextOffset = 0;

        @Override
        public boolean hasNext() {
          return nextOffset < backingObj.keys().length();
        }

        @Override
        public Widget next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          Widget next =
              formatValue(backingObj, backingObj.keys().get(nextOffset), depth,
                  nextOffset + 1 < backingObj.keys().length());
          nextOffset++;
          return next;
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }
  }

  /**
   * Entry point for the formatter.
   *
   * @param destination Destination GWT object where the results will be placed
   * @param jsonString String to format
   */
  public static void prettify(Panel destination, String jsonString) {
    // Don't bother syntax highlighting empty text.
    boolean empty = Strings.isNullOrEmpty(jsonString);
    destination.setVisible(!empty);
    if (empty) {
      return;
    }

    if (!GWT.isScript()) {
      // Syntax highlighting is *very* slow in Development Mode (~30s for large
      // responses), but very fast when compiled and run as JS (~30ms). For the
      // sake of my sanity, syntax highlighting is disabled in Development
      destination.add(new InlineLabel(jsonString));
    } else {

      try {
        DynamicJso root = JsonUtils.<DynamicJso>safeEval(jsonString);
        destination.add(formatGroup(new JsObjectIterable(root, 1), "", 0, "{", "}", false));
      } catch (Exception e) {
        // As a fallback in case anything goes wrong, just set the inner text
        // without any highlighting.
        destination.add(new InlineLabel(jsonString));
      }
    }
  }

  /**
   * Iterate through an object or array adding the widgets generated for all children
   */
  private static FlowPanel formatGroup(Iterable<Widget> objIterable,
      String title,
      int depth,
      String openGroup,
      String closeGroup,
      boolean hasSeparator) {

    FlowPanel object = new FlowPanel();

    FlowPanel titlePanel = new FlowPanel();
    Label paddingSpaces = new InlineLabel(indentation(depth));
    titlePanel.add(paddingSpaces);

    Label titleLabel = new InlineLabel(title + openGroup);
    titleLabel.addStyleName(style.jsonKey());
    Collapser.decorateCollapserControl(titleLabel, true);
    titlePanel.add(titleLabel);

    object.add(titlePanel);

    FlowPanel objectContents = new FlowPanel();
    for (Widget child : objIterable) {
      objectContents.add(child);
    }
    object.add(objectContents);

    InlineLabel placeholder = new InlineLabel(indentation(depth + 1) + PLACEHOLDER_TEXT);
    ClickHandler collapsingHandler = new Collapser(objectContents, placeholder, titleLabel);
    placeholder.setVisible(false);
    placeholder.addClickHandler(collapsingHandler);
    object.add(placeholder);

    titleLabel.addClickHandler(collapsingHandler);

    StringBuilder closingLabelText = new StringBuilder(indentation(depth)).append(closeGroup);
    if (hasSeparator) {
      closingLabelText.append(SEPARATOR_TEXT);
    }

    object.add(new Label(closingLabelText.toString()));

    return object;
  }

  private static Widget formatArrayValue(
      DynamicJsArray obj, int index, int depth, boolean hasSeparator) {
    JsType type = obj.typeofIndex(index);
    if (type == null) {
      return simpleInline("", "null", style.jsonNull(), depth, hasSeparator);
    }
    String title = "";
    switch (type) {
      case NUMBER:
        return simpleInline(
            title, String.valueOf(obj.getDouble(index)), style.jsonNumber(), depth, hasSeparator);
      case INTEGER:
        return simpleInline(
            title, String.valueOf(obj.getInteger(index)), style.jsonNumber(), depth, hasSeparator);
      case BOOLEAN:
        return simpleInline(
            title, String.valueOf(obj.getBoolean(index)), style.jsonBoolean(), depth, hasSeparator);
      case STRING:
        return inlineWidget(title, formatString(obj.getString(index)), depth, hasSeparator);
      case ARRAY:
        return formatGroup(new JsArrayIterable(obj.<DynamicJsArray>get(index), depth + 1), title,
            depth, "[", "]", hasSeparator);
      case OBJECT:
        return formatGroup(new JsObjectIterable(obj.<DynamicJso>get(index), depth + 1), title,
            depth, "{", "}", hasSeparator);
    }
    return new FlowPanel();
  }

  private static Widget formatValue(DynamicJso obj, String key, int depth, boolean hasSeparator) {
    JsType type = obj.typeofKey(key);
    if (type == null) {
      return simpleInline(titleString(key), "null", style.jsonNull(), depth, hasSeparator);
    }
    String title = titleString(key);
    switch (type) {
      case NUMBER:
        return simpleInline(
            title, String.valueOf(obj.getDouble(key)), style.jsonNumber(), depth, hasSeparator);
      case INTEGER:
        return simpleInline(
            title, String.valueOf(obj.getInteger(key)), style.jsonNumber(), depth, hasSeparator);
      case BOOLEAN:
        return simpleInline(
            title, String.valueOf(obj.getBoolean(key)), style.jsonBoolean(), depth, hasSeparator);
      case STRING:
        return inlineWidget(title, formatString(obj.getString(key)), depth, hasSeparator);
      case ARRAY:
        return formatGroup(new JsArrayIterable(obj.<DynamicJsArray>get(key), depth + 1), title,
            depth, "[", "]", hasSeparator);
      case OBJECT:
        return formatGroup(new JsObjectIterable(obj.<DynamicJso>get(key), depth + 1), title, depth,
            "{", "}", hasSeparator);
    }
    return new FlowPanel();
  }

  private static Widget simpleInline(
      String title, String inlineText, String style, int depth, boolean hasSeparator) {
    Widget valueLabel = new InlineLabel(inlineText);
    valueLabel.addStyleName(style);
    return inlineWidget(title, Lists.newArrayList(valueLabel), depth, hasSeparator);
  }

  private static Widget inlineWidget(
      String title, List<Widget> inlineWidgets, int depth, boolean hasSeparator) {

    FlowPanel inlinePanel = new FlowPanel();

    StringBuilder keyText = new StringBuilder(indentation(depth)).append(title);
    InlineLabel keyLabel = new InlineLabel(keyText.toString());
    keyLabel.addStyleName(style.jsonKey());
    inlinePanel.add(keyLabel);

    for (Widget child : inlineWidgets) {
      inlinePanel.add(child);
    }

    if (hasSeparator) {
      inlinePanel.add(new InlineLabel(SEPARATOR_TEXT));
    }

    return inlinePanel;
  }

  private static String indentation(int depth) {
    return Strings.repeat(" ", depth);
  }

  private static List<Widget> formatString(String rawText) {
    if (isLink(rawText)) {
      List<Widget> response = Lists.newArrayList();
      response.add(new InlineLabel("\""));

      boolean createdExplorerLink = false;
      try {
        Entry<String, ApiMethod> method = getMethodForUrl(rawText);
        if (method != null) {
          String explorerLink = createExplorerLink(rawText, method);
          InlineHyperlink linkObject = new InlineHyperlink(rawText, explorerLink);
          linkObject.addStyleName(style.jsonStringExplorerLink());
          response.add(linkObject);
          createdExplorerLink = true;
        }
      } catch (IndexOutOfBoundsException e) {
        // Intentionally blank - this will only happen when iterating the method
        // url template in parallel with the url components and you run out of
        // components
      }

      if (!createdExplorerLink) {
        Anchor linkObject = new Anchor(rawText, rawText, OPEN_IN_NEW_WINDOW);
        linkObject.addStyleName(style.jsonStringLink());
        response.add(linkObject);
      }

      response.add(new InlineLabel("\""));
      return response;
    } else {
      Widget stringText = new InlineLabel("\"" + rawText + "\"");
      stringText.addStyleName(style.jsonString());
      return Lists.newArrayList(stringText);
    }
  }

  private static String titleString(String name) {
    return "\"" + name + "\": ";
  }

  /**
   * Attempts to identify an {@link ApiMethod} corresponding to the given url.
   * If one is found, a {@link Map.Entry} will be returned where the key is the
   * name of the method, and the value is the {@link ApiMethod} itself. If no
   * method is found, this will return {@code null}.
   */
  static Map.Entry<String, ApiMethod> getMethodForUrl(String url) {
    String apiLinkPrefix = Config.getBaseUrl() + appState.getCurrentService().getBasePath();
    if (!url.startsWith(apiLinkPrefix)) {
      return null;
    }

    // Only check GET methods since those are the only ones that can be returned
    // in the response.
    Iterable<Map.Entry<String, ApiMethod>> getMethods =
        Iterables.filter(appState.getCurrentService().allMethods().entrySet(),
            new Predicate<Map.Entry<String, ApiMethod>>() {
              @Override
              public boolean apply(Entry<String, ApiMethod> input) {
                return input.getValue().getHttpMethod() == HttpMethod.GET;
              }
            });

    int paramIndex = url.indexOf("?");
    String path = url.substring(0, paramIndex > 0 ? paramIndex : url.length());
    for (Map.Entry<String, ApiMethod> entry : getMethods) {
      // Try to match the request URL with its method by comparing it to the
      // method's rest base path URI template. To do this we have to remove the
      // {...} placeholders.
      String regex =
          apiLinkPrefix + entry.getValue().getPath().replaceAll("\\{[^\\/]+\\}", "[^\\/]+");
      if (path.matches(regex)) {
        return entry;
      }
    }
    return null;
  }

  /**
   * Creates an Explorer link token (e.g.,
   * #_s=<service>&_v=<version>&_m=<method>) corresponding to the given request
   * URL, given the method name and method definition returned by
   * {@link #getMethodForUrl(String)}.
   */
  static String createExplorerLink(String url, Entry<String, ApiMethod> entry) {
    String path = url.substring(url.indexOf('?') + 1);
    StringBuilder tokenBuilder = new StringBuilder()
        .append("#_s=")
        .append(appState.getCurrentService().getName())
        .append("&_v=")
        .append(appState.getCurrentService().getVersion())
        .append("&_m=")
        .append(entry.getKey())
        .append("&")
        .append(path);

    String pathTemplate = entry.getValue().getPath();
    if (pathTemplate.contains("{")) {
      String urlPath = url.replaceFirst(
          Config.getBaseUrl() + appState.getCurrentService().getBasePath(), "");
      if (urlPath.contains("?")) {
        urlPath = urlPath.substring(0, urlPath.indexOf('?'));
      }
      String[] templateSections = pathTemplate.split("/");
      String[] urlSections = urlPath.split("/");
      for (int i = 0; i < templateSections.length; i++) {
        if (templateSections[i].contains("{")) {
          String paramName = templateSections[i].substring(1, templateSections[i].length() - 1);
          tokenBuilder.append("&").append(paramName).append("=").append(urlSections[i]);
        }
      }
    }
    return tokenBuilder.toString();
  }

  private static boolean isLink(String value) {
    return (value.startsWith("http://") || value.startsWith("https://")) && !value.contains("\n")
        && !value.contains("\t");
  }
}
