package com.example.zly.photoedit1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * 编辑图片工具类
 * <p>
 * <p>
 * <p>
 * 注意要申明provider
 * <p>
 * 新建xml
 * <?xml version="1.0" encoding="utf-8"?>
 * <paths xmlns:android="http://schemas.android.com/apk/res/android">
 * <external-path
 * name="external_files"
 * path="." />
 * </paths>
 * <p>
 * <p>
 * 清单文件 使用
 * <provider
 * android:name=".EditPhotoUtil$GenericFileProvider"
 * android:authorities="${applicationId}.my.provider"
 * android:exported="false"
 * android:grantUriPermissions="true">
 * <meta-data
 * android:name="android.support.FILE_PROVIDER_PATHS"
 * android:resource="@xml/provider_paths" />
 * </provider>
 */
public class EditPhotoUtil {
    //图片保存路径
    private final String filePath = Environment.getExternalStorageDirectory() + "/RockyEditPhoto/";
    private int color = Color.RED;//画笔颜色
    private int width = 1;//画笔大小
    private int circle;//形状
    private static final int MY_REQUEST_CODE = 3022;

    /* 用来标识请求照相功能的activity */
    public static final int CAMERA_WITH_DATA = 3023;

    /* 用来标识请求gallery的activity */
    public static final int PHOTO_PICKED_WITH_DATA = 3021;

    private ArrayList<Shapes> shapes = new ArrayList<Shapes>();


    private static final EditPhotoUtil ourInstance = new EditPhotoUtil();
    public String tempPhotoPath;
    private File mCurrentPhotoFile;
    private int screenWidth;
    private Bitmap srcPic;
    private Bitmap copyPic;
    private Canvas canvas;
    private Paint paint;
    private ImageView iv;

    public static EditPhotoUtil getInstance() {
        return ourInstance;
    }

    private EditPhotoUtil() {
        paint = new Paint();
        paint.setAntiAlias(true);
    }

    /**
     * 设置画笔颜色
     *
     * @param color
     */
    public EditPhotoUtil setPintColor(int color) {
        this.color = color;
        return this;
    }

    /**
     * 设置画笔形状
     * <p>
     * 0 圆形
     * 1 矩形
     * 2 箭头
     */
    public EditPhotoUtil setPintType(PaintType type) {

        switch (type) {
            case OVAL:
                circle = 0;
                break;
            case RECT:
                circle = 1;
                break;
            case ARROW:
                circle = 2;
                break;
        }
        return this;
    }

    /**
     * 设置 画笔的粗细
     * <p>
     * 1 细
     * 5 一般
     * 10 粗
     */
    public EditPhotoUtil setPaintWidthType(PaintWidthType type) {
        switch (type) {
            case THIN:
                width = 1;
                break;
            case BLOCK:
                width = 10;

                break;
            case NORMAL:
                width = 5;
                break;
        }
        return this;
    }


    private static final String[] PERMISSION_EXTERNAL_STORAGE = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int REQUEST_EXTERNAL_STORAGE = 100;


    /* 从相册中获取照片 */
    public void getPictureFromPhoto(Activity activity) {

        int permissionWrite = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionWrite != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSION_EXTERNAL_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        } else {
            Intent openphotoIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activity.startActivityForResult(openphotoIntent, PHOTO_PICKED_WITH_DATA);

        }


    }

    /**
     * 拍照
     */
    public void takePhoto(Activity activity) {
        DisplayMetrics metric = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metric);
        // 屏幕宽度（像素）
        screenWidth = metric.widthPixels;

        //申请照相机权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{Manifest.permission.CAMERA},
                        MY_REQUEST_CODE);
            } else {
                getPictureFromCamera(activity);
            }
        }


    }


    /* 从相机中获取照片 */
    private void getPictureFromCamera(Activity activity) {
        DisplayMetrics metric = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metric);
        // 屏幕宽度（像素）
        screenWidth = metric.widthPixels;
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");

        tempPhotoPath = filePath + getNewFileName() + ".jpg";

        mCurrentPhotoFile = new File(tempPhotoPath);

        if (!mCurrentPhotoFile.exists()) {
            try {
                mCurrentPhotoFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


//        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mCurrentPhotoFile));
        intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(activity,
                activity.getApplicationContext().getPackageName() + ".my.provider",
                mCurrentPhotoFile));
        activity.startActivityForResult(intent, CAMERA_WITH_DATA);
    }


    /**
     * 根据时间戳生成文件名
     *
     * @return
     */
    private String getNewFileName() {
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
    private String saveBitmap(Bitmap bitmap, String name) {
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

    /**
     * 单步撤销
     */
    public void cancelOneStep() {
        int size = shapes.size();
        if (size > 0) {
            shapes.remove(size - 1);
            drawGuiji();
        }
    }

    /**
     * 全部撤销
     */
    public void cancelAll() {
        shapes.clear();
        drawGuiji();
    }

    /**
     * 画画
     *
     * @param photoPath 相册返回或者拍照返回的路径
     * @param iv        显示的图片
     */
    @SuppressLint("ClickableViewAccessibility")
    public void drawPic(String photoPath, final ImageView iv) {

        this.iv = iv;
        Bitmap bitmap = compressionFiller(photoPath, iv);

        String camera_path = saveBitmap(bitmap, "saveTemp");

        srcPic = BitmapFactory.decodeFile(camera_path);
        copyPic = Bitmap.createBitmap(srcPic.getWidth(), srcPic.getHeight(),
                srcPic.getConfig());
        canvas = new Canvas(copyPic);

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
        Matrix matrix = new Matrix();
        // 5、使用画笔在画板上画画
        // 参看原图画画
        // srcPic 原图
        // matrix 表示图形的矩阵对象,封装了处理图形的api
        // paint 画画时使用的画笔
        canvas.drawBitmap(srcPic, matrix, paint);
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

    ////////

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


    public enum PaintType {
        OVAL("圆"), RECT("矩形"), ARROW("箭头");

        private String des;

        PaintType(String des) {

            this.des = des;
        }
    }

    public enum PaintWidthType {
        THIN("细"), NORMAL("中"), BLOCK("粗");

        private String des;

        PaintWidthType(String des) {

            this.des = des;
        }
    }


    public void saveEdit(Activity activity) {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_REQUEST_CODE);
            } else {
                String rocky_edit_pic = saveBitmap(getViewBitmap(iv), getNewFileName() + "_rocky_edit_pic");
                Log.d("EditPhotoUtil", rocky_edit_pic);
                cancelAll();
            }
        }

    }


    private Bitmap getViewBitmap(View v) {
        v.clearFocus();
        v.setPressed(false);
        boolean willNotCache = v.willNotCacheDrawing();
        v.setWillNotCacheDrawing(false);
        int color = v.getDrawingCacheBackgroundColor();
        v.setDrawingCacheBackgroundColor(0);
        if (color != 0) {
            v.destroyDrawingCache();
        }
        v.buildDrawingCache();
        Bitmap cacheBitmap = v.getDrawingCache();
        if (cacheBitmap == null) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(cacheBitmap);
        v.destroyDrawingCache();
        v.setWillNotCacheDrawing(willNotCache);
        v.setDrawingCacheBackgroundColor(color);
        return bitmap;
    }

    public static class GenericFileProvider extends FileProvider {
        public GenericFileProvider() {
        }
    }
}
