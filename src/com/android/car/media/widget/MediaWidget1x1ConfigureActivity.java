package com.android.car.media.widget;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.android.car.media.R;
import com.harman.psa.widget.tabview.PSATabLayout;
import com.harman.psa.widget.verticallist.model.ItemData;

public class MediaWidget1x1ConfigureActivity extends AppCompatActivity implements SubFragment
        .OnChooseItemListener {

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private static final int PERMISSION_REQUEST_CODE = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_media_widget1x1_configure);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission
                    .READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        } else {
            initView();
        }

    }

    private void initView() {
        ViewPager viewPager = findViewById(R.id.viewPager);
        PSATabLayout tabLayout = findViewById(R.id.tabLayout);

        TabPagerAdapter pagerAdapter = new TabPagerAdapter(getSupportFragmentManager(), this);

        viewPager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(viewPager);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initView();
                } else {
                    Toast.makeText(MediaWidget1x1ConfigureActivity.this, getString(R.string.permission_denied),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions,
                        grantResults);
        }
    }

    @Override
    public void onChoose(ItemData itemValue) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        MediaWidget1x1.updateAppWidget(this, appWidgetManager, mAppWidgetId, itemValue);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}

