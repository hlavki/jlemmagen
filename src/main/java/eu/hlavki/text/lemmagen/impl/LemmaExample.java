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
import java.util.Arrays;
import java.util.HashSet;

/**
 *
 * @author Michal Hlavac <hlavki@hlavki.eu>
 */
public class LemmaExample implements Comparable<LemmaExample> {

    private String word;
    private String lemma;
    private String signature;
    private String msd;
    private double weight;
    private LemmaRule rule;
    private LemmatizerSettings settings;

    private String wordRearCache;
    private String wordFrontCache;
    private String lemmaFrontCache;

    @SuppressWarnings("LeakingThisInConstructor")
    public LemmaExample(String word, String lemma, double weight, String msd, RuleList rules,
            LemmatizerSettings settings) {
        this.word = word;
        this.lemma = lemma;
        this.msd = msd;
        this.weight = weight;
        this.settings = settings;
        this.rule = rules.addRule(this);

        switch (settings.getMsdConsider()) {
            case IGNORE:
            case JOIN_ALL:
            case JOIN_DISTINCT:
            case JOIN_SAME_SUBSTRING:
                signature = "[" + word + "]==>[" + lemma + "]";
                break;
            case DISTINCT:
            default:
                signature = "[" + word + "]==>[" + lemma + "](" + (msd != null ? msd : "") + ")";
                break;
        }

        this.wordRearCache = null;
        this.wordFrontCache = null;
        this.lemmaFrontCache = null;
    }

    public String getWord() {
        return word;
    }

    public String getLemma() {
        return lemma;
    }

    public String getSignature() {
        return signature;
    }

    public String getMsd() {
        return msd;
    }

    public double getWeight() {
        return weight;
    }

    public LemmaRule getRule() {
        return rule;
    }

    public String getWordFront() {
        if (wordFrontCache == null) {
            wordFrontCache = new StringBuilder(word).reverse().toString();
        }
        return wordFrontCache;
    }

    /**
     * Lemma to be produced by pre-lemmatizing with Front-Lemmatizer (Warning it is reversed)
     *
     * @return
     */
    public String getLemmaFront() {
        if (lemmaFrontCache == null) {
            lemmaFrontCache = new StringBuilder(getWordRear()).reverse().toString();
        }
        return lemmaFrontCache;
    }

    /**
     * word to be lemmatized by standard Rear-Lemmatizer (it's beggining has been already modified by
     * Front-Lemmatizer)
     *
     * @return
     */
    public String getWordRear() {
        if (wordRearCache == null) {
            LongestCommonResult lcResult = longestCommonSubString(word, lemma);
            String common = lcResult.substring;
            int wordPos = lcResult.pos1;
            int lemmaPos = lcResult.pos2;
            wordRearCache = lemmaPos == -1 ? lemma
                    : (lemma.substring(0, lemmaPos + common.length()) + word.substring(wordPos + common.length()));
        }
        return wordRearCache;
    }

    /**
     * lemma to be produced by standard Rear-Lemmatizer from WordRear
     *
     * @return
     */
    public String getLemmaRear() {
        return lemma;
    }

    private LongestCommonResult longestCommonSubString(String str1, String str2) {
        int[][] l = new int[str1.length() + 1][str2.length() + 1];
        int z = 0;
        String ret = "";
        int pos1 = -1;
        int pos2 = -1;

        for (int i = 0; i < str1.length(); i++)
            for (int j = 0; j < str2.length(); j++)
                if (str1.charAt(i) == str2.charAt(j)) {
                    if (i == 0 || j == 0) l[i][j] = 1;
                    else l[i][j] = l[i - 1][j - 1] + 1;
                    if (l[i][j] > z) {
                        z = l[i][j];
                        pos1 = i - z + 1;
                        pos2 = j - z + 1;
                        ret = str1.substring(i - z + 1, (i - z + 1) + z);
                    }
                }

        return new LongestCommonResult(ret, pos1, pos2);
    }

    public static int equalsPrifixLen(String str1, String str2) {
        int maxLen = Math.min(str1.length(), str2.length());

        for (int pos = 0; pos < maxLen; pos++)
            if (str1.charAt(pos) != str2.charAt(pos)) return pos;

        return maxLen;
    }

    @Override
    public int compareTo(LemmaExample o) {
        int result;

        result = compareStrings(this.word, o.word, false);
        if (result != 0) return result;

        result = compareStrings(this.lemma, o.lemma, true);
        if (result != 0) return result;

        if (settings.getMsdConsider() == LemmatizerSettings.MsdConsideration.DISTINCT
                && this.msd != null && o.msd != null) {
            result = compareStrings(this.msd, o.msd, true);
            if (result != 0) return result;
        }

        return 0;
    }

    public void join(LemmaExample joinLe) {
        weight += joinLe.weight;
        if (msd != null)
            switch (settings.getMsdConsider()) {
                case IGNORE:
                    msd = null;
                    break;
                case DISTINCT:
                    break;
                case JOIN_ALL:
                    msd += "|" + joinLe.msd;
                    break;
                case JOIN_DISTINCT:
                    if (!new HashSet<String>(Arrays.asList(msd.split("\\|"))).contains(joinLe.msd)) {
                        msd += "|" + joinLe.msd;
                    }
                    break;
                case JOIN_SAME_SUBSTRING:
                    int pos = 0;
                    int max = Math.min(msd.length(), joinLe.msd.length());
                    while (pos < max && msd.charAt(pos) == joinLe.msd.charAt(pos))
                        pos++;
                    msd = msd.substring(0, pos);
                    break;
                default:
                    break;
            }
    }

    public int similarTo(LemmaExample le) {
        return similar(this, le);
    }

    public static int similar(LemmaExample le1, LemmaExample le2) {
        String word1 = le1.word;
        String word2 = le2.word;
        int len1 = word1.length();
        int len2 = word2.length();
        int maxLen = Math.min(len1, len2);

        for (int pos = 1; pos <= maxLen; pos++)
            if (word1.charAt(len1 - pos) != word2.charAt(len2 - pos)) return pos - 1;

        //TODO similarTo should be bigger if two words are totaly equal
        //if (word1 == word2)
        //    return maxLen + 1;
        //else
        return maxLen;
    }

    /**
     * Function used to comprare current MultextExample (ME) against argument ME. Mainly used in for sorting
     * lists of MEs.
     *
     * @param str1 string to compare
     * @param str2 string to compare
     * @param forward
     * @return 1 if current ME is bigger, -1 if smaler and 0 if both are the same.
     */
    public static int compareStrings(String str1, String str2, boolean forward) {
        int len1 = str1.length();
        int len2 = str2.length();
        int maxlen = Math.min(len1, len2);

        if (forward) {
            for (int pos = 0; pos < maxlen; pos++) {
                if (str1.charAt(pos) > str2.charAt(pos)) return 1;
                if (str1.charAt(pos) < str2.charAt(pos)) return -1;
            }
        } else {
            for (int pos = 1; pos <= maxlen; pos++) {
                if (str1.charAt(len1 - pos) > str2.charAt(len2 - pos)) return 1;
                if (str1.charAt(len1 - pos) < str2.charAt(len2 - pos)) return -1;
            }
        }

        if (len1 > len2) return 1;
        if (len1 < len2) return -1;
        return 0;
    }

    private static class LongestCommonResult {

        public final String substring;
        public final int pos1;
        public final int pos2;

        public LongestCommonResult(String substring, int pos1, int pos2) {
            this.substring = substring;
            this.pos1 = pos1;
            this.pos2 = pos2;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(word == null ? "" : "W:\"" + word + "\" ");
        sb.append(lemma == null ? "" : "L:\"" + lemma + "\" ");
        sb.append(msd == null ? "" : "M:\"" + msd + "\" ");
        sb.append(Double.isNaN(weight) ? "" : "F:\"" + weight + "\" ");
        sb.append(rule == null ? "" : "R:" + rule.toString() + " ");

        return sb.substring(0, sb.length() - 1);
    }

    public void writeObject(ObjectOutput out, boolean topObject) throws IOException {
//save metadata
        out.writeBoolean(topObject);

        //save value types --------------------------------------
        writeString(out, word);
        writeString(out, lemma);
        writeString(out, signature);
        if (msd == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            writeString(out, msd);
        }
        out.writeDouble(weight);

        //save refernce types if needed -------------------------
        if (topObject) {
            settings.writeObject(out);
            rule.writeObject(out, false);
        }
    }

    public LemmaExample(ObjectInput in, LemmatizerSettings settings, LemmaRule rule) throws IOException,
            ClassNotFoundException {
        readObject(in, settings, rule);
    }

    private void readObject(ObjectInput in, LemmatizerSettings settings, LemmaRule rule) throws IOException,
            ClassNotFoundException {
        //load metadata
        boolean topObject = in.readBoolean();

        //load value types --------------------------------------
        word = readString(in);
        lemma = readString(in);
        signature = readString(in);
        if (in.readBoolean()) {
            msd = readString(in);
        } else {
            msd = null;
        }
        weight = in.readDouble();

        //load refernce types if needed -------------------------
        if (topObject) {
            this.settings = new LemmatizerSettings(in);
            this.rule = new LemmaRule(in, this.settings);
        } else {
            this.settings = settings;
            this.rule = rule;
        }

        this.wordRearCache = null;
        this.wordFrontCache = null;
        this.lemmaFrontCache = null;
    }
}
