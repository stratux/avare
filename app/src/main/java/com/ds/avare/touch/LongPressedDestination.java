package com.ds.avare.touch;

/**
 * Created by pasniak on 12/4/2016.
 */
public class LongPressedDestination implements Comparable<LongPressedDestination> {
    private String id, type;
    private double distance;
    private double lat, lon;
    private String weatherColor = "#ffffff";
    public LongPressedDestination(String id, String type) {
        this.id = id;
        this.type = type;
        this.distance = 0.0;
    }

    public LongPressedDestination(String id, String type, double distance, double lat, double lon) {
        this.id = id;
        this.type = type;
        this.distance = distance;
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getWeatherColor() {
        return weatherColor;
    }

    public void setWeatherColor(String weatherColor) {
        this.weatherColor = weatherColor;
    }

    public String getLatLonString() {
        return lat + "&" + lon;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String toJSON() {
        String json = "{";
        json += "\"id\":\"" + this.id + "\",";
        json += "\"type\":\"" + this.type + "\",";
        json += "\"distance\":\"" + this.distance + "\",";
        json += "\"weatherColor\":\"" + this.weatherColor + "\"";
        json += "}";
        return json;
    }

    public int compareTo(LongPressedDestination other) {
        if (this.distance < other.getDistance()) {
            return -1;
        } else {
            return 1;
        }
    }
}
