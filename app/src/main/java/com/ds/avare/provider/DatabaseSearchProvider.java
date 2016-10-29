package com.ds.avare.provider;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;

import com.ds.avare.R;
import com.ds.avare.StorageService;
import com.ds.avare.place.Destination;

import java.util.LinkedHashMap;

/**
 * Created by arabbani on 10/28/16.
 */
public class DatabaseSearchProvider extends ContentProvider {

    // Service that keeps state even when activity is dead
    protected StorageService mService;

    // Defines callbacks for service binding, passed to bindService()
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            mService = ((StorageService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg) { }
    };

    @Override
    public boolean onCreate() {
        mService = null;

        // Registering our receiver. Bind now.
        Intent intent = new Intent(getContext(), StorageService.class);
        getContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        MatrixCursor cursor = new MatrixCursor(
                new String[] {
                        BaseColumns._ID,
                        SearchManager.SUGGEST_COLUMN_TEXT_1,
                        SearchManager.SUGGEST_COLUMN_TEXT_2,
                        SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
                        SearchManager.SUGGEST_COLUMN_ICON_1
                }
        );

        if (mService == null) {
            return cursor;
        }

        synchronized (DatabaseSearchProvider.class) {
            LinkedHashMap<String, String> params = new LinkedHashMap<>();
            String search = uri.getLastPathSegment().toLowerCase();

            // This is not to be done repeatedly with new text input so sync.
            mService.getDBResource().search(search, params, false);
            mService.getUDWMgr().search(search, params); // From user defined points of interest

            if (params.size() > 0) {
                int i = 0;
                for (String key : params.keySet()){
                    String id = params.get(key);
                    String[] split = key.split(";");
                    String dbType = split[1];
                    String name = split[2];

                    int drawable = getDrawableFromType(dbType);

                    cursor.addRow(new Object[] { i, id, name, key, drawable });
                    i++;
                }
            }
        }

        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private int getDrawableFromType(String dbType) {
        if (dbType.equals("TACAN")) {
            return R.drawable.tacan;
        } else if (dbType.equals("NDB/DME")) {
            return R.drawable.ndbdme;
        } else if (dbType.equals("MARINE NDB") || dbType.equals("UHF/NDB") || dbType.equals("NDB")) {
            return R.drawable.ndb;
        } else if (dbType.equals("VOR/DME")) {
            return R.drawable.vordme;
        } else if (dbType.equals("VOT")) {
            return R.drawable.vot;
        } else if(dbType.equals("VORTAC")) {
            return R.drawable.vortac;
        } else if (dbType.equals("FAN MARKER")) {
            return R.drawable.marker;
        } else if (dbType.equals("VOR")) {
            return R.drawable.vor;
        } else if (dbType.equals("AIRPORT") || dbType.equals("SEAPLANE BAS") || dbType.equals("HELIPORT")
                || dbType.equals("ULTRALIGHT") || dbType.equals("GLIDERPORT") || dbType.equals("BALLOONPORT")) {
            return R.drawable.airport;
        } else if(dbType.equals("YREP-PT") || dbType.equals("YRNAV-WP") || dbType.equals("NARTCC-BDRY")
                || dbType.equals("NAWY-INTXN") || dbType.equals("NTURN-PT") || dbType.equals("YWAYPOINT")
                || dbType.equals("YMIL-REP-PT") || dbType.equals("YCOORDN-FIX") || dbType.equals("YMIL-WAYPOINT")
                || dbType.equals("YNRS-WAYPOINT") || dbType.equals("YVFR-WP") || dbType.equals("YGPS-WP")
                || dbType.equals("YCNF") || dbType.equals("YRADAR") || dbType.equals("NDME-FIX")
                || dbType.equals("NNOT-ASSIGNED") || dbType.equals("NDP-TRANS-XING") || dbType.equals("NSTAR-TRANS-XIN")
                || dbType.equals("NBRG-INTXN")) {
            return R.drawable.fix;
        } else if(dbType.equals(Destination.GPS)) {
            return R.drawable.geo;
        } else if(dbType.equals(Destination.MAPS)) {
            return R.drawable.maps;
        } else if(dbType.equals(Destination.UDW)) {
            return android.R.drawable.ic_dialog_map;
        } else {
            return R.drawable.unknown;
        }
    }

}
