package com.taha.accessiblejourneyplanner.ui.stopsearch;

/**
 * Display model for one stop search result: id (NaPTAN), name, modes string.
 */
public class StopSearchItem {
    public final String id;
    public final String name;
    public final String modesStr;

    public StopSearchItem(String id, String name, String modesStr) {
        this.id = id != null ? id : "";
        this.name = name != null ? name : "";
        this.modesStr = modesStr != null ? modesStr : "";
    }
}
