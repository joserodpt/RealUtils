package joserodpt.realutils.gui;

/*
 * RealUtils - shared utilities for the Real* plugins
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2026
 * @link https://github.com/joserodpt/RealUtils
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** A list read a page at a time, for inventory screens that show so many entries per page. */
public class Pagination<T> extends ArrayList<T> {

    private final int pageSize;

    public Pagination(final int pageSize) {
        this(pageSize, new ArrayList<>());
    }

    @SafeVarargs
    public Pagination(final int pageSize, final T... objects) {
        this(pageSize, Arrays.asList(objects));
    }

    public Pagination(final int pageSize, final Collection<T> objects) {
        this.pageSize = pageSize;
        this.addAll(objects);
    }

    public int pageSize() {
        return this.pageSize;
    }

    public int totalPages() {
        return (int) Math.ceil((double) this.size() / this.pageSize);
    }

    public boolean exists(final int page) {
        return page >= 0 && page < this.totalPages();
    }

    /** @throws IndexOutOfBoundsException for a page that does not exist, which is what an empty list has */
    public List<T> getPage(final int page) {
        if (page < 0 || page >= this.totalPages()) {
            throw new IndexOutOfBoundsException("Page: " + page + ", Size: " + this.totalPages());
        }
        final int min = page * this.pageSize;
        final int max = Math.min(min + this.pageSize, this.size());
        return new ArrayList<>(this.subList(min, max));
    }

    @Override
    public String toString() {
        return "Pagination{pageSize=" + this.pageSize + ", size=" + this.size() + '}';
    }
}
