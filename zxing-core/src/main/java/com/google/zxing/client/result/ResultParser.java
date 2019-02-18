/*
 * Copyright 2007 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.result;

import com.google.zxing.Result;

import java.util.regex.Pattern;

/**
 * <p>Abstract class representing the result of decoding a barcode, as more than
 * a String -- as some type of structured data. This might be a subclass which represents
 * a URL, or an e-mail address. {@link #parseResult(Result)} will turn a raw
 * decoded string into the most appropriate type of structured representation.</p>
 *
 * <p>Thanks to Jeff Griffin for proposing rewrite of these classes that relies less
 * on exception-based mechanisms during parsing.</p>
 *
 * @author Sean Owen
 */
public abstract class ResultParser {

  private static final ResultParser[] PARSERS = {
      new ISBNResultParser(),
      new ProductResultParser(),
      new ExpandedProductResultParser(),
      new VINResultParser(),
  };

  private static final Pattern DIGITS = Pattern.compile("\\d+");
  private static final String BYTE_ORDER_MARK = "\ufeff";

  /**
   * Attempts to parse the raw {@link Result}'s contents as a particular type
   * of information (email, URL, etc.) and return a {@link ParsedResult} encapsulating
   * the result of parsing.
   *
   * @param theResult the raw {@link Result} to parse
   * @return {@link ParsedResult} encapsulating the parsing result
   */
  public abstract ParsedResult parse(Result theResult);

  protected static String getMassagedText(Result result) {
    String text = result.getText();
    if (text.startsWith(BYTE_ORDER_MARK)) {
      text = text.substring(1);
    }
    return text;
  }

  public static ParsedResult parseResult(Result theResult) {
    for (ResultParser parser : PARSERS) {
      ParsedResult result = parser.parse(theResult);
      if (result != null) {
        return result;
      }
    }
    return new TextParsedResult(theResult.getText(), null);
  }

  protected static boolean isStringOfDigits(CharSequence value, int length) {
    return value != null && length > 0 && length == value.length() && DIGITS.matcher(value).matches();
  }

  protected static boolean isSubstringOfDigits(CharSequence value, int offset, int length) {
    if (value == null || length <= 0) {
      return false;
    }
    int max = offset + length;
    return value.length() >= max && DIGITS.matcher(value.subSequence(offset, max)).matches();
  }

}
