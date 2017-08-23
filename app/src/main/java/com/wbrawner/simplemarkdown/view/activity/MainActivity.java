package com.wbrawner.simplemarkdown.view.activity;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.wbrawner.simplemarkdown.MarkdownApplication;
import com.wbrawner.simplemarkdown.R;
import com.wbrawner.simplemarkdown.presentation.MarkdownPresenter;
import com.wbrawner.simplemarkdown.view.adapter.EditPagerAdapter;
import java.io.InputStream;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    public static final int WRITE_PERMISSION_REQUEST = 0;
    private static final int OPEN_FILE_REQUEST = 1;
    public static final String AUTHORITY = "com.wbrawner.simplemarkdown.fileprovider";

    @Inject
    MarkdownPresenter presenter;

    @BindView(R.id.pager)
    ViewPager pager;
    @BindView(R.id.layout_tab)
    TabLayout tabLayout;

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((MarkdownApplication) getApplication()).getComponent().inject(this);

        // Reset the background color
        getWindow().setBackgroundDrawable(new ColorDrawable(0xFFFFFFFF));
        ButterKnife.bind(this);
        pager.setAdapter(
                new EditPagerAdapter(getSupportFragmentManager(), MainActivity.this)
        );
        pager.setPageMargin(1);
        pager.setPageMarginDrawable(R.color.colorAccent);
        Intent intent = getIntent();
        if (intent != null && !intent.getAction().equals(Intent.ACTION_MAIN) && intent.getData() != null) {
            loadFromUri(intent.getData());
        }
        if (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            tabLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
            tabLayout.setVisibility(View.GONE);
        else
            tabLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                if (ContextCompat.checkSelfPermission(
                        MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED)
                    showSaveDialog();
                else {
                    if (Build.VERSION.SDK_INT >= 23) {
                        requestPermissions(
                                new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                WRITE_PERMISSION_REQUEST
                        );
                    }
                }
                break;
            case R.id.action_share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, presenter.getMarkdown());
                shareIntent.setType("text/plain");
                startActivity(Intent.createChooser(
                        shareIntent,
                        getString(R.string.share_file)
                ));
                break;
            case R.id.action_load:
                requestOpen();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.action_save);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(R.string.hint_filename);
        input.setText(presenter.getFileName());
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            if (input.getText().length() > 0) {
                presenter.setFileName(input.getText().toString());
                setTitle(input.getText());
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    String path = Environment.getExternalStorageDirectory() + "/" +
                            Environment.DIRECTORY_DOCUMENTS + "/" + input.getText();
                    presenter.saveMarkdown(path);
                }
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
        });

        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case WRITE_PERMISSION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, open file save dialog
                    showSaveDialog();
                } else {
                    // Permission denied, do nothing
                    Toast.makeText(MainActivity.this, R.string.no_permissions, Toast.LENGTH_SHORT)
                            .show();
                }
                return;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (pager.getCurrentItem() == EditPagerAdapter.FRAGMENT_EDIT)
            super.onBackPressed();
        else
            pager.setCurrentItem(EditPagerAdapter.FRAGMENT_EDIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case OPEN_FILE_REQUEST:
                if (resultCode == RESULT_OK) {
                    loadFromUri(data.getData());
                }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void loadFromUri(Uri fileUri) {
        try {
            InputStream in =
                    getContentResolver().openInputStream(fileUri);
            Cursor retCur = getContentResolver()
                    .query(fileUri, null, null, null, null);
            if (retCur != null) {
                int nameIndex = retCur
                        .getColumnIndex(OpenableColumns.DISPLAY_NAME);
                retCur.moveToFirst();
                presenter.setFileName(retCur.getString(nameIndex));
            }
            presenter.loadMarkdown(in);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, R.string.file_load_error, Toast.LENGTH_SHORT)
                    .show();
        }

    }

    private void requestOpen() {
        Intent openIntent = new Intent(Intent.ACTION_GET_CONTENT);
        openIntent.setType("text/*");
        openIntent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(
                    Intent.createChooser(
                            openIntent,
                            getString(R.string.open_file)
                    ),
                    OPEN_FILE_REQUEST
            );
        } catch (ActivityNotFoundException e) {
            Toast.makeText(MainActivity.this, R.string.no_filebrowser, Toast.LENGTH_SHORT)
                    .show();
        }
    }


}