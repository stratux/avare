package com.ds.avare;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
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

import com.google.gson.Gson;
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
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
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

//            connectWebSocket();
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

    public void HandleNEXRADFrame(int product_id, byte[] msg, int len) {
        boolean elementIdentifier = (((int)msg[0]) & 0x80) != 0; // RLE or Empty?
        int mBlock;
        int mData[];
        LinkedList<Integer> mEmpty;
        int index = 3;
        boolean conus;

        if (product_id == 64)
            conus = true;
        else
            conus = false;
        mBlock = ((int)msg[0] & 0x0F) << 16;
        mBlock += (((int)msg[1] & 0xFF) << 8);
        mBlock += (int)msg[2] & 0xFF;
        /*
         * Decode blocks RLE encoded
         */
        if(elementIdentifier) {
            mData = new int[32 * 4];
            mEmpty = null;

            /*
             * Each row element is 1 minute (4 minutes total)
             * Each col element is 1.5 minute (48 minutes total)
             */
            for (int i = 0; i < 32 * 4; i++) {
                mData[i] = INTENSITY[0];
            }

            int j = 0;
            int i;
            while(index < len) {
                int numberOfBins = ((msg[index] & 0xF8) >> 3) + 1;
                for(i = 0; i < numberOfBins; i++) {
                    if(j >= mData.length) {
                        /*
                         * Some sort of error.
                         */
                        mData = null;
                        return;
                    }
                    mData[j] = INTENSITY[(msg[index] & 0x07)];
                    j++;
                }
                index++;
            }
        }
        else {
            /*
             * Make a list of empty blocks
             */
            mData = null;
            mEmpty = new LinkedList<Integer>();
            mEmpty.add(mBlock);
            int bitmaplen = (int)msg[index] & 0x0F;

            if(((int)msg[index] & 0x10) != 0) {
                mEmpty.add(mBlock + 1);
            }

            if(((int)msg[index] & 0x20) != 0) {
                mEmpty.add(mBlock + 2);
            }

            if(((int)msg[index] & 0x30) != 0) {
                mEmpty.add(mBlock + 3);
            }

            if(((int)msg[index] & 0x40) != 0) {
                mEmpty.add(mBlock + 4);
            }

            for(int i = 1; i < bitmaplen; i++) {
                if(((int)msg[index + i] & 0x01) != 0) {
                    mEmpty.add(mBlock + i * 8 - 3);
                }

                if(((int)msg[index + i] & 0x02) != 0) {
                    mEmpty.add(mBlock + i * 8 - 2);
                }

                if(((int)msg[index + i] & 0x04) != 0) {
                    mEmpty.add(mBlock + i * 8 - 1);
                }

                if(((int)msg[index + i] & 0x08) != 0) {
                    mEmpty.add(mBlock + i * 8 - 0);
                }

                if(((int)msg[index + i] & 0x10) != 0) {
                    mEmpty.add(mBlock + i * 8 + 1);
                }

                if(((int)msg[index + i] & 0x20) != 0) {
                    mEmpty.add(mBlock + i * 8 + 2);
                }

                if(((int)msg[index + i] & 0x40) != 0) {
                    mEmpty.add(mBlock + i * 8 + 3);
                }

                if(((int)msg[index + i] & 0x80) != 0) {
                    mEmpty.add(mBlock + i * 8 + 4);
                }
            }
        }
                  /*
                     * XXX: If we are getting this from station, it must be current, fix this.
                     */
        long time = Helper.getMillisGMT();//object.getLong("time");

        int empty[];
        if (mEmpty != null)
        {
            empty = new int[mEmpty.size()];
            for(int i = 0; i < mEmpty.size(); i++) {
                empty[i] = mEmpty.get(i);
            }
        }
        else
        {
            empty = null;
        }

                    /*
                     * Put in nexrad.
                     */
        mService.getAdsbWeather().putImg(
                time, mBlock, empty, conus, mData, 32, 4);
    }

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
                        for (int i = 0; i < jArray.length(); i++) {
                            JSONObject jobj2 = jArray.getJSONObject(i);
                            final int mBlock = jobj2.getInt("Block");
                            final int rType=jobj2.getInt("Radar_Type");
                            final int Scale=jobj2.getInt("Scale");
                            final double LatNorth = jobj2.getDouble("LatNorth");
                            final double LonWest = jobj2.getDouble("LonWest");
                            final double Height = jobj2.getDouble("Height");
                            final double Width = jobj2.getDouble("Width");
                            final JSONArray jIntensityArray = jobj2.getJSONArray("Intensity");
                            int IntensityLen = jIntensityArray.length();
                            int IntensityArray[] = new int[IntensityLen];
                            for (int j=0; j<IntensityLen; j++) {
                                IntensityArray[j] = INTENSITY[jIntensityArray.getInt(j) & 7];
                            long time = Helper.getMillisGMT();//object.getLong("time");
                            boolean conus = (rType == 64) ? true : false;
                            mService.getAdsbWeather().putImg(
                                    time, mBlock, null, conus, IntensityArray, 32, 4);
                            }
                        }
                    }
                    break;
            }
        } catch (JSONException e) {
            return;
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
            return;
        }
    }



    public void HandleWeatherMessage(String type, JSONObject object) {
        double lon = 0;
        double lat = 0;
        double elev = 0;
        int tisid;


        try {
            String AllData = object.getString("Time")+" "+object.getString("Data");

            lon = object.getDouble("TowerLon");
            lat = object.getDouble("TowerLat");
            tisid = object.getInt("TisId");
            mService.getAdsbWeather().putUatTower(object.getLong("Ticks"), lon, lat, tisid);


/*
            if(type.equals("NEXRAD")) {
                // Raw UAT handler here
                HandleNEXRADMessage(object);
            }
*/
            if(type.equals("METAR") || type.equals("SPECI")) {
                        /*
                         * Put METAR
                         */

                String category = MetarFlightCategory.getFlightCategory(object.getString("Location"),AllData);

                mService.getAdsbWeather().putMetar(object.getLong("Ticks"),
                        object.getString("Location"), AllData, category); //object.getString("flight_category"));
            }
            if(type.equals("WINDS")) {

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
                mService.getAdsbWeather().putWinds(object.getLong("Ticks"),
                        object.getString("location"), AllData);

            }
            else if(type.equals("TAF") || type.equals("TAF.AMD")) {
                mService.getAdsbWeather().putTaf(object.getLong("Ticks"),
                        object.getString("Location"), AllData);
            }
            else if(type.equals("PIREP")) {
                mService.getAdsbWeather().putAirep(object.getLong("Ticks"),
                        object.getString("Location"), AllData, mService.getDBResource());
            }
            else {
                Logger.Logit("Unhandled type from stratux: "+type);
            }


        } catch (JSONException e) {
            return;
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
    public void HandleSituationMessage(JSONObject object) {
        try {

            long timeticks = Helper.getMillisGMT();
            Location l = new Location(LocationManager.GPS_PROVIDER);
            l.setLongitude(object.getDouble("Lng"));
            l.setLatitude(object.getDouble("Lat"));
            l.setSpeed((float) object.getDouble("GroundSpeed"));
            l.setBearing((float) object.getDouble("TrueCourse"));

            // TODO: We need to covert time from the message
            //l.setTime(object.getLong("time"));
            l.setTime(timeticks);

            // Choose most appropriate altitude. This is because people fly all sorts
            // of equipment with or without altitudes
            // convert all altitudes in feet
            final double meterAltitude = (object.getDouble("Alt") * 0.3048);
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

        } catch (JSONException e) {
            return;
        }
    }

    public Handler mHandlerWeb = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            String text = (String)msg.obj;

            if(text == null || mService == null) {
                return;
            }

            /*
             * Get JSON
             */
            try {
                JSONObject object = new JSONObject(text);
                final Gson gson = new Gson();
                String type = object.getString("Type");

                if(type.equals("situation")) {
                    HandleSituationMessage(object);
                }

                if(type.equals("Raw")) {
                    HandleRawDataMessage(object);
                }
                else if(type.equals("traffic")) {
                    mService.getTrafficCache().putTraffic(
                            object.getString("Tail"),
                            object.getInt("Icao_addr"),
                            (float)object.getDouble("Lat"),
                            (float)object.getDouble("Lng"),
                            object.getInt("Alt"),
                            (float)object.getDouble("Track"),
                            (int)object.getInt("Speed"),
                            Helper.getMillisGMT()
                            /*XXX:object.getLong("time")*/);
                } else {
                    HandleWeatherMessage(type, object);
                }

            } catch (JSONException e) {
                return;
            }
        }
    };

}
