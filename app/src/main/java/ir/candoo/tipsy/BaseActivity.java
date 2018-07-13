package ir.candoo.tipsy;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.firebase.client.Firebase;

public class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity =====>";
    public ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        prerequisites();


    }

    private void prerequisites() {
        try {
            Firebase.setAndroidContext(this);
            FacebookSdk.sdkInitialize(getApplicationContext());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("pls wait ....");
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        hideProgressDialog();
    }
}
