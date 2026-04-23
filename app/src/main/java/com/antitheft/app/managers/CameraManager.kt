package com.antitheft.app.managers

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.view.Surface
import java.io.File
import java.io.FileOutputStream

class CameraManager(private val context: Context) {
    
    private var camera: Camera? = null
    private var mediaRecorder: MediaRecorder? = null
    
    fun capturePhoto(useFlash: Boolean, callback: (String) -> Unit) {
        Thread {
            try {
                camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK)
                val parameters = camera?.parameters
                parameters?.apply {
                    pictureFormat = ImageFormat.JPEG
                    setPictureSize(1920, 1080)
                    flashMode = if (useFlash) Camera.Parameters.FLASH_MODE_ON else Camera.Parameters.FLASH_MODE_OFF
                    focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                }
                camera?.parameters = parameters
                
                val surfaceTexture = SurfaceTexture(0)
                camera?.setPreviewTexture(surfaceTexture)
                camera?.startPreview()
                
                Thread.sleep(500)
                
                camera?.takePicture(null, null, { data, _ ->
                    val file = File(context.externalCacheDir, "photo_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { it.write(data) }
                    camera?.stopPreview()
                    camera?.release()
                    camera = null
                    callback(file.absolutePath)
                })
            } catch (e: Exception) {
                callback("Error: ${e.message}")
            }
        }.start()
    }
    
    fun recordVideo(duration: Int, callback: (String) -> Unit) {
        Thread {
            try {
                val file = File(context.externalCacheDir, "video_${System.currentTimeMillis()}.mp4")
                camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK)
                camera?.unlock()
                
                mediaRecorder = MediaRecorder().apply {
                    setCamera(camera)
                    setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
                    setVideoSource(MediaRecorder.VideoSource.CAMERA)
                    val profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)
                    setOutputFormat(profile.fileFormat)
                    setVideoFrameRate(profile.videoFrameRate)
                    setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight)
                    setVideoEncodingBitRate(profile.videoBitRate)
                    setAudioEncodingBitRate(profile.audioBitRate)
                    setAudioChannels(profile.audioChannels)
                    setAudioSamplingRate(profile.audioSampleRate)
                    setOutputFile(file.absolutePath)
                    setMaxDuration(duration * 1000)
                    setPreviewDisplay(Surface(SurfaceTexture(0)))
                    setOnInfoListener { _, what, _ ->
                        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                            stopRecording()
                            callback(file.absolutePath)
                        }
                    }
                    prepare()
                    start()
                }
                
                Thread.sleep((duration * 1000).toLong())
                if (mediaRecorder != null) {
                    stopRecording()
                    callback(file.absolutePath)
                }
            } catch (e: Exception) {
                callback("Error: ${e.message}")
            }
        }.start()
    }
    
    private fun stopRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            camera?.lock()
            camera?.release()
            camera = null
        } catch (e: Exception) { e.printStackTrace() }
    }
}
