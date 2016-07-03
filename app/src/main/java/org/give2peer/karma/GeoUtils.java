/*
 * Copyright (C) 2007-2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.give2peer.karma;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.give2peer.karma.exception.CriticalException;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;

//import com.ianhanniballake.localstorage.LocalStorageProvider;

/**
 * bad name ;)
 */
public class GeoUtils {
    public GeoUtils() {} //private constructor to enforce Singleton pattern



    public static LatLngBounds getLatLngBounds(List<LatLng> latLngs) {
        LatLngBounds.Builder bc = new LatLngBounds.Builder();
        for (LatLng latLng : latLngs) { bc.include(latLng); }

        return bc.build();
    }

    public static LatLng getLatLngCentroid(List<LatLng> latLngs) {
        if (latLngs.size() == 0) return null;

        return getLatLngBounds(latLngs).getCenter();
    }

    public static LatLng locationToLatLng (Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }


}
