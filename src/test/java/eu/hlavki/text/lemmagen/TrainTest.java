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
