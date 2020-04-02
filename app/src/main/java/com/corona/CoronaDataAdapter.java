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

    public String getSnackbarInfo(Corona corona) {
        Corona c = corona;
        SimpleDateFormat df = ComInterface.MMdd_HHmm ;
        String info = String.format("동선 겹침: %s ~ %s\n위도 %.4f, 경도 %.4f", df.format(c.visit_fr), df.format(c.visit_to), c.latitude, c.longitude ) ;

        return info;
    }

    @Override
    public void onClick(View view) {
        int position=(Integer) view.getTag();

        Corona corona =(Corona) this.getItem(position) ;

        if (view.getId() == R.id.item_info ) {
            String info = "Info = " + this.getSnackbarInfo(corona);
            Snackbar snackbar = Snackbar.make(view, info, Snackbar.LENGTH_LONG);
            snackbar.setAction("No action", null);
            snackbar.addCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar snackbar, int event) {
                }

                @Override
                public void onShown(Snackbar snackbar) {
                }
            });
            snackbar.show();
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
