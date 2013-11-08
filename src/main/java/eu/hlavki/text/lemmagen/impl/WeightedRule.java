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

/**
 *
 * @author Michal Hlavac <hlavki@hlavki.eu>
 */
public class WeightedRule implements Comparable<WeightedRule> {

    private final LemmaRule rule;
    private final double weight;

    public WeightedRule(LemmaRule lemmaRule, double weight) {
        this.rule = lemmaRule;
        this.weight = weight;
    }

    public LemmaRule getRule() {
        return rule;
    }

    public double getWeight() {
        return weight;
    }

    @Override
    public int compareTo(WeightedRule o) {
        if (this.weight < o.weight) return 1;
        if (this.weight > o.weight) return -1;
        if (this.rule.getId() < o.rule.getId()) return 1;
        if (this.rule.getId() > o.rule.getId()) return -1;
        return 0;
    }

    @Override
    public String toString() {
        return rule.toString() + "(" + String.format("%.2f", weight * 100) + "%)";
    }
}
