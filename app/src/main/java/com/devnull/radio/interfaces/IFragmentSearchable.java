package com.devnull.radio.interfaces;

import com.devnull.radio.station.StationsFilter;

public interface IFragmentSearchable {
    void Search(StationsFilter.SearchStyle searchStyle, String query);
}
