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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import eu.hlavki.text.lemmagen.api.Lemmatizer;
import static eu.hlavki.text.lemmagen.impl.Serializer.*;

/**
 *
 * @author Michal Hlavac <hlavki@hlavki.eu>
 */
public final class LemmaTreeNode implements Lemmatizer {

    private static final long serialVersionUID = 6749137746050810566L;

    //settings
    private LemmatizerSettings settings;

    //tree structure references
    private Map<Character, LemmaTreeNode> subNodes;
    private LemmaTreeNode parentNode;

    //essential node properties
    private int similarity; //similarity among all words in this node
    private String condition; //suffix that must match in order to lemmatize
    private boolean wholeWord; //true if condition has to match to whole word

    //rules and weights;
    private LemmaRule bestRule; //the best rule to be applied when lemmatizing
    private WeightedRule[] bestRules; //list of best rules
    private double weight;

    //source of this node
    private int start;
    private int end;
    private ExampleList examples;

    private LemmaTreeNode(LemmatizerSettings settings) {
        this.settings = settings;
    }

    public LemmaTreeNode(LemmatizerSettings settings, ExampleList examples) {
        this(settings, examples, 0, examples.getSize() - 1, null);
    }

    /**
     *
     * @param settings
     * @param examples
     * @param start Index of the first word of the current group
     * @param end Index of the last word of the current group
     * @param parentNode
     */
    @SuppressWarnings("LeakingThisInConstructor")
    private LemmaTreeNode(LemmatizerSettings settings, ExampleList examples, int start, int end,
            LemmaTreeNode parentNode) {
        this(settings);
        this.parentNode = parentNode;
        this.subNodes = null;

        this.start = start;
        this.end = end;
        this.examples = examples;

        if (start >= examples.getSize() || end >= examples.getSize() || start > end) {
            bestRule = examples.getRules().getDefaultRule();
            bestRules = new WeightedRule[1];
            bestRules[0] = new WeightedRule(bestRule, 0);
            weight = 0;
            return;
        }

        int conditionLength = Math.min(parentNode == null ? 0 : parentNode.similarity + 1, examples.get(start).getWord().length());
        this.condition = examples.get(start).getWord().substring(examples.get(start).getWord().length() - conditionLength);
        this.similarity = examples.get(start).similarTo(examples.get(end));
        this.wholeWord = parentNode == null ? false
                : examples.get(end).getWord().length() == parentNode.similarity;

        findBestRules();
        addSubAll();

        //TODO check this heuristics, can be problematic when there are more applicable rules
        if (subNodes != null) {
            Map<Character, LemmaTreeNode> replaceNodes = new HashMap<Character, LemmaTreeNode>();
            for (Map.Entry<Character, LemmaTreeNode> child : subNodes.entrySet()) {
                if (child.getValue().subNodes != null && child.getValue().subNodes.size() == 1) {
                    Iterator<LemmaTreeNode> childChildIter = child.getValue().subNodes.values().iterator();
                    LemmaTreeNode childChild = childChildIter.next();
                    if (child.getValue().bestRule.equals(bestRule)) {
                        replaceNodes.put(child.getKey(), childChild);
                    }
                }
                for (Character kvpChild : replaceNodes.keySet()) {
                    subNodes.put(kvpChild, replaceNodes.get(kvpChild));
                    replaceNodes.get(kvpChild).parentNode = this;
                }
            }
        }
    }

    public int getTreeSize() {
        int count = 1;
        if (subNodes != null) {
            for (LemmaTreeNode child : subNodes.values()) {
                count += child.getTreeSize();
            }
        }
        return count;
    }

    public String getCondition() {
        return condition;
    }

    public double getWeight() {
        return weight;
    }

    private void findBestRules() {
        weight = 0;

        //calculate dWeight of whole node and calculates qualities for all rules
        Map<LemmaRule, Double> applicableRules = new HashMap<LemmaRule, Double>();
        //dictApplicableRules.Add(elExamples.Rules.DefaultRule, 0);
        while (applicableRules.isEmpty()) {
            for (int exm = start; exm <= end; exm++) {
                LemmaRule lr = examples.get(exm).getRule();
                double exmWeight = examples.get(exm).getWeight();
                weight += exmWeight;

                if (lr.isApplicableToGroup(condition.length())) {
                    if (applicableRules.containsKey(lr)) {
                        applicableRules.put(lr, applicableRules.get(lr) + exmWeight);
                    } else {
                        applicableRules.put(lr, exmWeight);
                    }
                }
            }
            //if none found then increase condition length or add some default appliable rule
            if (applicableRules.isEmpty()) {
                if (this.condition.length() < similarity) {
                    this.condition = examples.get(start).getWord().substring(examples.get(start).getWord().length() - (condition.length() + 1));
                } else {
                    //TODO preveri hevristiko, mogoce je bolje ce se doda default rule namesto rulea od starsa
                    applicableRules.put(parentNode.bestRule, 0d);
                }
            }
        }

        //TODO can optimize this step using sorted list (dont add if it's worse than the worst)
        List<WeightedRule> sortedRules = new ArrayList<WeightedRule>();
        for (Map.Entry<LemmaRule, Double> entry : applicableRules.entrySet()) {
            sortedRules.add(new WeightedRule(entry.getKey(), entry.getValue() / weight));
        }
        Collections.sort(sortedRules);

        //keep just best iMaxRulesPerNode rules
        int rulesCount = sortedRules.size();
        if (settings.getMaxRulesPerNode() > 0)
            rulesCount = Math.min(sortedRules.size(), settings.getMaxRulesPerNode());

        bestRules = new WeightedRule[rulesCount];
        for (int rule = 0; rule < rulesCount; rule++) {
            bestRules[rule] = sortedRules.get(rule);
        }

        //set best rule
        bestRule = bestRules[0].getRule();

        //TODO must check if this heuristics is OK (to privilige parent rule)
        if (parentNode != null) {
            for (int rule = 0; rule < sortedRules.size() && sortedRules.get(rule).getWeight() == sortedRules.get(0).getWeight(); rule++) {
                if (sortedRules.get(rule).getRule().equals(parentNode.bestRule)) {
                    bestRule = sortedRules.get(rule).getRule();
                    break;
                }
            }
        }
    }

    private void addSubAll() {
        int startGroup = start;
        char prevChar = '\0';
        boolean subGroupNeeded = false;

        for (int wrd = start; wrd <= end; wrd++) {
            String word = examples.get(wrd).getWord();

            char thisChar = word.length() > similarity ? word.charAt(word.length() - 1 - similarity) : '\0';

            if (wrd != start && prevChar != thisChar) {
                if (subGroupNeeded) {
                    addSub(startGroup, wrd - 1, prevChar);
                    subGroupNeeded = false;
                }
                startGroup = wrd;
            }

            //TODO check out bSubGroupNeeded when there are multiple posible rules (not just lrBestRule)
            if (!examples.get(wrd).getRule().equals(bestRule)) {
                subGroupNeeded = true;
            }

            prevChar = thisChar;
        }
        if (subGroupNeeded && startGroup != start) addSub(startGroup, end, prevChar);
    }

    private void addSub(int start, int end, char ch) {
        LemmaTreeNode sub = new LemmaTreeNode(settings, examples, start, end, this);

        //TODO - maybe not realy appropriate because loosing statisitcs from multiple possible rules
        if (sub.bestRule.equals(bestRule) && sub.subNodes == null) return;

        if (subNodes == null) {
            subNodes = new HashMap<Character, LemmaTreeNode>();
        }
        subNodes.put(ch, sub);
    }

    public boolean conditionSatisfied(CharSequence word) {
        //if (bWholeWord)
        //    return sWord == sCondition;
        //else 
        //    return sWord.EndsWith(sCondition);

        int diff = word.length() - condition.length();
        if (diff < 0 || (wholeWord && diff > 0)) return false;

        int wrdEnd = condition.length() - parentNode.condition.length() - 1;
        for (int idx = 0; idx < wrdEnd; idx++) {
            if (condition.charAt(idx) != word.charAt(idx + diff)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public CharSequence lemmatize(CharSequence word) {
        if (word.length() >= similarity && subNodes != null) {
            char ch = word.length() > similarity ? word.charAt(word.length() - 1 - similarity) : '\0';
            if (subNodes.containsKey(ch) && subNodes.get(ch).conditionSatisfied(word)) {
                return subNodes.get(ch).lemmatize(word);
            }
        }
        return bestRule.lemmatize(word);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb, 0);
        return sb.toString();
    }

    private void toString(StringBuilder sb, int level) {
        sb.append(new String(new char[level]).replace('\0', '\t'));
        sb.append("Suffix=\"").append(wholeWord ? "^" : "").append(condition).append("\"; ");
        sb.append("Rule=\"").append(bestRule.toString()).append("\"; ");
        sb.append("Weight=").append(weight).append("\"; ");
        if (bestRules != null && bestRules.length > 0)
            sb.append("Cover=").append(bestRules[0].getWeight()).append("; ");
        sb.append("Rulles=");
        if (bestRules != null) {
            for (WeightedRule wr : bestRules) {
                sb.append(" ").append(wr.toString());
            }
            sb.append("; ");
        }

        sb.append("\n");

        if (subNodes != null) {
            for (LemmaTreeNode child : subNodes.values()) {
                child.toString(sb, level + 1);
            }
        }
    }

    public void writeObject(ObjectOutput out) throws IOException {
        out.writeBoolean(subNodes != null);
        if (subNodes != null) {
            out.writeInt(subNodes.size());
            for (Map.Entry<Character, LemmaTreeNode> kvp : subNodes.entrySet()) {
                out.writeChar(kvp.getKey());
                kvp.getValue().writeObject(out);
            }
        }

        out.writeInt(similarity);
        writeString(out, condition);
        out.writeBoolean(wholeWord);

        writeString(out, bestRule.getSignature());
        out.writeInt(bestRules.length);
        for (WeightedRule rule : bestRules) {
            writeString(out, rule.getRule().getSignature());
            out.writeDouble(rule.getWeight());
        }
        out.writeDouble(weight);

        out.writeInt(start);
        out.writeInt(end);
    }

    public LemmaTreeNode(ObjectInput in, LemmatizerSettings settings, ExampleList examples,
            LemmaTreeNode parentNode) throws IOException, ClassNotFoundException {
        readObject(in, settings, examples, parentNode);
    }

    private void readObject(ObjectInput in, LemmatizerSettings settings, ExampleList examples,
            LemmaTreeNode parentNode) throws IOException, ClassNotFoundException {
        this.settings = settings;
        if (in.readBoolean()) {
            subNodes = new HashMap<Character, LemmaTreeNode>();
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                char ch = in.readChar();
                LemmaTreeNode ltrSub = new LemmaTreeNode(in, this.settings, examples, this);
                subNodes.put(ch, ltrSub);
            }
        } else {
            subNodes = null;
        }

        this.parentNode = parentNode;

        similarity = in.readInt();
        condition = readString(in);
        wholeWord = in.readBoolean();

        String br = readString(in);
        bestRule = examples.getRules().get(br);

        int iCountBest = in.readInt();
        bestRules = new WeightedRule[iCountBest];
        for (int i = 0; i < iCountBest; i++) {
            bestRules[i] = new WeightedRule(examples.getRules().get(readString(in)), in.readDouble());
        }

        weight = in.readDouble();

        start = in.readInt();
        end = in.readInt();
        this.examples = examples;
    }

}
