package com.mobanic;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.mobanic.views.RatioImageView;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;


public class DetailActivity extends AppCompatActivity {

    private ParseObject mCar;
    private String mCarId;
    private int mCarPosition;

    private Intent mShareIntent;
    private Uri mImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // TODO Check and return if car was sold

        if (getIntent() != null) {
            mCarId = getIntent().getStringExtra("car_id");
            mCarPosition = getIntent().getIntExtra("car_position", -1);
        } else if (savedInstanceState != null) {
            mCarId = savedInstanceState.getString("car_id");
            mCarPosition = savedInstanceState.getInt("car_position");
        }

        updateCarDetails();

        findViewById(R.id.fab_contact).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(DetailActivity.this, ContactActivity.class);
                i.putExtra("car_id", mCarId);
                i.putExtra("car_position", mCarPosition);
                startActivity(i);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        outState.putString("car_id", mCarId);
        outState.putInt("car_position", mCarPosition);
        super.onSaveInstanceState(outState, outPersistentState);
    }

    private void updateCarDetails() {
        ParseQuery<Car> query = ParseQuery.getQuery("Car");
        query.fromLocalDatastore();
        query.getInBackground(mCarId, new GetCallback<Car>() {
            @Override
            public void done(Car car, ParseException e) {
                if (e != null) {
                    Toast.makeText(DetailActivity.this, e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                } else if (car.isSold()) {
                    Toast.makeText(DetailActivity.this, getString(R.string.sold),
                            Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                mCar = car;

                String make = car.getMake();
                String model = car.getModel();

                String title = make + " " + model;
                if (title.length() > 20) {
                    title = model;
                }
                getSupportActionBar().setTitle(make);

                setCoverImage();
                // TODO Open separate connection to download gallery images for a specific page
                setGalleryImages();
                fillOutSpecs();
                fillOutFeatures();

                String url = mCar.getParseFile("coverImage").getUrl();
                if (url != null) {
                    new SetShareIntentTask().execute(title, url);
                }
            }
        });
    }

    private class SetShareIntentTask extends AsyncTask<String, Void, Intent> {

        @Override
        protected Intent doInBackground(String... strings) {

            String title = strings[0];
            String urlStr = strings[1];

            Bitmap bitmap = null;
            try {
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url
                        .openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                bitmap = BitmapFactory.decodeStream(input);
            } catch (IOException e) {
                Log.e("DetailActivity", "Failed to attach image to share intent");
            }

            String imagePath = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "title", null);

            mImageUri = Uri.parse(imagePath);

            mShareIntent = new Intent();
            mShareIntent.setAction(Intent.ACTION_SEND);
            mShareIntent.putExtra(Intent.EXTRA_SUBJECT, title + " - Mobanic");
            mShareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this car I found! Care for your own test drive? - mobanic.com");
            mShareIntent.putExtra("sms_body", "Check out this car I found! Care for your own test drive? - mobanic.com");
            mShareIntent.putExtra(Intent.EXTRA_STREAM, mImageUri);
            mShareIntent.setType("text/plain");
            mShareIntent.setType("image/*");

            return null;
        }

        @Override
        protected void onPostExecute(Intent intent) {
            super.onPostExecute(intent);

            invalidateOptionsMenu();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mImageUri != null) {
            getContentResolver().delete(mImageUri, null, null);
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
                    findViewById(R.id.gallery_header).setVisibility(View.VISIBLE);
                    findViewById(R.id.flipper).setVisibility(View.VISIBLE);

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
                    if (images.size() == 1) {
                        flipper.setOnClickListener(null);
                        flipper.stopFlipping();
                    }
                }
            }
        });
    }

    private void fillOutSpecs() {
        ((TextView) findViewById(R.id.make)).setText(mCar.getString("make"));
        ((TextView) findViewById(R.id.model)).setText(mCar.getString("model"));
        ((TextView) findViewById(R.id.year)).setText(mCar.getInt("year") + "");
        ((TextView) findViewById(R.id.mileage)).setText(mCar.getInt("mileage") + "");
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
                        R.layout.list_item_feature,
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return(true);
        }
        return super.onOptionsItemSelected(item);
    }
}