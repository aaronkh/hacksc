package bought.my.way.into.hacksc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.snapchat.kit.sdk.SnapCreative;
import com.snapchat.kit.sdk.creative.api.SnapCreativeKitApi;
import com.snapchat.kit.sdk.creative.api.SnapCreativeKitCompletionCallback;
import com.snapchat.kit.sdk.creative.api.SnapCreativeKitSendError;
import com.snapchat.kit.sdk.creative.exceptions.SnapStickerSizeException;
import com.snapchat.kit.sdk.creative.media.SnapMediaFactory;
import com.snapchat.kit.sdk.creative.media.SnapPhotoFile;
import com.snapchat.kit.sdk.creative.media.SnapSticker;
import com.snapchat.kit.sdk.creative.models.SnapContent;
import com.snapchat.kit.sdk.creative.models.SnapPhotoContent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    CameraView camera;
    // To show stuff in the callback
    ImageButton flip, capture;
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();
    SnapCreativeKitApi snapCreativeKitApi;
    SnapMediaFactory snapMediaFactory;

    void post(String url, String json, Callback cb) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        client.newCall(request).enqueue(cb);
    }

    ProgressDialog pd;
    Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pd = new ProgressDialog(this);
        pd.setMessage("Scanning image...");
        pd.setCancelable(false);
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);
        flip = findViewById(R.id.flip);
        flip.setOnClickListener(v -> toggleCamera());
        capture = findViewById(R.id.capture);
        capture.setOnClickListener(v -> capturePictureSnapshot());
        camera = findViewById(R.id.camera);
        camera.setLifecycleOwner(this);
        snapCreativeKitApi = SnapCreative.getApi(this);
        snapMediaFactory = SnapCreative.getMediaFactory(this);
        camera.addCameraListener(new Listener());
        mainHandler = new Handler(this.getMainLooper());
    }

    @Override
    protected void onResume() {
        super.onResume();
//        camera
    }

    private class Listener extends CameraListener {

        @Override
        public void onCameraOpened(@NonNull CameraOptions options) {
        }

        @Override
        public void onCameraError(@NonNull CameraException exception) {
            super.onCameraError(exception);
        }

        @Override
        public void onPictureTaken(@NonNull PictureResult result) {

            super.onPictureTaken(result);
            result.toBitmap(bm -> {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bm.compress(Bitmap.CompressFormat.PNG, 100, baos); //bm is the bitmap object
                        byte[] b = baos.toByteArray();
                        String encoded = Base64.encodeToString(b, Base64.NO_PADDING | Base64.NO_WRAP);
                        Log.d("encoded", encoded);
                        String json = "{ \"requests\": [ { \"image\": { \"content\":\"" + encoded + "\" }, \"features\": [ { \"type\": \"LOGO_DETECTION\" } ] } ] }";
                        pd.show();

                        try {
                            post("https://vision.googleapis.com/v1/images:annotate?key=" + apisecret.API_KEY, json, new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    pd.dismiss();
                                    toast();
                                }

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    Bitmap bam = BitmapFactory.decodeResource(getResources(), R.drawable.hackscwhite);
                                    try {
                                        File newf = new File(getFilesDir(), "snap.PNG");
                                        File f = new File(newf.getCanonicalPath());
                                        SnapSticker snapSticker = null;
                                        try {
                                            JSONArray json = (new JSONObject(response.body().string())).getJSONArray("responses");
                                            for (int i = 0; i < json.length(); i++) {
                                                JSONArray ja = json.getJSONObject(i).getJSONArray("logoAnnotations");
                                                for (int j = 0; j < ja.length(); j++) {
                                                    JSONObject anal = ja.getJSONObject(j);
                                                    if (anal.getString("description").toLowerCase().contains("supreme") ||
                                                            anal.getString("description").toLowerCase().contains("louis vuitton") ||
                                                            anal.getString("description").toLowerCase().contains("gucci") ||
                                                            anal.getString("description").toLowerCase().contains("balenciaga") ||
                                                            anal.getString("description").toLowerCase().contains("prada")

                                                    ) {
                                                        bam = BitmapFactory.decodeResource(getResources(), R.drawable.hackscblack);
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        try {

                                            //create a file to write bitmap data
                                            File nfsticker = new File(getFilesDir(), "fsticker.PNG");
                                            File fsticker = new File(nfsticker.getCanonicalPath());
                                            if (!fsticker.exists())
                                                fsticker.createNewFile();
                                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                            bam.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
                                            byte[] bitmapdata = bos.toByteArray();

                                            FileOutputStream fos = new FileOutputStream(fsticker);
                                            fos.write(bitmapdata);
                                            fos.flush();
                                            fos.close();
                                            snapSticker = snapMediaFactory.getSnapStickerFromFile(fsticker);
                                        } catch (SnapStickerSizeException e) {
                                            e.printStackTrace();
                                        }
// Height and width~~ ~~in pixels
                                        snapSticker.setWidth(300);
                                        snapSticker.setHeight(300);

// Position is specified as a ratio between 0 & 1 to place the center of the sticker
                                        snapSticker.setPosX(0.3f);
                                        snapSticker.setPosY(0.7f);

// Specify clockwise rotation desired
                                        snapSticker.setRotationDegreesClockwise(330); // degrees clockwise

// Note: Your snap content can be video, photo, or live-cam
                                        if (f.exists()) {
                                            f.delete();
                                        } else {
                                            f.createNewFile();
                                        }
                                        FileOutputStream out = new FileOutputStream(f);
                                        bm.compress(Bitmap.CompressFormat.PNG, 90, out);
                                        out.flush();
                                        out.close();

                                        SnapPhotoFile photoFile = snapMediaFactory.getSnapPhotoFromFile(f);
                                        SnapPhotoContent snapPhotoContent = new SnapPhotoContent(photoFile);

                                        snapPhotoContent.setSnapSticker(snapSticker);
                                        pd.dismiss();
                                        camera.close();

                                        mainHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                Log.d("close", "runnable");

//                                                snapCreativeKitApi.send(snapContent);
                                                snapCreativeKitApi.sendWithCompletionHandler(snapPhotoContent, new SnapCreativeKitCompletionCallback() {
                                                    @Override
                                                    public void onSendSuccess() {
                                                        Log.d("yay", "yay");
                                                    }

                                                    @Override
                                                    public void onSendFailed(SnapCreativeKitSendError snapCreativeKitSendError) {
                                                        Log.d("error", snapCreativeKitSendError.toString());
                                                        Log.d("error", snapCreativeKitSendError.name());
                                                    }
                                                });
                                            }
                                        });
                                    } catch (Exception e) {
                                        Log.d("so,e error", "it is in catch lol");
                                        e.printStackTrace();
                                    }
                                }
                            });

                        } catch (IOException e) {
                            e.printStackTrace();
                            pd.dismiss();
                            toast();
                        }
                    }
            );

        }

    }

    private void capturePicture() {
        if (camera.isTakingPicture()) return;
        camera.takePicture();
    }

    private void capturePictureSnapshot() {
        if (camera.isTakingPicture()) return;
        camera.takePictureSnapshot();
    }

    private void toggleCamera() {
        if (camera.isTakingPicture() || camera.isTakingVideo()) return;
        camera.toggleFacing();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean valid = true;
        for (int grantResult : grantResults) {
            valid = valid && grantResult == PackageManager.PERMISSION_GRANTED;
        }
        if (valid && !camera.isOpened()) {
            camera.open();
        }
    }

    private void toast() {
        Toast.makeText(this, "Couldn't read image", Toast.LENGTH_SHORT);
    }
}
