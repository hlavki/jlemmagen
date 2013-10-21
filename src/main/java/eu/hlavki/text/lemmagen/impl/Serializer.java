/*
 * Copyright (C) 2013 Michal Hlavac <hlavki@hlavki.eu>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
