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
