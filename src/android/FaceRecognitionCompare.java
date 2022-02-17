package cordova.plugin.facerecognition;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;

import com.ttv.face.ErrorInfo;
import com.ttv.face.FaceEngine;
import com.ttv.face.FaceResult;
import com.ttv.face.GenderInfo;
import com.ttv.face.LivenessInfo;
import com.ttv.face.MaskInfo;
import com.ttv.facedemo.R;
import com.ttv.imageutil.TTVImageFormat;
import com.ttv.imageutil.TTVImageUtil;
import com.ttv.imageutil.TTVImageUtilError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class FaceRecognitionCompare extends Activity {

  private static final int TYPE_MAIN = 0;
  private static final int TYPE_ITEM = 1;
  private byte[] mainFeature;
  private Boolean error = false;
  private String message;
  private float similarity;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    String license = "";
    FaceEngine.getInstance(this).setActivation(license);
    FaceEngine.getInstance(this).init(1);
    Bundle data = getIntent().getExtras();
    String mainImage = data.getString("mainImage", "");
    String otherImage = data.getString("otherImage", "");
    byte[] mainDecodedString = Base64.decode(mainImage, Base64.DEFAULT);
    Bitmap bitmap = BitmapFactory.decodeByteArray(mainDecodedString, 0, mainDecodedString.length);
    this.processImage(bitmap, TYPE_MAIN);
    byte[] otherDecodedString = Base64.decode(otherImage, Base64.DEFAULT);
    bitmap = BitmapFactory.decodeByteArray(otherDecodedString, 0, otherDecodedString.length);
    this.processImage(bitmap, TYPE_ITEM);
  }

  public void processImage(Bitmap bitmap, int type) {
    if (bitmap == null) {
      this.error = true;
      this.message = "bitmap is null!";
      this.response();
      return;
    }
    bitmap = TTVImageUtil.getAlignedBitmap(bitmap, true);

    byte[] bgr24 = TTVImageUtil.createImageData(bitmap.getWidth(), bitmap.getHeight(), TTVImageFormat.BGR24);
    int transformCode = TTVImageUtil.bitmapToImageData(bitmap, bgr24, TTVImageFormat.BGR24);
    if (transformCode != TTVImageUtilError.CODE_SUCCESS) {
      this.error = true;
      this.message = "transform bitmap To ImageData failed";
      this.response();
      return;
    }

    List<FaceResult> faceResults = FaceEngine.getInstance(this).detectFace(bitmap);
    int faceProcessCode = FaceEngine.getInstance(this).faceAttrProcess(bitmap, faceResults);

    if (faceProcessCode != ErrorInfo.MOK) {
      this.error = true;
      this.message = "bitmap process failed!";
      this.response();
      return;
    }

    if (faceResults.size() == 0) {
      this.error = true;
      this.message = "bitmap can not do further action, exit!";
      this.response();
      return;
    }

    int isMask = MaskInfo.UNKNOWN;
    if (!faceResults.isEmpty()) {
      isMask = faceResults.get(0).mask;
    }

    if (isMask == MaskInfo.UNKNOWN) {
      this.error = true;
      this.message = "mask is unknown";
      this.response();
      return;
    }

    if (type == TYPE_MAIN && isMask == MaskInfo.WORN) {
      this.error = true;
      this.message = "Please choose main image without mask";
      this.response();
      return;
    }

    if(type == TYPE_MAIN && faceResults.size() > 0) {
      mainFeature = faceResults.get(0).feature;
    }

    if(type == TYPE_ITEM) {
      FaceEngine.getInstance(this).extractFeature(bitmap, false, faceResults);
      if(faceResults.size() > 0) {
        float score = FaceEngine.getInstance(this).compareFeature(mainFeature, faceResults.get(0).feature);
        this.similarity = score;
        this.error = false;
        response();
        return;
      }
    }
  }

  private void response() {
    JSONObject obj = new JSONObject();
    try {
      obj.put("error", error);
      obj.put("message", this.message);
      obj.put("similarity", this.similarity);
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
