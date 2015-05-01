package com.mobanic.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.mobanic.R;
import com.mobanic.utils.RatioImageView;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;


public class DetailActivity extends ActionBarActivity {

    private ParseObject mCar;
    private String mCarId;

    public static Context mContext;

    private Intent mShareIntent;
    private SharedPreferences mSharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSharedPrefs.edit().putBoolean("imageReady", false).apply();

        mContext = this;

        if (getIntent() != null) {
            mCarId = getIntent().getStringExtra("car_id");
        } else if (savedInstanceState != null) {
            mCarId = savedInstanceState.getString("car_id");
        }

        updateCarsList(false);

        findViewById(R.id.button_contact).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(DetailActivity.this, ContactActivity.class));
            }
        });
    }

    private void updateCarsList(boolean fromNetwork) {
        if (mCarId == null) return;

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Car");
        if (!fromNetwork) {
            query.fromLocalDatastore();
        }
        query.getInBackground(mCarId, new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject car, ParseException e) {
                if (car == null) return;

                mCar = car;

                String make = car.getString("make");
                String model = car.getString("model");

                String title = make + " " + model;
                if (title.length() > 20) {
                    title = model;
                }
                getSupportActionBar().setTitle(make);

                setCoverImage();
                setGalleryImages();
                fillOutSpecs();
                fillOutFeatures();

                String url = mCar.getParseFile("coverImage").getUrl();
                if (title != null & url != null) {
                    new SetShareIntentTask().execute(title, url);
                }
            }
        });
    }

    private class SetShareIntentTask extends AsyncTask<String, Void, Intent> {

        @Override
        protected Intent doInBackground(String... strings) {

            String title = strings[0];
            String strUrl = strings[1];

            try {
                SharedPreferences sharedPrefs =
                        PreferenceManager.getDefaultSharedPreferences(DetailActivity.this);
                int imageNum = sharedPrefs.getInt("imageNum", 0);

                if (imageNum > 0) {
                    File dir = getFilesDir();
                    File file = new File(dir, "car" + imageNum + ".png");
                    file.delete();
                }

                URL url = new URL(strUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.connect();
                InputStream is = conn.getInputStream();
                Bitmap bm = BitmapFactory.decodeStream(is);
                FileOutputStream fos = DetailActivity.this.openFileOutput("car" + (imageNum + 1) + ".png", Context.MODE_WORLD_READABLE);
                sharedPrefs.edit().putInt("imageNum", imageNum+1).apply();

                ByteArrayOutputStream outstream = new ByteArrayOutputStream();

                bm.compress(Bitmap.CompressFormat.PNG, 100, outstream);
                byte[] byteArray = outstream.toByteArray();

                fos.write(byteArray);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            mShareIntent = new Intent();
            mShareIntent.setAction(Intent.ACTION_SEND);
            mShareIntent.putExtra(Intent.EXTRA_SUBJECT, title + " - Mobanic");
            mShareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this car I found! Care for your own test drive? - mobanic.com");
            mShareIntent.putExtra("sms_body", "Check out this car I found! Care for your own test drive? - mobanic.com");

            SharedPreferences sharedPrefs =
                    PreferenceManager.getDefaultSharedPreferences(DetailActivity.this);
            int imageNum = sharedPrefs.getInt("imageNum", 0);
            mShareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(getFilesDir(), "car" + imageNum + ".png")));

            mShareIntent.setType("text/plain");
            mShareIntent.setType("image/*");

            return null;
        }

        @Override
        protected void onPostExecute(Intent intent) {
            super.onPostExecute(intent);

            mSharedPrefs.edit().putBoolean("imageReady", true);
            invalidateOptionsMenu();
        }
    }

    private void setCoverImage() {
        String url = mCar.getParseFile("coverImage").getUrl();

        RatioImageView imageView = (RatioImageView) findViewById(R.id.image);
        Picasso.with(this).load(url).fit().centerCrop().into(imageView);
    }

    private void setGalleryImages() {
        final ViewFlipper flipper = (ViewFlipper) findViewById(R.id.flipper);
        flipper.setInAnimation(AnimationUtils.loadAnimation(this,
                android.R.anim.fade_in));
        flipper.setOutAnimation(AnimationUtils.loadAnimation(this,
                android.R.anim.fade_out));
        flipper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flipper.stopFlipping();
                flipper.showNext();
                flipper.startFlipping();
            }
        });

        ParseQuery<ParseObject> query = mCar.getRelation("galleryImage").getQuery();
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> images, ParseException e) {
                if (e == null && images.size() > 0) {
                    flipper.removeAllViews();
                    for (ParseObject image : images) {
                        String url = image.getParseFile("image").getUrl();

                        RatioImageView imageView = (RatioImageView) View.inflate(
                                DetailActivity.this,
                                R.layout.gallery_image,
                                null);
                        Picasso.with(DetailActivity.this)
                                .load(url)
                                .fit()
                                .centerCrop()
                                .into(imageView);
                        flipper.addView(imageView);
                    }
                } else {
                    findViewById(R.id.gallery_header).setVisibility(View.GONE);
                    findViewById(R.id.flipper).setVisibility(View.GONE);
                }
            }
        });
    }

    private void fillOutSpecs() {
        ((TextView) findViewById(R.id.make)).setText(mCar.getString("make"));
        ((TextView) findViewById(R.id.model)).setText(mCar.getString("model"));
        ((TextView) findViewById(R.id.year)).setText(mCar.getInt("year") + "");
        String mileage = NumberFormat.getNumberInstance(Locale.US).format(mCar.getInt("mileage"));
        ((TextView) findViewById(R.id.mileage)).setText(mileage);
        ((TextView) findViewById(R.id.previousOwners)).setText(mCar.getInt("previousOwners") + "");
        ((TextView) findViewById(R.id.engine)).setText(mCar.getString("engine"));
        ((TextView) findViewById(R.id.transmission)).setText(mCar.getString("transmission"));
        ((TextView) findViewById(R.id.fuelType)).setText(mCar.getString("fuelType"));
        ((TextView) findViewById(R.id.color)).setText(mCar.getString("color"));
        ((TextView) findViewById(R.id.location)).setText(mCar.getString("location"));
    }

    private void fillOutFeatures() {
        List<String> features = mCar.getList("features");

        LinearLayout featuresContainer = (LinearLayout) findViewById(R.id.features_container);
        if (features != null) {
            for (String feature : features) {
                TextView textView = (TextView) View.inflate(
                        DetailActivity.this,
                        R.layout.feature_list_item,
                        null);
                textView.setText(feature);
                featuresContainer.addView(textView);
            }
        } else {
            findViewById(R.id.features_header).setVisibility(View.GONE);
            findViewById(R.id.features_container).setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);

        MenuItem item = menu.findItem(R.id.menu_item_share);
        ShareActionProvider shareActionProvider =
                (ShareActionProvider) MenuItemCompat.getActionProvider(item);

            if (shareActionProvider != null & mShareIntent != null) {
                shareActionProvider.setShareIntent(mShareIntent);
            }

        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("car_id", mCar.getObjectId());
        super.onSaveInstanceState(outState);
    }

    public static Context getContext() {
        return mContext;
    }

    public static class PushReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                ParseQuery<ParseObject> query = ParseQuery.getQuery("Car");
                query.fromLocalDatastore();
                query.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> cars, ParseException e) {
                        for (ParseObject car : cars) {
                            car.unpinInBackground();
                        }
                    }
                });

                ((DetailActivity) DetailActivity.getContext()).updateCarsList(true);
            } catch (Exception e) {
                Log.d("DetailActivity", "Can't get activity context to update content. " +
                        "Just skip, will be updated in a moment.");
            }
        }
    }
}
