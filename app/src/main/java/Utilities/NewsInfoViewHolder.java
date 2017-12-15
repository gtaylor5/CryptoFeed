package Utilities;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.cryptoinc.cryptofeed.R;

/**
 * Created by Gerard on 11/19/2017.
 */

public class NewsInfoViewHolder extends RecyclerView.ViewHolder {

    TextView titleView;
    TextView sourceView;
    String link;
    View v;

    public NewsInfoViewHolder(View itemView) {
        super(itemView);
        titleView = (TextView)itemView.findViewById(R.id.titleView);
        sourceView = (TextView)itemView.findViewById(R.id.source_view);
        v = itemView;
    }

    public void setViews(NewsInfo info){
        titleView.setText(info.getTitle());
        sourceView.setText(info.getSource());
        link = info.getLink();
    }
}
