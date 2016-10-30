package com.ds.avare;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Button;

import com.ds.avare.animation.TwoButton;
import com.ds.avare.instruments.CDI;
import com.ds.avare.message.Logger;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Plan;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.MetarFlightCategory;

import org.java_websocket.drafts.Draft_17;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.util.LinkedList;

import java.net.URI;
import java.net.URISyntaxException;

import com.ds.avare.utils.StratuxNEXRADEntryType;
import com.ds.avare.utils.StratuxRawType;
import com.ds.avare.utils.StratuxSituationType;
import com.ds.avare.utils.StratuxTrafficType;
import com.ds.avare.utils.StratuxWeatherType;
import com.google.gson.Gson;

import static android.R.attr.type;
import static java.lang.Thread.sleep;
import com.google.gson.reflect.TypeToken;

/**
 * Created by jimdevel on 10/18/2016.
 */

public class IWebsocketService extends Service {
    public static final int MIN_ALTITUDE = -1000;
    public static final int INTENSITY[] = {
            0x00000000,
            0x00000000,
            0xFF007F00, // dark green
            0xFF00AF00, // light green
            0xFF00FF00, // lighter green
            0xFFFFFF00, // yellow
            0xFFFF7F00, // orange
            0xFFFF0000  // red
    };
    private StorageService mService;
    private JSONObject mGeoAltitude;
    private Preferences mPref;
    WebSocketClient client;


    private ServiceConnection mConnection = new ServiceConnection() {

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName, android.os.IBinder)
         */
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            /*
             * We've bound to LocalService, cast the IBinder and get LocalService instance
             */
            StorageService.LocalBinder binder = (StorageService.LocalBinder)service;
            mService = binder.getService();
            mGeoAltitude = null;
            mPref = new Preferences(getApplicationContext());
        }

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };
    private void connectWebSocket() {
        URI uri;
        String mConnectAddr;

        if (mPref != null) {
            mConnectAddr = mPref.getStratuxIpAddress();
        }
        else
        {
            mConnectAddr = "192.168.10.1";
        }
        String jsonAddr = "ws://" + mConnectAddr + "/jsonio";
        try {
            uri = new URI(jsonAddr);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        client = new WebSocketClient(uri, new Draft_17()) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                client.send("Hello from websocket");
            }

            @Override
            public void onMessage(String s) {
                Message msg = mHandlerWeb.obtainMessage();
                final String message = s;
                msg.obj = s;
                mHandlerWeb.sendMessage(msg);
            }

            @Override
            public void onClose(int i, String s, boolean b) {
//                SystemClock.sleep(1000);
                connectWebSocket();
            }

            @Override
            public void onError(Exception e) {
                client.close();
            }
        };
        client.connect();
    }

    @Override
    public void onCreate() {
        URI uri;
        mService = null;
        Intent intent = new Intent(this, StorageService.class);
        getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mPref = new Preferences(getApplicationContext());
        connectWebSocket();
    }

    @Override
    public void onDestroy() {
        getApplicationContext().unbindService(mConnection);
        mService = null;
    }
    /**
     *
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mWebsocket;
    }

    private final IWebsocket.Stub mWebsocket = new IWebsocket.Stub() {
        @Override
        public void sendDataText(String text) {
            Message msg = mHandlerWeb.obtainMessage();
            msg.obj = text;
            mHandlerWeb.sendMessage(msg);
        }

        @Override
        /**
         *
         */
        public String recvDataText() {
            return null;
        }

    };


    public void HandleRawDataMessage(JSONObject object)
    {
        try {
            final int iMonth = object.getInt("FISB_month");
            final int iDay = object.getInt("FISB_day");
            final int iHours = object.getInt("FISB_hours");
            final int iMin = object.getInt("FISB_minutes");
            final int iSec = object.getInt("FISB_seconds");
            final int iFISBLen = object.getInt("FISB_length");
            final int iProductId = object.getInt("Product_id");

            switch(iProductId) {
                case 63:
                case 64:
                    JSONArray jArray = object.getJSONArray("NEXRAD");
                    if (jArray != null) {
                            int lenval = jArray.length();
                        for (int i = 0; i < lenval; i++) {
                            JSONObject jobj2 = jArray.getJSONObject(i);
                            final int mBlock = jobj2.getInt("Block");
                            final int rType=jobj2.getInt("Radar_Type");
                            final int Scale=jobj2.getInt("Scale");
                            final double LatNorth = jobj2.getDouble("LatNorth");
                            final double LonWest = jobj2.getDouble("LonWest");
                            final double Height = jobj2.getDouble("Height");
                            final double Width = jobj2.getDouble("Width");
                            final boolean isEmpty = jobj2.getBoolean("IsEmpty");
                            final JSONArray jDataArray = jobj2.getJSONArray("Data");
                            int IntensityLen = jDataArray.length();
                            int IntensityArray[] = new int[IntensityLen];
                            for (int j=0; j<IntensityLen; j++) {
                                if (isEmpty == false) {
                                    IntensityArray[j] = INTENSITY[jDataArray.getInt(j) & 7];
                                } else {
                                    IntensityArray[j] = jDataArray.getInt(j);
                                }
                            }
                            long time = Helper.getMillisGMT();//object.getLong("time");
                            boolean conus = (rType == 64) ? true : false;

                            if (isEmpty) {
                                mService.getAdsbWeather().putImg(
                                        time, mBlock, IntensityArray, conus, null, 32, 4);
                            } else {
                                mService.getAdsbWeather().putImg(
                                        time, mBlock, null, conus, IntensityArray, 32, 4);
                            }

                        }
                    }
                    break;
            }
        } catch (JSONException e) {
            Log.e("IWebsocketService", e.getMessage());
        }
    }

    public void HandleNEXRADMessage(JSONObject object)
    {
        try {
            String data = object.getString("Data");

            // The split data is in nexData
            String nexData[] = data.split(" ");
            if (nexData.length > 4)
            {
                int conusval = Integer.parseInt(nexData[0]);
                boolean conus = (conusval == 0) ? false : true;
                int mBlock = Integer.parseInt(nexData[1]);
                int cols = Integer.parseInt(nexData[2]);
                int rows = Integer.parseInt(nexData[3]);
                int elementval = Integer.parseInt(nexData[4]);
                String sData[] = nexData[5].split(",");
                long time = Helper.getMillisGMT();//object.getLong("time");
                int[] mData = new int[sData.length];
                for(int i = 0;i < sData.length;i++)
                {
                    // Note that this is assuming valid input
                    // If you want to check then add a try/catch
                    // and another index for the numbers if to continue adding the others
                    int mInt =Integer.parseInt(sData[i]);
                    if (elementval != 0)
                        mData[i] = INTENSITY[mInt & 0x07];
                    else
                        mData[i] = mInt;
                }
                //public void putImg(long time, int block, int empty[], boolean isConus, int data[], int cols, int rows)
                if (elementval != 0) {
                    mService.getAdsbWeather().putImg(time, mBlock, null, conus, mData, cols, rows);
                }
                else
                {
                    mService.getAdsbWeather().putImg(time, mBlock, mData, conus, null, cols, rows);
                }
            }


        } catch (JSONException e) {
            Log.e("IWebsocketService", e.getMessage());

        }
    }





    public void HandleWeatherMessage(StratuxWeatherType wx) {
        double lon = 0;
        double lat = 0;
        double elev = 0;
        int tisid;
        Long Ticks;

        try {
            String AllData = wx.Time+" "+wx.Data;


            lon = wx.TowerLon;
            lat = wx.TowerLat;
            tisid = wx.TisId;
            Ticks = wx.Ticks;

            mService.getAdsbWeather().putUatTower(Ticks, lon, lat, tisid);


/*
            if(type.equals("NEXRAD")) {
                // Raw UAT handler here
                HandleNEXRADMessage(object);
            }
*/
            if(wx.Type.equals("METAR") || wx.Type.equals("SPECI")) {
                        /*
                         * Put METAR
                         */

                String category = MetarFlightCategory.getFlightCategory(wx.Location,AllData);

                mService.getAdsbWeather().putMetar(Ticks,
                        wx.Location, AllData, category); //object.getString("flight_category"));
            }
            if(wx.Type.equals("WINDS")) {

//                AllData = object.getString("Location") + " " + object.getString("Time")+" "+object.getString("Data");
                String tokens[] = AllData.split("\n");
                if(tokens.length < 2) {
                                    /*
                                     * Must have line like
                                     * MSY 230000Z  FT 3000 6000    F9000   C12000  G18000  C24000  C30000  D34000  39000   Y
                                     * and second line like
                                     * 1410 2508+10 2521+07 2620+01 3037-12 3041-26 304843 295251 29765
                                     */
                }

                tokens[0] = tokens[0].replaceAll("\\s+", " ");
                tokens[1] = tokens[1].replaceAll("\\s+", " ");
                String winds[] = tokens[1].split(" ");
                String alts[] = tokens[0].split(" ");

                                /*
                                 * Start from 3rd entry - alts
                                 */
                AllData = "";
                boolean found = false;
                for(int i = 2; i < alts.length; i++) {
                    if(alts[i].contains("3000") && !alts[i].contains("30000")) {
                        AllData += winds[i - 2] + ",";
                        found = true;
                    }
                }
                if(!found) {
                    AllData += ",";
                }
                found = false;
                for(int i = 2; i < alts.length; i++) {
                    if(alts[i].contains("6000")) {
                        AllData += winds[i - 2] + ",";
                        found = true;
                    }
                }
                if(!found) {
                    AllData += ",";
                }
                found = false;
                for(int i = 2; i < alts.length; i++) {
                    if(alts[i].contains("9000") && !alts[i].contains("39000")) {
                        AllData += winds[i - 2] + ",";
                        found = true;
                    }
                }
                if(!found) {
                    AllData += ",";
                }
                found = false;
                for(int i = 2; i < alts.length; i++) {
                    if(alts[i].contains("12000")) {
                        AllData += winds[i - 2] + ",";
                        found = true;
                    }
                }
                if(!found) {
                    AllData += ",";
                }
                found = false;
                for(int i = 2; i < alts.length; i++) {
                    if(alts[i].contains("18000")) {
                        AllData += winds[i - 2] + ",";
                        found = true;
                    }
                }
                if(!found) {
                    AllData += ",";
                }
                found = false;
                for(int i = 2; i < alts.length; i++) {
                    if(alts[i].contains("24000")) {
                        AllData += winds[i - 2] + ",";
                        found = true;
                    }
                }
                if(!found) {
                    AllData += ",";
                }
                found = false;
                for(int i = 2; i < alts.length; i++) {
                    if(alts[i].contains("30000")) {
                        AllData += winds[i - 2] + ",";
                        found = true;
                    }
                }
                if(!found) {
                    AllData += ",";
                }
                found = false;
                for(int i = 2; i < alts.length; i++) {
                    if(alts[i].contains("34000")) {
                        AllData += winds[i - 2] + ",";
                        found = true;
                    }
                }
                if(!found) {
                    AllData += ",";
                }
                found = false;
                for(int i = 2; i < alts.length; i++) {
                    if(alts[i].contains("39000")) {
                        AllData += winds[i - 2] + ",";
                        found = true;
                    }
                }
                if(!found) {
                    AllData += ",";
                }
                mService.getAdsbWeather().putWinds(Ticks,
                        wx.Location, AllData);

            }
            else if(wx.Type.equals("TAF") || wx.Type.equals("TAF.AMD")) {
                mService.getAdsbWeather().putTaf(Ticks,
                        wx.Location, AllData);
            }
            else if(wx.Type.equals("PIREP")) {
                mService.getAdsbWeather().putAirep(Ticks,
                        wx.Location, AllData, mService.getDBResource());
            }
            else {
                Logger.Logit("Unhandled type from stratux: "+type);
            }


        } catch (Exception e) {
            Log.e("IWebsocketService", e.getMessage());

        }
    }

    /*
    object.put("type", "ownship");
    object.put("longitude", (double)om.mLon);
    object.put("latitude", (double)om.mLat);
    object.put("speed", (double)(om.mHorizontalVelocity));
    object.put("bearing", (double)om.mDirection);
    object.put("time", (long)om.getTime());
    object.put("altitude", (double) om.mAltitude);

 */
    public void HandleSituationMessage(StratuxSituationType situation) {
        try {

            long timeticks = Helper.getMillisGMT();
            Location l = new Location(LocationManager.GPS_PROVIDER);
            l.setLongitude(situation.Lng);
            l.setLatitude(situation.Lat);
            l.setSpeed((float) situation.GroundSpeed);
            l.setBearing((float) situation.TrueCourse);

            // TODO: We need to covert time from the message
            //l.setTime(object.getLong("time"));
            l.setTime(timeticks);

            // Choose most appropriate altitude. This is because people fly all sorts
            // of equipment with or without altitudes
            // convert all altitudes in feet
            final double meterAltitude = situation.Alt * 0.3048;
            final double pressureAltitude = meterAltitude * Preferences.heightConversion;
            double deviceAltitude = MIN_ALTITUDE;
            double geoAltitude = MIN_ALTITUDE;
            // If geo altitude from adsb available, use it if not too old
            if(mGeoAltitude != null) {
                long t1 = System.currentTimeMillis();
                long t2 = mGeoAltitude.getLong("time");
                if((t1 - t2) < 10000) { // 10 seconds
                    geoAltitude = mGeoAltitude.getDouble("Alt") * Preferences.heightConversion;
                    if(geoAltitude < MIN_ALTITUDE) {
                        geoAltitude = MIN_ALTITUDE;
                    }
                }
            }
            // If geo altitude from device available, use it if not too old
            if(mService.getGpsParams() != null) {
                long t1 = System.currentTimeMillis();
                long t2 = mService.getGpsParams().getTime();
                if ((t1 - t2) < 10000) { // 10 seconds
                    deviceAltitude = mService.getGpsParams().getAltitude();
                    if(deviceAltitude < MIN_ALTITUDE) {
                        deviceAltitude = MIN_ALTITUDE;
                    }
                }
            }

            // choose best altitude. give preference to pressure altitude because that is
            // the most correct for traffic purpose.
            double alt = pressureAltitude;
            if(alt <= MIN_ALTITUDE) {
                alt = geoAltitude;
            }
            if(alt <= MIN_ALTITUDE) {
                alt = deviceAltitude;
            }
            if(alt <= MIN_ALTITUDE) {
                alt = MIN_ALTITUDE;
            }

            // set pressure altitude for traffic alerts
            mService.getTrafficCache().setOwnAltitude((int) alt);

            // For own height prefer geo altitude, do not use deviceAltitude here because
            // we could get into rising altitude condition through feedback
            alt = geoAltitude;
            if(alt <= MIN_ALTITUDE) {
                alt = pressureAltitude;
            }
            if(alt <= MIN_ALTITUDE) {
                alt = MIN_ALTITUDE;
            }
            l.setAltitude(alt / Preferences.heightConversion);
            mService.getGps().onLocationChanged(l, "ownship");

        } catch (Exception e) {
            Log.e("IWebsocketService", e.getMessage());

        }
    }

    public Handler mHandlerWeb = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            String text = (String)msg.obj;
            Gson gson = new Gson();
            if(text == null || mService == null) {
                return;
            }

            /*
             * Get JSON
             */
            try {
                JSONObject object = new JSONObject(text);
                String type = object.getString("Type");

                if(type.equals("situation")) {
                    StratuxSituationType situation = gson.fromJson(text, StratuxSituationType.class);
                    HandleSituationMessage(situation);
                } else if(type.equals("Raw")) {
                    HandleRawDataMessage(object);
/*
                    Integer pid = object.getInt("Product_id");
                    if ((pid == 63) || (pid == 64)) {
                        StratuxRawType NEXRAD = gson.fromJson(text, StratuxRawType.class);
                        if (NEXRAD.NEXRAD != null) {

                        }
                    }
*/
                }
                else if(type.equals("traffic")) {

                    try {
                        StratuxTrafficType traffic = gson.fromJson(text, StratuxTrafficType.class);
                        mService.getTrafficCache().putTraffic(
                                traffic.Tail,
                                traffic.Icao_addr,
                                traffic.Lat, traffic.Lng,
                                traffic.Alt, traffic.Track,
                                traffic.Speed,
                                Helper.getMillisGMT()
                            /*XXX:object.getLong("time")*/);
                    } catch(Exception e)
                    {
                        Log.e("IWebsocketService", e.getMessage());
                    }
                } else  if (type.length() > 1) {
                    try {

                        StratuxWeatherType wx = gson.fromJson(text, StratuxWeatherType.class);
                        HandleWeatherMessage(wx);
                    } catch(Exception e) {
                        Log.e("IWebsocketService", e.getMessage());
                    }
//                    HandleWeatherMessage(type, object);
                }

            } catch (JSONException e) {
                Log.e("IWebsocketService", e.getMessage());
            }
        }
    };

}
