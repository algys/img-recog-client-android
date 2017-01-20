package com.example.algys.imagerecognizer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class InfoActivity extends AppCompatActivity {
    private static final String TAG = "AndroidCameraApi";

    ImageView image;
    TextView author_text;
    TextView year_text;
    TextView info_text;


    String author;
    String year;
    String info;
    String name;
    String url;

    Bitmap mIcon_val;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        image = (ImageView) findViewById(R.id.imageView);
        author_text = (TextView) findViewById(R.id.textView);
        year_text = (TextView) findViewById(R.id.textView2);
        info_text = (TextView) findViewById(R.id.textView3);

        Intent intent = getIntent();

        author = intent.getStringExtra("author");
        name = intent.getStringExtra("name");
        info = intent.getStringExtra("info");
        year = intent.getStringExtra("year");
        url = intent.getStringExtra("url");

        setTitle(name);
        author_text.setText(author);
        year_text.setText(year);
        info_text.setText(info);

        MyTask task = new MyTask();
        task.execute(url);

    }

    public static Bitmap downloadImage(String iUrl) {
        Bitmap bitmap = null;
        HttpURLConnection conn = null;
        BufferedInputStream buf_stream = null;
        try {
            Log.v(TAG, "Starting loading image by URL: " + iUrl);
            conn = (HttpURLConnection) new URL(iUrl).openConnection();
            conn.setDoInput(true);
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.connect();
            buf_stream = new BufferedInputStream(conn.getInputStream(), 8192);
            bitmap = BitmapFactory.decodeStream(buf_stream);
            buf_stream.close();
            conn.disconnect();
            buf_stream = null;
            conn = null;
        } catch (MalformedURLException ex) {
            Log.e(TAG, "Url parsing was failed: " + iUrl);
        } catch (IOException ex) {
            Log.d(TAG, iUrl + " does not exists");
        } catch (OutOfMemoryError e) {
            Log.w(TAG, "Out of memory!!!");
            return null;
        } finally {
            if ( buf_stream != null )
                try { buf_stream.close(); } catch (IOException ex) {}
            if ( conn != null )
                conn.disconnect();
        }
        return bitmap;
    }


    class MyTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap img = downloadImage(params[0]);
            return img;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            image.setImageBitmap(result);
        }
    }
}
