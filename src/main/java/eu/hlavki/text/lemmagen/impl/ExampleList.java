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

import static eu.hlavki.text.lemmagen.impl.LemmatizerSettings.MsdConsideration.IGNORE;
import static eu.hlavki.text.lemmagen.impl.Serializer.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Michal Hlavac <hlavki@hlavki.eu>
 */
public class ExampleList {

    private static final Logger log = LoggerFactory.getLogger(ExampleList.class);
    private LemmatizerSettings settings;
    private RuleList rules;
    private Map<String, LemmaExample> examples;
    private transient List<LemmaExample> examplesList;

    public ExampleList(LemmatizerSettings settings) {
        this.settings = settings;

        this.examples = new TreeMap<String, LemmaExample>();
        this.examplesList = null;
        this.rules = new RuleList(settings);
    }

    public ExampleList(BufferedReader reader, String sFormat, LemmatizerSettings settings)
            throws NumberFormatException, IOException {
        this(settings);
        addMultextFile(reader, sFormat);
    }

    public LemmaExample get(int idx) {
        if (examplesList == null) finalizeAdditions();
        return examplesList.get(idx);
    }

    public int getSize() {
        if (examplesList == null) finalizeAdditions();
        return examplesList.size();
    }

    public double getWeightSum() {
        if (examplesList == null) finalizeAdditions();
        double weight = 0;
        for (LemmaExample exm : examplesList) {
            weight += exm.getWeight();
        }
        return weight;
    }

    public List<LemmaExample> getExamplesList() {
        if (examplesList == null) finalizeAdditions();
        return examplesList;
    }

    public RuleList getRules() {
        return rules;
    }

    public final void addMultextFile(BufferedReader reader, String format) throws IOException {
        int err = 0;
        int lineIdx = 0;

        int wIdx = format.indexOf('W');
        int lIdx = format.indexOf('L');
        int mIdx = format.indexOf('M');
        int fIdx = format.indexOf('F');
        int iLen = Math.max(Math.max(wIdx, lIdx), Math.max(mIdx, fIdx)) + 1;

        if (wIdx < 0 || lIdx < 0) {
            log.error("Can not find word and lemma location in the format specification");
            return;
        }

        String line;
        while ((line = reader.readLine()) != null && err < 50) {
            lineIdx++;

            String[] words = line.split("\t");
            if (words.length < iLen) {
                log.warn("Line doesn't confirm to the given format \"" + format + "\"! Line " + lineIdx + ".");
                err++;
                continue;
            }

            String word = words[wIdx];
            String lemma = words[lIdx];
            if (lemma.equals("=")) {
                lemma = word;
            }
            String msd = null;
            if (mIdx > -1) msd = words[mIdx];
            double weight = 1;
            if (fIdx > -1) {
                weight = Double.parseDouble(words[mIdx]);
            }

            addExample(word, lemma, weight, msd);
        }
        if (err == 50) {
            log.error("Parsing stopped because of too many (50) errors. Check format specification");
        }
    }

    public LemmaExample addExample(String word, String lemma, double weight, String msd) {
        String newMsd = settings.getMsdConsider() != IGNORE ? msd : null;
        LemmaExample leNew = new LemmaExample(word, lemma, weight, newMsd, rules, settings);
        return add(leNew);
    }

    private LemmaExample add(LemmaExample newLe) {
        LemmaExample result = examples.get(newLe.getSignature());
        if (result == null) {
            examples.put(newLe.getSignature(), newLe);
        } else {
            result.join(newLe);
        }
        examplesList = null;
        return result;
    }

    public final void clear() {
        examples.clear();
        examplesList = null;
    }

    public final void finalizeAdditions() {
        if (examplesList != null) return;
        examplesList = new ArrayList<LemmaExample>(examples.values());
        Collections.sort(examplesList);
    }

    public ExampleList getFrontRearExampleList(boolean front) {
        ExampleList examplesNew = new ExampleList(settings);
        for (LemmaExample le : getExamplesList()) {
            if (front) {
                examplesNew.addExample(le.getWordFront(), le.getLemmaFront(), le.getWeight(), le.getMsd());
            } else {
                examplesNew.addExample(le.getWordRear(), le.getLemmaRear(), le.getWeight(), le.getMsd());
            }
        }
        examplesNew.finalizeAdditions();
        return examplesNew;
    }

    public void writeObject(ObjectOutput out, boolean serializeExamples, boolean topObject) throws IOException {
        //save metadata
        out.writeBoolean(topObject);

        //save refernce types if needed -------------------------
        if (topObject) {
            settings.writeObject(out);
        }

        rules.writeObject(out, false);

        if (!serializeExamples) {
            out.writeBoolean(false); // lstExamples == null
            out.writeInt(0); // dictExamples.Count == 0
        } else {
            if (examplesList == null) {
                out.writeBoolean(false); // lstExamples == null

                //save dictionary items
                int count = examples.size();
                out.writeInt(count);

                for (LemmaExample kvp : examples.values()) {
                    writeString(out, kvp.getRule().getSignature());
                    kvp.writeObject(out, false);
                }
            } else {
                out.writeBoolean(true); // lstExamples != null

                //save list & dictionary items
                int count = examplesList.size();
                out.writeInt(count);

                for (LemmaExample le : examplesList) {
                    writeString(out, le.getRule().getSignature());
                    le.writeObject(out, false);
                }
            }
        }
    }

    public ExampleList(ObjectInput in, LemmatizerSettings settings) throws IOException,
            ClassNotFoundException {
        readObject(in, settings);
    }

    private void readObject(ObjectInput in, LemmatizerSettings settings) throws IOException,
            ClassNotFoundException {
        //load metadata
        boolean topObject = in.readBoolean();

        //load refernce types if needed -------------------------
        if (topObject) {
            this.settings = new LemmatizerSettings(in);
        } else {
            this.settings = settings;
        }

        rules = new RuleList(in, this.settings);

        boolean createLstExamples = in.readBoolean();

        examplesList = createLstExamples ? new ArrayList<LemmaExample>() : null;
        examples = new HashMap<String, LemmaExample>();

        //load dictionary items
        int count = in.readInt();
        for (int idx = 0; idx < count; idx++) {
            LemmaRule rule = rules.get(readString(in));
            LemmaExample le = new LemmaExample(in, this.settings, rule);

            examples.put(le.getSignature(), le);
            if (createLstExamples) examplesList.add(le);
        }
    }
}
