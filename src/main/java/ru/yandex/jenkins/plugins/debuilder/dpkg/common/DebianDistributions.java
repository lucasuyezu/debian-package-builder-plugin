package ru.yandex.jenkins.plugins.debuilder.dpkg.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the debian distributions
 * 
 * @author caiocezar
 * 
 */
public class DebianDistributions {
    private ArrayList<String> distributions;
    /**
     * The regex that valid the distribution name "^([-+0-9a-z.]+)$". This
     * pattern has copied for the perl module Dpkg::Changelog::Entry::Debian
     * version 1.00
     */
    public static String DISTRIBUTION_PATTERN = "^([-+0-9a-z.]+)$";

    /**
     * @return a copy of the distributions preserving the object integrity, or
     *         null
     */
    public ArrayList<String> getCopy() {
        if (this.distributions == null)
            return null;

        return new ArrayList<String>(distributions);
    }

    /**
     * Add a distribution to the current array or initialize a new one;
     * 
     * @param distribution
     *            The a valid distribution
     * @return True if added, false if distribution is invalid (
     *         {@link #isValid(String)})
     */
    public boolean add(String distribution) {
        if (this.distributions == null)
            this.distributions = new ArrayList<String>();

        if (!isValid(distribution))
            return false;

        this.distributions.add(distribution);
        return true;
    }

    /**
     * Add a valid list of distributions using {@link #add(String)}
     * 
     * @param distributions
     *            The distributions
     * @return An empty List if all distributions have been imported or return
     *         the reject distributions, never null. (See {@link #add(String)})
     */
    public List<String> addAll(List<String> distributions) {
        List<String> rejected = new ArrayList<String>();
        for (String dist : distributions)
            if (!add(dist))
                rejected.add(dist);
        return rejected;
    }

    /**
     * Clear the current list and set all the valid distributions using
     * {@link #addAll(List)}
     * 
     * @param distributions
     *            The distributions to set
     * @return Same as {@link #addAll(List)}
     */
    public List<String> setDistributions(List<String> distributions) {
        this.distributions = null;
        return addAll(distributions);
    }

    /**
     * Add a distribution if not already present
     * 
     * @param distribution
     *            The distribution
     * @return True if the merge was successful, even if the entry was already
     *         present, false if the
     */
    public boolean merge(String distribution) {
        if (this.distributions != null && this.distributions.contains(distribution))
            return true;

        if (add(distribution))
            return true;
        return false;
    }

    /**
     * Add all distributions if not already present
     * 
     * @param distributions
     *            The distributions
     * @return An empty List if all distributions have been merged or return the
     *         reject distributions, never null. (See {@link #merge(String)})
     */
    public List<String> mergeAll(List<String> distributions) {
        List<String> rejected = new ArrayList<String>();

        if (this.distributions != null) {
            for (String dist : this.distributions)
                if (!merge(dist))
                    rejected.add(dist);
        } else {
            addAll(distributions);
        }

        return rejected;
    }

    /**
     * Same as {@link ArrayList#size()}
     */
    public int size() {
        return distributions.size();
    }

    /**
     * Same as {@link ArrayList#remove(Object)}
     */
    public boolean remove(Object o) {
        return this.distributions.remove(o);
    }

    /**
     * Same as {@link ArrayList#contains(Object)}
     */
    public boolean contains(Object o) {
        return distributions.contains(o);
    }

    /**
     * Parse if the distribution is valid. The distribution should be not null
     * and must only match the {@link #DISTRIBUTION_PATTERN}
     * 
     * @param distribution
     *            The distribution
     * @return True if valid false if not
     */
    public static boolean isValid(String distribution) {
        if (distribution == null || !distribution.matches(DISTRIBUTION_PATTERN))
            return false;
        return true;
    }

}
