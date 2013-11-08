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
