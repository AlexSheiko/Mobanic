package com.mappfia.mobanic;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

import static com.mappfia.mobanic.MakesSpinner.MakesSpinnerListener;

public class MainActivity extends ActionBarActivity
        implements MakesSpinnerListener {

    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;
    private CarsAdapter mCarsAdapter;
    private MakesSpinner mMakeSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupActionBar();


        mCarsAdapter = new CarsAdapter(this);

        final ListView carsListView = (ListView) findViewById(R.id.listview_cars);
        carsListView.setAdapter(mCarsAdapter);
        carsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                                    int position, long id) {
                String carId = mCarsAdapter.getItem(position).getObjectId();
                Intent intent = new Intent(MainActivity.this,
                        DetailActivity.class);
                intent.putExtra("car_id", carId);
                startActivity(intent);

                // TODO: If selected car is sold, show similar models in listing
            }
        });

        populateCarsList(false);
    }

    private void populateCarsList(boolean fromNetwork) {
        populateCarsList(fromNetwork, null, null);
    }

    private void populateCarsList(boolean fromNetwork, final String key, final List<String> valuesList) {
        final ListView carsListView = (ListView) findViewById(R.id.listview_cars);
        final FrameLayout progressBar = (FrameLayout) findViewById(R.id.progressBar);

        carsListView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Car");
        if (!fromNetwork) {
            query.fromLocalDatastore();
        }
        if (key != null && valuesList != null) {
            query.whereContainedIn(key, valuesList);
        }
        query.orderByDescending("createdAt");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> cars, ParseException e) {
                if (cars.size() == 0) {
                    if (isOnline()) {
                        populateCarsList(true);
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Connect to a network to load cars list", Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                progressBar.setVisibility(View.GONE);
                carsListView.setVisibility(View.VISIBLE);

                if (e == null) {
                    List<String> makeStrings = new ArrayList<>();

                    mCarsAdapter.clear();
                    for (ParseObject car : cars) {
                        mCarsAdapter.add(car);
                        car.pinInBackground();
                        makeStrings.add(car.getString("make"));
                    }

                    if (key == null && valuesList == null) {
                        mMakeSpinner = (MakesSpinner) findViewById(R.id.make_spinner);
                        mMakeSpinner.setItems(MainActivity.this, makeStrings);
                    }
                }
            }
        });
    }

    @Override
    public void onMakesSelected(List<String> valuesArray) {
        populateCarsList(true, "make", valuesArray);
    }

    private void setupActionBar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                mToolbar, R.string.drawer_open, R.string.drawer_close);
        mDrawerLayout.setDrawerListener(drawerToggle);
        drawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mToolbar.inflateMenu(R.menu.menu_main);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        // TODO: Replace the refresh button with GCM live updates from server
        if (id == R.id.action_refresh) {
            populateCarsList(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

}
