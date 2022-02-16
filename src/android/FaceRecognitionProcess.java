package cordova.plugin.facerecognition;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import com.ttv.face.ErrorInfo;
import com.ttv.face.FaceEngine;
import com.ttv.face.FaceResult;
import com.ttv.face.GenderInfo;
import com.ttv.face.LivenessInfo;
import com.ttv.face.MaskInfo;
import com.ttv.imageutil.TTVImageFormat;
import com.ttv.imageutil.TTVImageUtil;
import com.ttv.imageutil.TTVImageUtilError;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;

public class FaceRecognitionProcess extends Activity {

  private Bitmap mBitmap = null;
  private String age;
  private String gender;
  private String liveness;
  private String mask;
  private Boolean error = false;
  private String message;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    String license = "";
    FaceEngine.getInstance(this).setActivation(license);
    FaceEngine.getInstance(this).init(1);
    Bundle data = getIntent().getExtras();
    String images = data.getString("image", "");
    byte[] decodedString = Base64.decode(images, Base64.DEFAULT);
    mBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    Log.d("Log", "masuk 1");
    this.processImage();
  }

  public void processImage() {
    Log.d("Log", "Masuk processImage");

    if (mBitmap == null) {
      Log.d("Log", "Error 1");
      this.error = true;
      this.message = "bitmap is null!";
      this.response();
    }
    Bitmap bitmap = TTVImageUtil.getAlignedBitmap(mBitmap, true);

    byte[] bgr24 = TTVImageUtil.createImageData(bitmap.getWidth(), bitmap.getHeight(), TTVImageFormat.BGR24);
    int transformCode = TTVImageUtil.bitmapToImageData(bitmap, bgr24, TTVImageFormat.BGR24);
    if (transformCode != TTVImageUtilError.CODE_SUCCESS) {
      this.error = true;
      Log.d("Log", "Error 2");
      this.message = "transform bitmap To ImageData failed";
      this.response();
    }

    List<FaceResult> faceResults = FaceEngine.getInstance(this).detectFace(bitmap);
    int faceProcessCode = FaceEngine.getInstance(this).faceAttrProcess(bitmap, faceResults);

    Log.d("Log", "" + faceResults.size());

    if (faceProcessCode != ErrorInfo.MOK) {
      this.error = true;
      Log.d("Log", "Error 3");
      this.message = "process failed!";
      this.response();
    }

    if (faceResults.size() == 0) {
      this.error = true;
      Log.d("Log", "Error 4");
      this.message = "can not do further action, exit!";
      this.response();
    }

    for (int i = 0; i < faceResults.size(); i++) {
      this.age = String.valueOf(faceResults.get(i).age);
    }

    for (int i = 0; i < faceResults.size(); i++) {
      this.gender = faceResults.get(i).gender == GenderInfo.MALE ? "MALE" : (faceResults.get(i).gender == GenderInfo.FEMALE ? "FEMALE" : "UNKNOWN");
    }

    if (faceResults.size() > 0) {
      for (int i = 0; i < faceResults.size(); i++) {
        String liveness = null;
        switch (faceResults.get(i).liveness) {
          case LivenessInfo.ALIVE:
            liveness = "REAL";
            break;
          case LivenessInfo.NOT_ALIVE:
            liveness = "FAKE";
            break;
          case LivenessInfo.FACE_NUM_MORE_THAN_ONE:
            liveness = "FACE_NUM_MORE_THAN_ONE";
            break;
          case LivenessInfo.UNKNOWN:
          default:
            liveness = "";
            break;
        }
        this.liveness = liveness;
      }
    }

    if (!faceResults.isEmpty()) {
      for (int i = 0; i < faceResults.size(); i++) {
        int mask = faceResults.get(i).mask;
        String stringMask;
        switch (mask) {
          case MaskInfo.NOT_WORN:
            stringMask = "No Mask";
            break;
          case MaskInfo.WORN:
            stringMask = "Mask";
            break;
          default:
            stringMask = "Uncertain Mask";
            break;
        }
        this.mask = stringMask;
      }
    }

    this.error = false;
    this.response();
  }

  private void response() {
    JSONObject obj = new JSONObject();
    Log.d("Log", "Response");
    Log.d("Log", "" + this.age);
    Log.d("Log", "" + this.gender);
    Log.d("Log", "" + this.liveness);
    Log.d("Log", "" + this.mask);


    try {
      obj.put("error", error);
      obj.put("age", this.age);
      obj.put("gender", this.gender);
      obj.put("liveness", this.liveness);
      obj.put("mask", this.mask);
      obj.put("message", this.message);
    } catch (JSONException e) {
      e.printStackTrace();
      obj = new JSONObject();
    }

    Intent data = new Intent();
    data.putExtra("data", obj.toString());
    setResult(RESULT_OK, data);
    finish();
  }

}
