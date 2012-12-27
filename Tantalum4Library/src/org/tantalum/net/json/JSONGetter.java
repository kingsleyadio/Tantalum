/*
 * Tantalum Mobile Toolset
 * https://projects.forum.nokia.com/Tantalum
 *
 * Special thanks to http://www.futurice.com for support of this project
 * Project lead: paul.houghton@futurice.com
 *
 * Copyright 2010 Paul Eugene Houghton
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.tantalum.net.json;

import org.tantalum.util.L;
import org.tantalum.net.HttpGetter;

/**
 *
 * @author Paul Houghton
 */
public class JSONGetter extends HttpGetter {

    private final JSONModel jsonModel;

    public JSONGetter(final String key, final JSONModel jsonModel) {
        super(key);
        this.jsonModel = jsonModel;
    }

    public Object doInBackground(final Object in) {
        String value = null;
        try {
            value = new String((byte[]) super.doInBackground(in), "UTF8").trim();
            if (value.startsWith("[")) {
                // Parser expects non-array base object- add one
                value = "{\"base:\"" + value + "}";
            }
            jsonModel.setJSON(value);
            setValue(jsonModel);
        } catch (Exception e) {
            //#debug
            L.e("JSONGetter HTTP response problem", key + " : " + value, e);
            cancel(false);
        }

        return jsonModel;
    }
}