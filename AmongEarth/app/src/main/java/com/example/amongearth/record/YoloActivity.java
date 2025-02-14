package com.example.amongearth.record;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.amongearth.R;
import com.example.amongearth.env.ImageUtils;
import com.example.amongearth.env.Logger;
import com.example.amongearth.env.Utils;
import com.example.amongearth.tflite.Classifier;
import com.example.amongearth.tflite.YoloV4Classifier;
import com.example.amongearth.tracking.MultiBoxTracker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class YoloActivity extends AppCompatActivity {

    public static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yolo2);

        imageView = findViewById(R.id.imageView);
        resultButton = findViewById(R.id.btn_result);
        resultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultButton.setEnabled(false);
                final Handler handler = new Handler();
                Glide.with(YoloActivity.this).load(R.raw.recycle).into(imageView);
                final Comparator<Classifier.Recognition> cmpAsc = new Comparator<Classifier.Recognition>() {
                    @Override
                    public int compare(Classifier.Recognition rhs, Classifier.Recognition lhs) {
                        return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                    }
                };

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final List<Classifier.Recognition> results = detector.recognizeImage(cropBitmap);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                String result_img = handleResult(cropBitmap, results);
                                Integer num_paper = 0, num_metal = 0, num_glass = 0, num_plastic = 0, num_waste = 0, num_nothing = 0;
                                Float confidence_min = 0.1f;

                                for (int i = 0; i < results.size(); ++i) {
                                    Classifier.Recognition result = results.get(i);
                                    if (result.getTitle().equals("paper")) {
                                        if (result.getConfidence() < confidence_min) num_nothing += 1;
                                        else num_paper += 1;
                                    }
                                    else if(result.getTitle().equals("metal")){
                                        if (result.getConfidence() < confidence_min) num_nothing += 1;
                                        else num_metal += 1;
                                    }
                                    else if(result.getTitle().equals("glass")){
                                        if (result.getConfidence() < confidence_min) num_nothing += 1;
                                        else num_glass += 1;
                                    }
                                    else if(result.getTitle().equals("plastic")){
                                        if (result.getConfidence() < confidence_min) num_nothing += 1;
                                        else num_plastic += 1;
                                    }
                                    else if(result.getTitle().equals("waste")){
                                        if (result.getConfidence() < confidence_min) num_nothing += 1;
                                        else num_waste += 1;
                                    }
                                    else if(result.getTitle().equals("eyeglasses")){
                                        if (result.getConfidence() < confidence_min) num_nothing += 1;
                                        else num_waste += 1;
                                    }
                                    else if(result.getTitle().equals("pringles")){
                                        if (result.getConfidence() < confidence_min) num_nothing += 1;
                                        else num_waste += 1;
                                    }
                                    else if(result.getTitle().equals("scissors")){
                                        if (result.getConfidence() < confidence_min) num_nothing += 1;
                                        else num_waste += 1;
                                    }
                                    else if(result.getTitle().equals("fruit packaging")){
                                        if (result.getConfidence() < confidence_min) num_nothing += 1;
                                        else num_waste += 1;
                                    }
                                    else if(result.getTitle().equals("cool pack")){
                                        if (result.getConfidence() < confidence_min) num_nothing += 1;
                                        else num_waste += 1;
                                    }
                                    else if(result.getTitle().equals("broken bottle")){
                                        if (result.getConfidence() < confidence_min) num_nothing += 1;
                                        else num_waste += 1;
                                    }
                                    else if(result.getTitle().equals("spring note")){
                                        if (result.getConfidence() < confidence_min) num_nothing += 1;
                                        else num_paper += 1;
                                    }
                                    else if(result.getTitle().equals("mat")){
                                        if (result.getConfidence() < confidence_min) num_nothing += 1;
                                        else num_waste += 1;
                                    }
                                    else if(result.getTitle().equals("wine glass")){
                                        if (result.getConfidence() < confidence_min) num_nothing += 1;
                                        else num_glass += 1;
                                    }
                                    else if(result.getTitle().equals("icepack")){
                                        if (result.getConfidence() < confidence_min) num_nothing += 1;
                                        else num_waste += 1;
                                    }
                                }

                                Intent intent2 = new Intent(YoloActivity.this, CountActivity.class);
                                Bundle bundle = new Bundle();
                                bundle.putString("originPath", origin_path);
                                bundle.putString("imgPath", result_img);
                                bundle.putInt("num_paper", num_paper);
                                bundle.putInt("num_metal", num_metal);
                                bundle.putInt("num_glass", num_glass);
                                bundle.putInt("num_plastic", num_plastic);
                                bundle.putInt("num_waste", num_waste);
                                bundle.putInt("num_nothing", num_nothing);
                                intent2.putExtras(bundle);
                                setResult(1, intent2);
                                startActivity(intent2);
                                }
                        });
                    }
                }).start();
            }
        });

        Intent intent = getIntent();
        origin_path = intent.getStringExtra("imgPath");
        this.sourceBitmap = BitmapFactory.decodeFile(origin_path);
        this.cropBitmap = Utils.processBitmap(sourceBitmap, TF_OD_API_INPUT_SIZE);

        initBox();
    }

    private static final Logger LOGGER = new Logger();
    public static final int TF_OD_API_INPUT_SIZE = 416;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;

    private static final String TF_OD_API_MODEL_FILE = "yolov4-custom-30000.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/custom.txt";

    private static final boolean MAINTAIN_ASPECT = false;
    private Integer sensorOrientation = 90;

    private Classifier detector;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private MultiBoxTracker tracker;

    protected int previewWidth = 0;
    protected int previewHeight = 0;

    private Bitmap sourceBitmap;
    private Bitmap cropBitmap;

    private String origin_path;

    private Button resultButton;
    private ImageView imageView;

    private void initBox() {
        previewHeight = TF_OD_API_INPUT_SIZE;
        previewWidth = TF_OD_API_INPUT_SIZE;
        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        tracker = new MultiBoxTracker(this);
        tracker.setFrameConfiguration(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, sensorOrientation);
        try {
            detector =
                    YoloV4Classifier.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_IS_QUANTIZED);
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
    }

    private String handleResult(Bitmap bitmap, List<Classifier.Recognition> results) {
        final Canvas canvas = new Canvas(bitmap);
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);

        final List<Classifier.Recognition> mappedRecognitions =
                new LinkedList<Classifier.Recognition>();

        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                canvas.drawRect(location, paint);
            }
        }

        File storage = getCacheDir();

        String fileName = "temp.jpg";
        File tempFile = new File(storage, fileName);

        try{
            tempFile.createNewFile();
            FileOutputStream out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90 , out);
            out.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempFile.getAbsolutePath();
    }
}

