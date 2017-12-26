package Utilities;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.cryptoinc.cryptofeed.R;

/**
 * Created by Gerard on 11/19/2017.
 */

public class NewsInfoViewHolder extends RecyclerView.ViewHolder {

    public TextView titleView;
    public TextView sourceView;
    View v;

    NewsInfoViewHolder(View itemView) {
        super(itemView);
        titleView = itemView.findViewById(R.id.titleView);
        sourceView = itemView.findViewById(R.id.source_view);
        v = itemView;
    }

    void setViews(NewsInfo info){
        titleView.setText(info.getTitle());
        sourceView.setText(info.getSource());
    }
}
