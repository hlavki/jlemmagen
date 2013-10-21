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
public class LemmatizerSettings implements Cloneable {

    /**
     * How algorithm considers msd tags.
     */
    public enum MsdConsideration {

        /**
         * Completely ignores mds tags (join examples with different tags and sum their weihgts).
         */
        IGNORE,
        /**
         * Same examples with different msd's are not considered equal and joined.
         */
        DISTINCT,
        /**
         * Joins examples with different tags (concatenates all msd tags).
         */
        JOIN_ALL,
        /**
         * Joins examples with different tags (concatenates just distinct msd tags - somehow slower).
         */
        JOIN_DISTINCT,
        /**
         * Joins examples with different tags (new tag is the left to right substring that all joined examples
         * share).
         */
        JOIN_SAME_SUBSTRING
    }

    /**
     * True if from string should be included in rule identifier ([from]->[to]). False if just length of from
     * string is used ([#len]->[to]).
     */
    private boolean useFromInRules;
    /**
     * Specification how algorithm considers msd tags.
     */
    private MsdConsideration msdConsider;
    /**
     * How many of the best rules are kept in memory for each node. Zero means unlimited.
     */
    private int maxRulesPerNode;
    /**
     * If true, than build proccess uses few more hevristics to build first left to right lemmatizer
     * (lemmatizes front of the word)
     */
    private boolean buildFrontLemmatizer;

    public LemmatizerSettings() {
        this(true, MsdConsideration.DISTINCT, 0, false);
    }

    public LemmatizerSettings(boolean useFromInRules, MsdConsideration msdConsider, int maxRulesPerNode,
            boolean buildFrontLemmatizer) {
        this.useFromInRules = useFromInRules;
        this.msdConsider = msdConsider;
        this.maxRulesPerNode = maxRulesPerNode;
        this.buildFrontLemmatizer = buildFrontLemmatizer;
    }

    public boolean isUseFromInRules() {
        return useFromInRules;
    }

    public void setUseFromInRules(boolean useFromInRules) {
        this.useFromInRules = useFromInRules;
    }

    public MsdConsideration getMsdConsider() {
        return msdConsider;
    }

    public void setMsdConsider(MsdConsideration msdConsider) {
        this.msdConsider = msdConsider;
    }

    public int getMaxRulesPerNode() {
        return maxRulesPerNode;
    }

    public void setMaxRulesPerNode(int maxRulesPerNode) {
        this.maxRulesPerNode = maxRulesPerNode;
    }

    public boolean isBuildFrontLemmatizer() {
        return buildFrontLemmatizer;
    }

    public void setBuildFrontLemmatizer(boolean buildFrontLemmatizer) {
        this.buildFrontLemmatizer = buildFrontLemmatizer;
    }

    public void writeObject(ObjectOutput out) throws IOException {
        out.writeBoolean(useFromInRules);
        out.writeInt(msdConsider.ordinal());
        out.writeInt(maxRulesPerNode);
        out.writeBoolean(buildFrontLemmatizer);
    }

    public LemmatizerSettings(ObjectInput in) throws IOException, ClassNotFoundException {
        readObject(in);
    }

    private void readObject(ObjectInput in) throws IOException, ClassNotFoundException {
        useFromInRules = in.readBoolean();
        msdConsider = MsdConsideration.values()[in.readInt()];
        maxRulesPerNode = in.readInt();
        buildFrontLemmatizer = in.readBoolean();
    }
}
