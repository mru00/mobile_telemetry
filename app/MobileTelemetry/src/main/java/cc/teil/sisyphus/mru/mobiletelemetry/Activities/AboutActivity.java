package cc.teil.sisyphus.mru.mobiletelemetry.Activities;

import android.os.Bundle;
import android.app.Activity;

import cc.teil.sisyphus.mru.mobiletelemetry.R;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        getActionBar().setDisplayHomeAsUpEnabled(false);
    }
}
