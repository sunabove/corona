package com.corona;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class CoronaDataAdapter extends ArrayAdapter<Corona> implements ComInterface, View.OnClickListener{

    public ArrayList<Corona> dataSet ;
    private Context context;
    private int lastPosition = -1;

    public CoronaDataAdapter(Context context, ArrayList<Corona> dataSet) {
        super(context, R.layout.corona_row_item, dataSet);
        this.context = context;
    }

    @Override
    public void onClick(View v) {
        int position=(Integer) v.getTag();

        Corona corona =(Corona) this.getItem(position) ;

        if (v.getId() == R.id.item_info ) {
            Snackbar.make(v, "Release date " + corona.visit_to, Snackbar.LENGTH_LONG)
                    .setAction("No action", null).show();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Corona corona = getItem(position);

        class ViewHolder {
            TextView patient;
            TextView place;
            TextView visitFr;
            TextView visitTo;
            ImageView info;
        }

        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.corona_row_item, parent, false);

            viewHolder = new ViewHolder();

            viewHolder.patient = (TextView) convertView.findViewById(R.id.coronaPatient);
            viewHolder.place = (TextView) convertView.findViewById(R.id.coronaPlace);
            viewHolder.visitFr = (TextView) convertView.findViewById(R.id.coronaVisitFr);
            viewHolder.visitTo = (TextView) convertView.findViewById(R.id.coronaVisitTo);
            viewHolder.info = (ImageView) convertView.findViewById(R.id.item_info);

            viewHolder.info.setOnClickListener(this);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Animation animation = AnimationUtils.loadAnimation(context, (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
        convertView.startAnimation(animation);

        lastPosition = position;

        SimpleDateFormat df = ComInterface.MMdd_HHmmSS;

        viewHolder.patient.setText(corona.patient);
        viewHolder.place.setText(corona.place);
        viewHolder.visitFr.setText( df.format( corona.visit_fr ));
        viewHolder.visitTo.setText( df.format( corona.visit_to ));

        viewHolder.info.setTag(position);

        return convertView;
    }

}
