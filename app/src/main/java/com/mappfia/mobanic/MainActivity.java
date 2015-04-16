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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;


public class MainActivity extends ActionBarActivity {

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupActionBar();


        final CarsAdapter carsAdapter = new CarsAdapter(this);

        ListView carsListView = (ListView) findViewById(R.id.listview_cars);
        carsListView.setAdapter(carsAdapter);
        carsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                                    int position, long id) {
                Intent intent = new Intent(MainActivity.this,
                        DetailActivity.class);
                intent.putExtra("car_id", carsAdapter.getItem(position).getObjectId());
                startActivity(intent);
            }
        });

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Car");
        if (!isOnline()) {
            query.fromLocalDatastore();
        }
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> cars, ParseException e) {
                if (e == null) {
                    for (ParseObject car : cars) {
                        carsAdapter.add(car);
                        car.pinInBackground();
                    }
                }
            }
        });
    }

    private void setupActionBar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        String[] navItems = getResources().getStringArray(R.array.categories);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(new NavigationDrawerAdapter(this, navItems));
        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                mToolbar, R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerSlide(View drawerView, float slideOffset) {
                float moveFactor = (mDrawerList.getWidth() * slideOffset);
                LinearLayout container = (LinearLayout) findViewById(R.id.container);
                container.setTranslationX(moveFactor);
            }
        };
        drawerToggle.syncState();
        mDrawerLayout.setDrawerListener(drawerToggle);
    }

    private void selectItem(int position) {
        /*
        String[] categories = getResources().getStringArray(R.array.categories);
        String category = categories[position];

        Intent intent = new Intent(this, CategoryActivity.class);
        intent.putExtra("category", category);
        startActivity(intent);
        */
        Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();

        mDrawerLayout.closeDrawer(Gravity.START);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mToolbar.inflateMenu(R.menu.menu_main);
        return true;
    }

    private class NavigationDrawerAdapter extends ArrayAdapter<String> {
        public NavigationDrawerAdapter(Context context, String[] navItems) {
            super(context, 0, navItems);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String itemName = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.nav_drawer_item, parent, false);
            }
            TextView navLabel = (TextView) convertView.findViewById(R.id.text);
            navLabel.setText(itemName);

            return convertView;
        }
    }

    public boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

}