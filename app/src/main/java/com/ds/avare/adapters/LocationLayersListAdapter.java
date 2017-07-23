package com.ds.avare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.ds.avare.R;

/**
 * Created by roleary on 11/27/2016.
 */

public class LocationLayersListAdapter  extends ArrayAdapter<String> {

    private final Context context;
    private final String[] values;

    public LocationLayersListAdapter(Context context, String[] values) {
        super(context, R.layout.location, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.location_layers, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.settings_text);
        Switch toggleSwitch = (Switch) rowView.findViewById(R.id.toggle_button);

        toggleSwitch.setOnClickListener(new View.OnClickListener() {
            private final String[] values = getContext().getResources().getStringArray(R.array.LayerType);

            @Override
            public void onClick(View v) {
//                Toast.makeText(getContext(), values[position] + " checked", Toast.LENGTH_LONG).show();
            }
        });

        textView.setText(values[position]);

        return rowView;
    }
}