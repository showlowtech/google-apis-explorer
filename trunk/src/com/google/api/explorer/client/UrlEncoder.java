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

package com.google.api.explorer.client;

import com.google.common.base.Join;
import com.google.common.collect.Lists;
import com.google.gwt.http.client.URL;

import java.util.List;

/**
 * Simple URL encoding interface that allows the URL encoding to be pluggable (for testing).
 *
 */
public interface UrlEncoder {
  /**
   * Returns a string where all characters that are not valid for a URL
   * component have been escaped. The escaping of a character is done by
   * converting it into its UTF-8 encoding and then encoding each of the
   * resulting bytes as a %xx hexadecimal escape sequence.
   *
   * @param decodedURLComponent URL query string to encode.
   * @return An escaped URL string.
   */
  String encodeQueryString(String decodedURLComponent);

  /**
   * Returns a string where all characters that are not valid for a URL
   * component have been escaped. The escaping of a character is done by
   * converting it into its UTF-8 encoding and then encoding each of the
   * resulting bytes as a %xx hexadecimal escape sequence.
   *
   * @param decodedURLComponent URL query string to encode.
   * @return An escaped URL string.
   */
  String encodePathSegment(String decodedURLComponent);

  /**
   * Default implementation that delegates to the GWT URL encoder.
   */
  static final UrlEncoder DEFAULT = new UrlEncoder() {
    // TODO(user) Nasty hack ahoy! remove the exception for buzz and the
    // splitting, joining stuff that happens below when Buzz can accept a
    // percent encoded "@" or we no longer need to support Buzz.

    // Special charater that Buzz requires to not be encoded
    private static final String SPECIAL_DELIMITER = "@";

    /**
     * For Buzz, we have to take special care not to encode the "@" sign
     */
    @Override
    public String encodePathSegment(String decodedURLComponent) {
      // Take special precautions to avoid encoding the special Buzz delimiter
      String[] segments = decodedURLComponent.split(SPECIAL_DELIMITER);
      List<String> result = Lists.newArrayListWithCapacity(segments.length);
      for (String segment : segments) {
        result.add(URL.encodePathSegment(segment));
      }
      return Join.join(SPECIAL_DELIMITER, result);
    }

    @Override
    public String encodeQueryString(String decodedURLComponent) {
      return URL.encodeQueryString(decodedURLComponent);
    }
  };
}
