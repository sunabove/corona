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

import java.util.ArrayList;

public class CoronaDataAdapter extends ArrayAdapter<CoronaModel> implements View.OnClickListener{

    public ArrayList<CoronaModel> dataSet ;
    private Context context;
    private int lastPosition = -1;

    public CoronaDataAdapter(Context context, ArrayList<CoronaModel> dataSet) {
        super(context, R.layout.corona_row_item, dataSet);
        this.context = context;
    }

    @Override
    public void onClick(View v) {
        int position=(Integer) v.getTag();
        Object object= getItem(position);
        CoronaModel coronaModel =(CoronaModel)object;

        if (v.getId() == R.id.item_info ) {
            Snackbar.make(v, "Release date " + coronaModel.getFeature(), Snackbar.LENGTH_LONG)
                    .setAction("No action", null).show();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        CoronaModel coronaModel = getItem(position);

        class ViewHolder {
            TextView txtName;
            TextView txtType;
            TextView txtVersion;
            ImageView info;
        }

        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.corona_row_item, parent, false);

            viewHolder = new ViewHolder();

            viewHolder.txtName = (TextView) convertView.findViewById(R.id.name);
            viewHolder.txtType = (TextView) convertView.findViewById(R.id.type);
            viewHolder.txtVersion = (TextView) convertView.findViewById(R.id.version_number);
            viewHolder.info = (ImageView) convertView.findViewById(R.id.item_info);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Animation animation = AnimationUtils.loadAnimation(context, (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
        convertView.startAnimation(animation);
        lastPosition = position;

        viewHolder.txtName.setText(coronaModel.getName());
        viewHolder.txtType.setText(coronaModel.getType());
        viewHolder.txtVersion.setText(coronaModel.getVersion_number());
        viewHolder.info.setOnClickListener(this);
        viewHolder.info.setTag(position);

        return convertView;
    }

}
