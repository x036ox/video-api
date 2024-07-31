package com.artur.youtback.utils.comparators;

import com.artur.common.entity.SearchHistory;

import java.util.Comparator;

public class SearchHistoryComparator implements Comparator<SearchHistory> {

    @Override
    public int compare(SearchHistory o1, SearchHistory o2) {
        return Long.signum(o2.getDateAdded() - o1.getDateAdded());
    }
}
