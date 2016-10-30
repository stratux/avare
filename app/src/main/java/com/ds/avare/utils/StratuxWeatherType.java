package com.ds.avare.utils;

/**
 * Created by jimdevel on 10/27/2016.
 */

//{"Type":"METAR","Location":"KUSE","Time":"280335Z","Data":"AUTO 26005KT 10SM OVC035 07/04 A3026 RMK AO2=\n","LocaltimeReceived":"0001-01-01T21:29:12.25Z","Ticks":1448854500000,
// "TowerLon":-87.83689498901367,"TowerLat":42.657766342163086,"TisId":0}

public class StratuxWeatherType {
    public String Type;
    public String Location;
    public String Time;
    public String Data;
    public String LocaltimeReceived;
    public Long Ticks;
    public Double TowerLon;
    public Double TowerLat;
    public Integer TisId;
}
