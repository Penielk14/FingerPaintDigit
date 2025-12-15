package com.example.myapplication;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import org.tensorflow.lite.Interpreter;

import android.widget.Button;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.nex3z.fingerpaintview.FingerPaintView;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    private TextView randomNumberText;
    private FingerPaintView fingerPaintView;
    private Button clearButton;
    private Button predictButton;
    private Button backButton;
    private Interpreter tflite;
    private int targetDigit;

    private static final int IMG_SIZE = 28;
    private static final int NUM_CLASSES = 10;
    private int wrongGuessCount = 0;
    private static final int MAX_WRONG_GUESS_COUNT = 5;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        randomNumberText = findViewById(R.id.randomNumberText);
        fingerPaintView = findViewById(R.id.fingerPaintView);
        clearButton = findViewById(R.id.clearButton);
        predictButton = findViewById(R.id.predictButton);
        backButton = findViewById(R.id.backButton);

        try {
            tflite = new Interpreter(loadModelFile());
        }catch(Exception ex){
            ex.printStackTrace();
        }

        chooseRandomDigit();


        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,HomeScreen.class);
                startActivity(intent);
            }
        });

        predictButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int predicted = predictDigitFromDrawing();
                

                if(predicted == targetDigit){
                    wrongGuessCount =0;
                    Toast.makeText(MainActivity.this, "Correct "+predicted, Toast.LENGTH_SHORT).show();
                    chooseRandomDigit();
                }else{
                    wrongGuessCount++;
                    Toast.makeText(MainActivity.this, "Wrong "+predicted, Toast.LENGTH_SHORT).show();

                    if (wrongGuessCount >= MAX_WRONG_GUESS_COUNT) {
                        openMainActivity2();
                    }
                }


            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fingerPaintView.clear();

            }
        });



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

   private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd("digit.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
   }

    private void chooseRandomDigit() {
        targetDigit = (int)(Math.random()*10);// 0 to 9
        randomNumberText.setText("Draw this number: " + targetDigit);

    }

    private ByteBuffer getByteBuffer(){
        int inputBufferSize = IMG_SIZE * IMG_SIZE;
        int inputBufferSum = inputBufferSize * 4;
        ByteBuffer inputBuffer = ByteBuffer.allocate(inputBufferSum);
        inputBuffer.order(ByteOrder.nativeOrder());

        Bitmap original = fingerPaintView.exportToBitmap();
        Bitmap resized = Bitmap.createScaledBitmap(original,28,28,true);

        int[] pixels = new int[inputBufferSize];
        resized.getPixels(pixels, 0, IMG_SIZE, 0, 0, IMG_SIZE, IMG_SIZE);


        for (int i = 0; i < inputBufferSize; i++) {
            int pixel = pixels[i];

            int a = Color.alpha(pixel);
            int r = Color.red(pixel);
            int g = Color.green(pixel);
            int b = Color.blue(pixel);

            float gray = (r + g + b) / 3.0f;

            float normalized = gray / 255.0f;

            normalized = 1.0f - normalized;

            inputBuffer.putFloat(normalized);


        }
        inputBuffer.rewind();
        return inputBuffer;

    }

    private int predictDigitFromDrawing(){
        ByteBuffer inputBuffer = getByteBuffer();
        float[][] output = new float[1][NUM_CLASSES];

        tflite.run(inputBuffer, output);

        int digit = 0;
        float bestDigit = output[0][0];

        for (int i = 0; i < NUM_CLASSES; i++){
            if(output[0][i] > bestDigit){
                bestDigit = output[0][i];
                digit = i;
            }
        }

        return digit;
    }


    public void openMainActivity2(){
        Intent intent = new Intent(MainActivity.this,MainActivity2.class);
        startActivity(intent);
        finish();
    }
}