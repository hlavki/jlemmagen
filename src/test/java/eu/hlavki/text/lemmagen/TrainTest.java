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
package eu.hlavki.text.lemmagen;

import eu.hlavki.text.lemmagen.api.Lemmatizer;
import java.io.BufferedReader;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import eu.hlavki.text.lemmagen.impl.DefaultLemmatizer;
import eu.hlavki.text.lemmagen.impl.LemmatizerSettings;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TrainTest {

    private static final String TEST_DICTIONARY = "/wfl-me-en.tbl";
    private static final String[] ACTUAL_WORDS = new String[]{"respond", "are", "uninflected", "items", "underlying", "singing"};
    private static final String[][] LEMMA_WORDS = new String[][]{
        {"respond", "be", "uninflect", "item", "underlie", "sing"}
    };

    @Before
    public void beforeTest() {
    }

    @After
    public void afterTest() {
    }

    @Test
    public void trainEnglish() {
        File tmpLemFile = null;
        try {
            tmpLemFile = File.createTempFile("lemmagen", ".lem");
            String format = "WLM";
            LemmatizerSettings settings = new LemmatizerSettings();
//            settings.setUseFromInRules(false);
//            settings.setMsdConsider(MsdConsideration.IGNORE);
//            settings.setMaxRulesPerNode(0);
//            settings.setBuildFrontLemmatizer(true);

            InputStream in = TrainTest.class.getResourceAsStream(TEST_DICTIONARY);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));

            System.out.println("Building model...");
            DefaultLemmatizer lm = new DefaultLemmatizer(br, format, settings);
            lm.buildModel();

            System.out.println("Model built");

            System.out.println("Saving model...");
            LemmatizerFactory.saveToFile(lm, tmpLemFile);
            System.out.println("Model saved.");

            assertLemmaEquals(lm, ACTUAL_WORDS, LEMMA_WORDS);

            System.out.println("Clearing examples...");
            lm.clearExamples();
            System.out.println("Examples clear...");

            System.out.println("Reading model from file");
            lm = (DefaultLemmatizer) LemmatizerFactory.readFromFile(tmpLemFile);

            assertLemmaEquals(lm, ACTUAL_WORDS, LEMMA_WORDS);

        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (tmpLemFile != null) tmpLemFile.delete();
        }
    }

    private static void assertLemmaEquals(Lemmatizer lm, String[] actual, String[][] expected) {
        for (int idx = 0; idx < actual.length; idx++) {
            CharSequence lemma = lm.lemmatize(actual[idx]);
            boolean result = false;
            StringBuilder sb = new StringBuilder("[");
            for (String[] row : expected) {
                result |= row[idx].equals(lemma);
                sb.append(row[idx]).append(", ");
            }
            sb.delete(sb.length() - 2, sb.length()).append("]");
            System.out.println("Lemma of " + actual[idx] + " is " + lemma + " and must be one of " + sb.toString());
            assertTrue(result);
        }
    }
}
