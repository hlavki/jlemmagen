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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.hlavki.text.lemmagen.api.TrainableLemmatizer;

/**
 *
 * @author Michal Hlavac <hlavki@hlavki.eu>
 */
public class DefaultLemmatizer implements TrainableLemmatizer {

    private static final Logger log = LoggerFactory.getLogger(DefaultLemmatizer.class);
    private LemmatizerSettings settings;
    private ExampleList examples;
    private LemmaTreeNode rootNode;
    private LemmaTreeNode rootNodeFront;

    public DefaultLemmatizer() {
        this(new LemmatizerSettings());
    }

    public DefaultLemmatizer(LemmatizerSettings settings) {
        this.settings = settings;
        this.examples = new ExampleList(settings);
        this.rootNode = null;
        this.rootNodeFront = null;
    }

    public DefaultLemmatizer(BufferedReader reader, String format, LemmatizerSettings settings)
            throws IOException {
        this(settings);
        addMultextFile(reader, format);
    }

    public LemmaTreeNode getRootNode() {
        return getRootNodeSafe();
    }

    public LemmaTreeNode getRootNodeFront() {
        return getRootNodeFrontSafe();
    }

    private LemmaTreeNode getRootNodeSafe() {
        if (rootNode == null) buildModel();
        return rootNode;
    }

    private LemmaTreeNode getRootNodeFrontSafe() {
        if (rootNodeFront == null && settings.isBuildFrontLemmatizer()) buildModel();
        return rootNodeFront;
    }

    public final void addMultextFile(BufferedReader reader, String format) throws IOException {
        this.examples.addMultextFile(reader, format);
        rootNode = null;
    }

    @Override
    public void addExample(String word, String lemma) {
        addExample(word, lemma, 1, null);
    }

    @Override
    public void addExample(String word, String lemma, double weight) {
        addExample(word, lemma, weight, null);
    }

    @Override
    public void addExample(String word, String lemma, double weight, String msd) {
        examples.addExample(word, lemma, weight, msd);
        rootNode = null;
    }

    public void clearExamples() {
        examples.clear();
    }

    public void finalizeAdditions() {
        examples.finalizeAdditions();
    }

    @Override
    public final void buildModel() {
        if (rootNode != null) return;

        if (!settings.isBuildFrontLemmatizer()) {
            //TODO remove: elExamples.FinalizeAdditions();
            examples.finalizeAdditions();
            rootNode = new LemmaTreeNode(settings, examples);
        } else {
            rootNode = new LemmaTreeNode(settings, examples.getFrontRearExampleList(false));
            rootNodeFront = new LemmaTreeNode(settings, examples.getFrontRearExampleList(true));
        }
    }

    @Override
    public CharSequence lemmatize(CharSequence word) {
        if (!settings.isBuildFrontLemmatizer()) {
            return getRootNodeSafe().lemmatize(word);
        } else {
            String wordFront = new StringBuilder(word).reverse().toString();
            CharSequence lemmaFront = getRootNodeFrontSafe().lemmatize(wordFront);
            String wordRear = new StringBuilder(lemmaFront).reverse().toString();
            return getRootNodeSafe().lemmatize(wordRear);
        }
    }

    public void writeObject(ObjectOutput out, boolean serializeExamples) throws IOException {

        settings.writeObject(out);

        out.writeBoolean(serializeExamples);
        examples.writeObject(out, serializeExamples, false);

        if (!serializeExamples) {
            examples.getFrontRearExampleList(false).writeObject(out, serializeExamples, false);
            examples.getFrontRearExampleList(true).writeObject(out, serializeExamples, false);
        }

        rootNode.writeObject(out);
        if (settings.isBuildFrontLemmatizer()) {
            rootNodeFront.writeObject(out);
        }
    }

    public DefaultLemmatizer(ObjectInput in) throws IOException {
        try {
            readObject(in);
            buildModel();
        } catch (ClassNotFoundException e) {
            log.error("Can't load instance from input stream", e);
        }
    }

    private void readObject(ObjectInput in) throws IOException, ClassNotFoundException {
        settings = new LemmatizerSettings(in);

        boolean serializeExamples = in.readBoolean();
        examples = new ExampleList(in, settings);

        ExampleList examplesRear;
        ExampleList examplesFront;

        if (serializeExamples) {
            examplesRear = examples.getFrontRearExampleList(false);
            examplesFront = examples.getFrontRearExampleList(true);
        } else {
            examplesRear = new ExampleList(in, settings);
            examplesFront = new ExampleList(in, settings);
        }

        if (!settings.isBuildFrontLemmatizer()) {
            rootNode = new LemmaTreeNode(in, settings, examples, null);
        } else {
            rootNode = new LemmaTreeNode(in, settings, examplesRear, null);
            rootNodeFront = new LemmaTreeNode(in, settings, examplesFront, null);
        }
    }

}
