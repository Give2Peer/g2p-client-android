package org.give2peer.karma.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.orm.SugarRecord;

import org.give2peer.karma.exception.CriticalException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 *
 */
public class ItemPicture extends SugarRecord implements Parcelable
{

    // Long id; // provided by SugarRecord
    String url;
    Map<String, String> thumbnails;


    //// PARCELABLE ////////////////////////////////////////////////////////////////////////////////

    private ItemPicture(Parcel in) {
        setId(in.readLong());
        setUrl(in.readString());
        // We want to zip two Lists into one Map.
        // http://stackoverflow.com/questions/1839668/clearest-way-to-combine-two-lists-into-a-map-java
        // TLDR; Zipping is more complicated than it should.
        List<String> thumbnailsKeys = in.createStringArrayList();
        List<String> thumbnailsVals = in.createStringArrayList();
        if (thumbnailsKeys.size() != thumbnailsVals.size()) {
            throw new CriticalException("Item picture parcel is corrupted.");
        }
        if (0 == thumbnailsKeys.size()) {
            throw new CriticalException("Item picture has no thumbnails.");
        }
        Map<String, String> thumbs = new HashMap<String, String>();
        for (int i = 0; i < thumbnailsKeys.size(); i++) {
            thumbs.put(thumbnailsKeys.get(i), thumbnailsVals.get(i));
        }
        setThumbnails(thumbs);
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(getId());
        parcel.writeString(getUrl());
        List<String> thumbnailsKeys = new ArrayList<>();
        List<String> thumbnailsVals = new ArrayList<>();
        for (Map.Entry<String, String> entry : getThumbnails().entrySet()) {
            thumbnailsKeys.add(entry.getKey());
            thumbnailsVals.add(entry.getValue());
        }
        parcel.writeStringList(thumbnailsKeys);
        parcel.writeStringList(thumbnailsVals);
    }

    public static final Creator<ItemPicture> CREATOR = new Creator<ItemPicture>() {
        @Override
        public ItemPicture createFromParcel(Parcel in) {
            return new ItemPicture(in);
        }

        @Override
        public ItemPicture[] newArray(int size) {
            return new ItemPicture[size];
        }
    };

    @Override
    public int describeContents() {
        // http://stackoverflow.com/questions/4076946/parcelable-where-when-is-describecontents-used/4914799#4914799
        return 0; // also, no constant is available (facepalm)
    }


    //// DESCRIPTION ///////////////////////////////////////////////////////////////////////////////

    /**
     * @return the url of the picture
     */
    public String toString() {
        return getUrl();
    }

    public String getThumbnail(int width, int height) {
        String key = String.format(Locale.FRENCH, "%dx%d", width, height);
        String thumb = getThumbnails().get(key); // null when key is not found
        if (null == thumb) thumb = "";
        return thumb;
    }

    /**
     * See https://github.com/Polidea/AndroidImageCache/issues/6
     * Note: the other image cache lib we're using is choking on our dirty https too.
     * @return the thumbnail URL in `http`, not `https`.
     */
//    public String getThumbnailNoSsl()
//    {
//        return StringUtils.httpsToHttp(getThumbnail());
//    }


    //// VANILLA ACCESSORS AND MUTATORS ////////////////////////////////////////////////////////////

//  Id is handled by SugarRecord.
//  public Long getId()                                       { return id;                         }
//  public void setId(Long id)                                { this.id = id;                      }

    public String getUrl()                                    { return url;                        }

    public void setUrl(String url)                            { this.url = url;                    }

    public Map<String, String> getThumbnails()                { return thumbnails;                 }

    public void setThumbnails(Map<String, String> thumbnails) { this.thumbnails = thumbnails;      }

}
