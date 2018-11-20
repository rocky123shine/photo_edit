package com.example.zly.rockyphotoeditdemo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int MY_REQUEST_CODE = 3022;
    private ArrayList<Shapes> shapes = new ArrayList<Shapes>();
    private ImageView iv;//展示图片
    private Bitmap copyPic;//编辑图片
    private Canvas canvas;//画板
    private Paint paint;//画笔
    private Matrix matrix;//矩阵
    private Bitmap srcPic;//原图
    private int color = Color.BLACK;//画笔颜色
    private int width = 0;//画笔大小
    private int circle;//形状
    /* 用来标识请求照相功能的activity */
    private static final int CAMERA_WITH_DATA = 3023;

    /* 用来标识请求gallery的activity */
    private static final int PHOTO_PICKED_WITH_DATA = 3021;

    private String photoPath, camera_path, tempPhotoPath;
    //图片保存路径
    public static final String filePath = Environment.getExternalStorageDirectory() + "/PictureTest/";
    private int screenWidth;
    private File mCurrentPhotoFile;


    private static final String[] PERMISSION_EXTERNAL_STORAGE = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int REQUEST_EXTERNAL_STORAGE = 100;

    private void verifyStoragePermissions(Activity activity) {
        int permissionWrite = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionWrite != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSION_EXTERNAL_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }else {
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        verifyStoragePermissions(this);
        iv = (ImageView) findViewById(R.id.iv);
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        // 屏幕宽度（像素）
        screenWidth = metric.widthPixels;
    }

    /**
     * 画画
     */
    private void drawPic() {
        srcPic = BitmapFactory.decodeFile(camera_path);
        copyPic = Bitmap.createBitmap(srcPic.getWidth(), srcPic.getHeight(),
                srcPic.getConfig());
        canvas = new Canvas(copyPic);
        paint = new Paint();
        paint.setAntiAlias(true);
        //绘制原图
        drawOld();
        iv.setImageBitmap(copyPic);
        //触摸事件
        iv.setOnTouchListener(new View.OnTouchListener() {

            private float endY;
            private float endX;
            private float startX;
            private float startY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:// 按下的事件类型
                        startX = event.getX();
                        startY = event.getY();
                        drawGuiji();
                        break;

                    case MotionEvent.ACTION_MOVE:// 移动的事件类型
                        // 得到结束位置的坐标点
                        endX = event.getX();
                        endY = event.getY();
                        // 清除之前轨迹
                        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                        canvas.drawPaint(paint);
                        drawGuiji();
                        paint.setStrokeWidth(width);
                        paint.setColor(color);
                        if (circle == 1) {
                            paint.setStyle(Paint.Style.STROKE);//设置边框
                            canvas.drawRect(startX, startY, endX, endY, paint);// 正方形
                        } else if (circle == 0) {
                            paint.setStyle(Paint.Style.STROKE);//设置边框
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                canvas.drawOval(startX, startY, endX, endY, paint);
                            }
                        } else if (circle == 2) {
                            paint.setStyle(Paint.Style.FILL);//设置边框
                            drawArrow(startX, startY, endX, endY, width, paint);
                        }
                        iv.setImageBitmap(copyPic);
                        break;

                    case MotionEvent.ACTION_UP:// 移动的事件类型
                        shapes.add(new Shapes(startX, startY, endX, endY, width, paint.getColor(), circle));//保存历史轨迹
                        break;
                }
                return true;
            }
        });
    }

    private void drawGuiji() {
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        drawOld();
        for (Shapes sp : shapes) {//画历史轨迹
            paint.setColor(sp.color);
            paint.setStrokeWidth(sp.width);
            if (sp.circle == 1) {
                paint.setStyle(Paint.Style.STROKE);//设置边框
                canvas.drawRect(sp.startX, sp.startY, sp.endX, sp.endY, paint);// 正方形
            } else if (sp.circle == 0) {
                paint.setStyle(Paint.Style.STROKE);//设置边框
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//api21之后的方法
                    canvas.drawOval(sp.startX, sp.startY, sp.endX, sp.endY, paint);//椭圆
                }
            } else if (sp.circle == 2) {
                paint.setStyle(Paint.Style.FILL);//设置边框
                drawArrow(sp.startX, sp.startY, sp.endX, sp.endY, sp.width, paint);//箭头
            }
        }
        iv.setImageBitmap(copyPic);
    }

    /**
     * 绘制底图
     */
    private void drawOld() {
        // 给画笔设置默认的颜色，在画画的过程中会使用原图的颜色来画画
        paint.setColor(Color.BLACK);

        // 处理图形
        matrix = new Matrix();
        // 5、使用画笔在画板上画画
        // 参看原图画画
        // srcPic 原图
        // matrix 表示图形的矩阵对象,封装了处理图形的api
        // paint 画画时使用的画笔
        canvas.drawBitmap(srcPic, matrix, paint);
    }

    /**
     * 红色按钮
     *
     * @param view
     */
    public void red(View view) {

        color = Color.RED;
    }

    /**
     * 绿色按钮
     *
     * @param view
     */
    public void green(View view) {
        color = Color.GREEN;

    }

    /**
     * 蓝色按钮
     *
     * @param view
     */
    public void blue(View view) {
        color = Color.BLUE;
    }

    public void small(View view) {
        //改变刷子的宽度
        width = 1;
    }

    public void zhong(View view) {
        //改变刷子的宽度
        width = 5;
    }

    public void big(View view) {
        //改变刷子的宽度
        width = 10;
    }

    /**
     * 圆形
     *
     * @param view
     */
    public void circle(View view) {
        circle = 0;
    }

    /**
     * 矩形
     *
     * @param view
     */
    public void fang(View view) {
        circle = 1;
    }

    /**
     * 矩形
     *
     * @param view
     */
    public void arrow(View view) {
        circle = 2;
    }

    /**
     * 相册
     *
     * @param view
     */
    public void pics(View view) {
        getPictureFromPhoto();
    }

    /**
     * 拍照
     *
     * @param view
     */
    public void photo(View view) {
        //申请照相机权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA},
                        MY_REQUEST_CODE);
            } else {
                getPictureFromCamera();
            }
        }
    }

    /**
     * 单步撤销
     *
     * @param view
     */
    public void one(View view) {
        int size = shapes.size();
        if (size > 0) {
            shapes.remove(size - 1);
            drawGuiji();
        }
    }

    /**
     * 全部撤销
     *
     * @param view
     */
    public void all(View view) {
        shapes.clear();
        drawGuiji();
    }

    /**
     * 画箭头
     *
     * @param sx
     * @param sy
     * @param ex
     * @param ey
     * @param paint
     */
    private void drawArrow(float sx, float sy, float ex, float ey, int width, Paint paint) {
        int size = 5;
        int count = 20;
        switch (width) {
            case 0:
                size = 5;
                count = 20;
                break;
            case 5:
                size = 8;
                count = 30;
                break;
            case 10:
                size = 11;
                count = 40;
                break;
        }
        float x = ex - sx;
        float y = ey - sy;
        double d = x * x + y * y;
        double r = Math.sqrt(d);
        float zx = (float) (ex - (count * x / r));
        float zy = (float) (ey - (count * y / r));
        float xz = zx - sx;
        float yz = zy - sy;
        double zd = xz * xz + yz * yz;
        double zr = Math.sqrt(zd);
        Path triangle = new Path();
        triangle.moveTo(sx, sy);
        triangle.lineTo((float) (zx + size * yz / zr), (float) (zy - size * xz / zr));
        triangle.lineTo((float) (zx + size * 2 * yz / zr), (float) (zy - size * 2 * xz / zr));
        triangle.lineTo(ex, ey);
        triangle.lineTo((float) (zx - size * 2 * yz / zr), (float) (zy + size * 2 * xz / zr));
        triangle.lineTo((float) (zx - size * yz / zr), (float) (zy + size * xz / zr));
        triangle.close();
        canvas.drawPath(triangle, paint);
    }

    /* 从相册中获取照片 */
    private void getPictureFromPhoto() {
        Intent openphotoIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(openphotoIntent, PHOTO_PICKED_WITH_DATA);
    }

    /* 从相机中获取照片 */
    private void getPictureFromCamera() {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");

        tempPhotoPath = filePath + getNewFileName()
                + ".jpg";

        mCurrentPhotoFile = new File(tempPhotoPath);

        if (!mCurrentPhotoFile.exists()) {
            try {
                mCurrentPhotoFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(mCurrentPhotoFile));
        startActivityForResult(intent, CAMERA_WITH_DATA);
    }

    /**
     * 根据时间戳生成文件名
     *
     * @return
     */
    public static String getNewFileName() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Date curDate = new Date(System.currentTimeMillis());

        return formatter.format(curDate);
    }

    /**
     * 将生成的图片保存到内存中
     *
     * @param bitmap
     * @param name
     * @return
     */
    public String saveBitmap(Bitmap bitmap, String name) {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            File dir = new File(filePath);
            if (!dir.exists())
                dir.mkdir();
            File file = new File(filePath + name + ".jpg");
            FileOutputStream out;
            try {
                out = new FileOutputStream(file);
                if (bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)) {
                    out.flush();
                    out.close();
                }
                return file.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 根据路径获取图片并且压缩，适应view
     *
     * @param filePath    图片路径
     * @param contentView 适应的view
     * @return Bitmap 压缩后的图片
     */
    public Bitmap compressionFiller(String filePath, View contentView) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inPurgeable = true;
        opt.inInputShareable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(filePath, opt);

        int layoutHeight = contentView.getHeight();
        float scale = 0f;
        int bitmapHeight = bitmap.getHeight();
        int bitmapWidth = bitmap.getWidth();
        scale = bitmapHeight > bitmapWidth
                ? layoutHeight / (bitmapHeight * 1f)
                : screenWidth / (bitmapWidth * 1f);
        Bitmap resizeBmp;
        if (scale != 0) {
            int bitmapheight = bitmap.getHeight();
            int bitmapwidth = bitmap.getWidth();
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale); // 长和宽放大缩小的比例
            resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmapwidth,
                    bitmapheight, matrix, true);
        } else {
            resizeBmp = bitmap;
        }
        return resizeBmp;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case CAMERA_WITH_DATA:
                photoPath = tempPhotoPath;
                break;
            case PHOTO_PICKED_WITH_DATA:
                if (data == null) {
                    Log.d("MainActivity", "fanhui null");
                    return;
                }
                Uri selectedImage = data.getData();

                if (null != selectedImage) {
                    String[] filePathColumns = {MediaStore.Images.Media.DATA};
                    Cursor c = getContentResolver().query(selectedImage, filePathColumns, null, null, null);
                    int columnIndex = c.getColumnIndex(filePathColumns[0]);
                    c.moveToFirst();
                    photoPath = c.getString(columnIndex);
                    c.close();
                    break;
                } else {
                    Log.d("MainActivity", "selectedImage:" + selectedImage);
                    return;
                }

        }
        shapes.clear();
        if (TextUtils.isEmpty(photoPath)) {
            Log.d("MainActivity", "路径 null");
            return;
        }
        Log.d("MainActivityphotoPath", photoPath);
        Bitmap bitmap = compressionFiller(photoPath, iv);
        if (bitmap == null) {
            Log.d("MainActivity", "bitmap:" + bitmap);
            return;
        }
        camera_path = saveBitmap(bitmap, "saveTemp");
        drawPic();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 调用相机
                getPictureFromCamera();
            } else {
                //没有权限
                Toast.makeText(MainActivity.this, "没有权限", Toast.LENGTH_SHORT).show();
            }
        }
    }


    class Shapes {
        public float startX, startY, endX, endY;
        public int width, color, circle;

        public Shapes(float startX, float startY, float endX, float endY, int width, int color, int circle) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.width = width;
            this.color = color;
            this.circle = circle;
        }
    }
//---------------------
//    作者：gu_jingli
//    来源：CSDN
//    原文：https://blog.csdn.net/gu_jingli/article/details/62429972
//    版权声明：本文为博主原创文章，转载请附上博文链接！
}


