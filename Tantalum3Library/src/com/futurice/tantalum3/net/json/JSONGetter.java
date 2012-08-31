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
package com.futurice.tantalum3.net.json;

import com.futurice.tantalum3.Task;
import com.futurice.tantalum3.log.Log;
import com.futurice.tantalum3.net.HttpGetter;

/**
 *
 * @author Paul Houghton
 */
public class JSONGetter extends HttpGetter {

    private final JSONModel jsonvo;

    public JSONGetter(final String url, final JSONModel jsonModel, final Task result, final int retriesRemaining) {
        super(url, retriesRemaining, result);
        this.jsonvo = jsonModel;
    }

    public Object exec() {
        final byte[] bytes = (byte[]) super.exec();
        String value = "";

        try {
            value = bytes.toString().trim();
            if (value.startsWith("[")) {
                // Parser expects non-array base object- add one
                value = "{\"base:\"" + value + "}";
            }
            jsonvo.setJSON(value);
            result.set(value);
        } catch (Exception e) {
            //#debug
            Log.l.log("JSONGetter HTTP response problem", this.getUrl() + " : " + value, e);
            result.cancel(false);
        }
        
        return bytes;
    }
}