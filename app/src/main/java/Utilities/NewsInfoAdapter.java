package Utilities;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cryptoinc.cryptofeed.R;

import java.util.ArrayList;

/**
 * Created by Gerard on 11/19/2017.
 */

public class NewsInfoAdapter extends RecyclerView.Adapter<NewsInfoViewHolder> {

    public ArrayList<NewsInfo> newsList;
    public LayoutInflater layoutInflater;

    public NewsInfoAdapter(LayoutInflater layoutInflater, ArrayList<NewsInfo> newsList) {
        this.layoutInflater = layoutInflater;
        this.newsList = newsList;
    }

    @Override
    public NewsInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.news_info, parent, false);
        return new NewsInfoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NewsInfoViewHolder holder, int position) {
        final NewsInfo info = newsList.get(position);
        holder.setViews(info);
        holder.v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newsInfoListListener.newsSelected(info);
            }
        });
    }

    private NewsInfoListListener newsInfoListListener;

    public void setNewsInfoListListener(NewsInfoListListener newsInfoListListener){
        this.newsInfoListListener = newsInfoListListener;
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    public interface NewsInfoListListener {
        void newsSelected(NewsInfo info);
    }

}
