package com.wuwang.aavt.examples;

import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.wuwang.aavt.av.CameraRecorder;
import com.wuwang.aavt.gl.BeautyFilter;
import com.wuwang.aavt.gl.Filter;
import com.wuwang.aavt.core.Renderer;
import com.wuwang.aavt.utils.MatrixUtils;

import java.io.IOException;

public class CameraRecorderActivity extends AppCompatActivity implements Renderer {

    private CameraRecorder mCameraRecord;
    private SurfaceView mSurfaceView;
    private Camera mCamera;
    private TextView mTvStart;
    private boolean isStart=false;
    private Filter mFilter;
    private int mCameraWidth,mCameraHeight;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_record);
        mSurfaceView= (SurfaceView) findViewById(R.id.mSurface);
        mFilter=new BeautyFilter(getResources()).setBeautyLevel(5);
        mCameraRecord=new CameraRecorder();

        mCameraRecord.setOutputPath(Environment.getExternalStorageDirectory().getAbsolutePath()+"/temp_cam.mp4");
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mCamera=Camera.open(1);
                mCameraRecord.setOutputSurface(holder.getSurface());
                mCameraRecord.setOutputSize(480, 640);
                mCameraRecord.setRenderer(CameraRecorderActivity.this);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mCameraRecord.setPreviewSize(width,height);
                mCameraRecord.startPreview();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if(isStart){
                    isStart=false;
                    try {
                        mCameraRecord.stopRecord();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mTvStart.setText("开始");
                }
                try {
                    mCameraRecord.stopPreview();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(mCamera!=null){
                    mCamera.stopPreview();
                    mCamera.release();
                    mCamera=null;
                }
            }
        });
        mTvStart= (TextView) findViewById(R.id.mTvStart);
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.mTvStart:
                isStart=!isStart;
                mTvStart.setText(isStart?"停止":"开始");
                if(isStart){
                    try {
                        mCameraRecord.startRecord();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    try {
                        mCameraRecord.stopRecord();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Intent v=new Intent(Intent.ACTION_VIEW);
                    v.setDataAndType(Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath()+"/temp_cam.mp4"),"video/mp4");
                    startActivity(v);
                }
                break;
        }
    }

    @Override
    public void create() {
        try {
            mCamera.setPreviewTexture(mCameraRecord.createInputSurfaceTexture());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Camera.Size mSize=mCamera.getParameters().getPreviewSize();
        mCameraWidth=mSize.height;
        mCameraHeight=mSize.width;
        mCamera.startPreview();

        mFilter.create();
    }

    @Override
    public void sizeChanged(int width, int height) {
        mFilter.sizeChanged(width, height);
        MatrixUtils.getMatrix(mFilter.getVertexMatrix(),MatrixUtils.TYPE_CENTERCROP,mCameraWidth,mCameraHeight,width,height);
        MatrixUtils.flip(mFilter.getVertexMatrix(),false,true);
    }

    @Override
    public void draw(int texture) {
        mFilter.draw(texture);
    }

    @Override
    public void destroy() {
        mFilter.destroy();
    }
}
