/*
 * Copyright (c) 2009-2013 Panxiaobo
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
package pxb.android;

import java.util.Arrays;

public class StringItem implements ResConst {
    final public String data;
    final public StyleSpan[] styleSpans;
    final public int resourceId;
    public int dataOffset;
    public int index;

    public StringItem(String data) {
        super();
        this.data = data;
        this.resourceId = NO_RESOURCE_ID;
        this.styleSpans = null;
    }

    public StringItem(String data, int resourceId) {
        super();
        this.data = data;
        this.resourceId = resourceId;
        this.styleSpans = null;
    }

    public StringItem(String data, StyleSpan[] styleSpans) {
        super();
        this.data = data;
        this.styleSpans = styleSpans;
        this.resourceId = NO_RESOURCE_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        StringItem that = (StringItem) o;
        if (!data.equals(that.data))
            return false;
        if (resourceId != that.resourceId)
            return false;
        if (resourceId != NO_RESOURCE_ID) {
            return true;
        }

        if (!Arrays.equals(styleSpans, that.styleSpans))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = data.hashCode();
        result = 31 * result + (styleSpans != null ? Arrays.hashCode(styleSpans) : 0);
        result = 31 * result + resourceId;
        return result;
    }

    public String toString() {
        return String.format("S%04d %s", index, data);
    }

}
