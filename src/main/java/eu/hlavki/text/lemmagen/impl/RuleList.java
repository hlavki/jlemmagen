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

import static eu.hlavki.text.lemmagen.impl.Serializer.*;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Michal Hlavac <hlavki@hlavki.eu>
 */
public class RuleList extends HashMap<String, LemmaRule> {

    private static final long serialVersionUID = -4961187394392918555L;

    private LemmatizerSettings settings;
    private LemmaRule defaultRule;

    public RuleList(LemmatizerSettings settings) {
        super();
        this.settings = settings;
        defaultRule = addRule(new LemmaRule("", "", 0, settings));
    }

    public LemmaRule getDefaultRule() {
        return defaultRule;
    }

    public LemmaRule addRule(LemmaExample le) {
        return addRule(new LemmaRule(le.getWord(), le.getLemma(), this.size(), settings));
    }

    private LemmaRule addRule(LemmaRule lemmaRule) {
        LemmaRule result = get(lemmaRule.getSignature());
        if (result == null) {
            this.put(lemmaRule.getSignature(), lemmaRule);
            result = lemmaRule;
        }
        return result;
    }

    public void writeObject(ObjectOutput out, boolean topObject) throws IOException {
        //save metadata
        out.writeBoolean(topObject);

        //save value types --------------------------------------
        //save refernce types if needed -------------------------
        if (topObject) {
            settings.writeObject(out);
        }

        //save list items ---------------------------------------
        out.writeInt(this.size());
        for (Map.Entry<String, LemmaRule> kvp : entrySet()) {
            writeString(out, kvp.getKey());
            kvp.getValue().writeObject(out, false);
        }

        //default rule is already saved in the list. Here just save its id.
        writeString(out, defaultRule.getSignature());
    }

    public RuleList(ObjectInput in, LemmatizerSettings settings) throws IOException, ClassNotFoundException {
        super();
        readObject(in, settings);
    }

    private void readObject(ObjectInput in, LemmatizerSettings settings) throws IOException,
            ClassNotFoundException {
        //load metadata
        boolean topObject = in.readBoolean();

        //load value types --------------------------------------
        //load refernce types if needed -------------------------
        if (topObject) {
            this.settings = new LemmatizerSettings(in);
        } else {
            this.settings = settings;
        }

        //load list items ---------------------------------------
        this.clear();
        int count = in.readInt();
        for (int idx = 0; idx < count; idx++) {
            String key = readString(in);
            LemmaRule value = new LemmaRule(in, this.settings);
            this.put(key, value);
        }

        //link the default rule just Id was saved.
        defaultRule = get(readString(in));
    }
}
