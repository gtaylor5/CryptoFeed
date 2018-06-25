package Utilities;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.cryptoinc.cryptofeed.R;

/**
 * Created by Gerard on 11/19/2017.
 */

public class NewsInfoViewHolder extends RecyclerView.ViewHolder {

    public TextView titleView;
    public TextView sourceView;
    public TextView bodyView;
    public ImageView imageView;

    View v;

    NewsInfoViewHolder(View itemView) {
        super(itemView);
        titleView = itemView.findViewById(R.id.titleView);
        sourceView = itemView.findViewById(R.id.source);
        bodyView = itemView.findViewById(R.id.body);
        imageView = itemView.findViewById(R.id.articleImage);

        v = itemView;
    }

    void setViews(NewsInfo info){
        titleView.setText(info.getTitle());
        sourceView.setText(info.getSource());
        bodyView.setText(info.getDescription());
        Glide.with(v.getContext()).load(info.imageLink).into(imageView);
    }
}
