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
    public String lemmatize(String word) {
        if (!settings.isBuildFrontLemmatizer()) {
            return getRootNodeSafe().lemmatize(word);
        } else {
            String wordFront = new StringBuilder(word).reverse().toString();
            String lemmaFront = getRootNodeFrontSafe().lemmatize(wordFront);
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
        System.out.println("Readed " + examples.getSize());

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
