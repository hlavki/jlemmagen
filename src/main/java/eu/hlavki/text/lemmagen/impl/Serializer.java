/*
 * Copyright 2013 Michal Hlavac <hlavki@hlavki.eu>.
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
package eu.hlavki.text.lemmagen.impl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 *
 * @author Michal Hlavac <hlavki@hlavki.eu>
 */
final class Serializer {

    public static final void writeString(ObjectOutput out, String str) throws IOException {
        out.writeBoolean(str == null);
        if (str != null) {
            out.writeUTF(str);
        }
    }

    public static final String readString(ObjectInput in) throws IOException {
        boolean n = in.readBoolean();
        String result = null;
        if (!n) {
            result = in.readUTF();
        }
        return result;
    }
}
