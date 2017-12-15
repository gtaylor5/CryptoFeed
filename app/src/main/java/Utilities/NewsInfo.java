package Utilities;

import android.support.annotation.NonNull;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Gerard on 11/19/2017.
 */

public class NewsInfo implements Comparable<NewsInfo>{

    private String Title;
    private String Link;
    private String Source;
    private String pubDate;
    private String description;

    private long dateInMills;

    public NewsInfo(String link, String source, String title, String pubDate, String description) {
        Link = link;
        Source = source;
        Title = title;
        this.pubDate = pubDate;
        this.description = description;
    }

    public NewsInfo(){}

    public String getLink() {
        return Link;
    }

    public void setLink(String link) {
        Link = link;
    }

    public String getSource() {
        return Source;
    }

    public void setSource(String source) {
        Source = source;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public String getPubDate() {
        return pubDate;
    }

    public void setPubDate(String pubDate) throws ParseException{
        DateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        DateFormat target = new SimpleDateFormat("M/d/yyyy H:mm", Locale.US);
        Date date = format.parse(pubDate);
        dateInMills = date.getTime();
        this.pubDate = target.format(date);
    }

    @Override
    public String toString() {
        return "Utilities.NewsInfo{" +
                "Title='" + Title + '\'' +
                ", Link='" + Link + '\'' +
                ", Source='" + Source + '\'' +
                ", pubDate='" + pubDate + '\'' +
                '}';
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    @Override
    public int compareTo(@NonNull NewsInfo o) {
        if(this == o){
            return 0;
        }
        return Long.compare(o.dateInMills, this.dateInMills);
    }
}
