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

/**
 *
 * @author Michal Hlavac <hlavki@hlavki.eu>
 */
public class LemmaRule {

    private int id;
    private int from;
    private String fromStr;
    private String toStr;
    private String signature;
    private LemmatizerSettings settings;

    public LemmaRule(String word, String lemma, int id, LemmatizerSettings settings) {
        this.settings = settings;
        this.id = id;

        int sameStem = sameStem(word, lemma);
        toStr = lemma.substring(sameStem);
        from = word.length() - sameStem;
 
        if (settings.isUseFromInRules()) {
            fromStr = word.substring(sameStem);
            signature = "[" + fromStr + "]==>[" + toStr + "]";
        } else {
            fromStr = null;
            signature = "[#" + from + "]==>[" + toStr + "]";
        }
    }

    public int getId() {
        return id;
    }

    public String getSignature() {
        return signature;
    }

    public int getFrom() {
        return from;
    }

    public String getToStr() {
        return toStr;
    }

    @Override
    public String toString() {
        return id + ":" + signature;
    }

    public boolean isApplicableToGroup(int groupCondLen) {
        return groupCondLen >= from;
    }

    public CharSequence lemmatize(CharSequence word) {
        return word.subSequence(0, word.length() - from) + toStr;
    }

    private static int sameStem(String str1, String str2) {
        int maxLength = Math.min(str1.length(), str2.length());

        for (int pos = 0; pos < maxLength; pos++)
            if (str1.charAt(pos) != str2.charAt(pos)) return pos;

        return maxLength;
    }

    public void writeObject(ObjectOutput out, boolean topObject) throws IOException {
        //save metadata
        out.writeBoolean(topObject);

        out.writeInt(id);
        out.writeInt(from);
        writeString(out,fromStr);
        writeString(out,toStr);
        writeString(out,signature);

        if (topObject) {
            settings.writeObject(out);
        }
    }

    public LemmaRule(ObjectInput in, LemmatizerSettings settings) throws IOException, ClassNotFoundException {
        readObject(in, settings);
    }

    private void readObject(ObjectInput in, LemmatizerSettings settings) throws IOException,
            ClassNotFoundException {
        //load metadata
        boolean topObject = in.readBoolean();

        //load value types --------------------------------------
        id = in.readInt();
        from = in.readInt();
        fromStr = readString(in);
        toStr = readString(in);
        signature = readString(in);

        //load refernce types if needed -------------------------
        if (topObject) {
            this.settings = new LemmatizerSettings(in);
        } else {
            this.settings = settings;
        }
    }
}
