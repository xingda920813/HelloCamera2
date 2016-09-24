package com.xdandroid.hellocamera2.app;

import android.os.*;
import android.support.annotation.*;
import android.view.*;

import com.zhy.autolayout.*;

import butterknife.*;

public abstract class BaseCameraActivity extends AutoLayoutActivity {

    private Unbinder unbinder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentViewResId());

        unbinder = ButterKnife.bind(this);
        preInitData();
    }

    protected abstract int getContentViewResId();

    protected abstract void preInitData();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (unbinder != null) {
            unbinder.unbind();
        }
    }
}
