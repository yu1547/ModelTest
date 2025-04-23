package com.example.counterapp

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.Context

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface

object CameraManager {
    private const val REQUEST_IMAGE_CAPTURE = 100
    private var photoUri: Uri? = null

    fun openCamera(activity: Activity) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        try {
            photoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // 使用 MediaStore 來存入共享存儲
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${getTimeStamp()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            } else {
                // Android 9 以下使用 FileProvider
                val photoFile = createImageFile(activity)
                FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", photoFile)
            }

            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            activity.startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun handleCameraResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?, callback: (Bitmap?) -> Unit) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            try {
                val bitmap = photoUri?.let { uri ->
                    MediaStore.Images.Media.getBitmap(activity.contentResolver, uri)
                }

                // 取得 EXIF 資訊並調整旋轉角度
                val rotatedBitmap = bitmap?.let { adjustImageOrientation(activity, it, photoUri!!) }

                callback(rotatedBitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        } else {
            callback(null)
        }
    }



    private fun createImageFile(activity: Activity): File {
        val storageDir: File? = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("IMG_${getTimeStamp()}", ".jpg", storageDir)
    }

    private fun getTimeStamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }

    private fun adjustImageOrientation(context: Context, bitmap: Bitmap, imageUri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val exif = inputStream?.let { ExifInterface(it) }
        val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) ?: ExifInterface.ORIENTATION_NORMAL

        val rotationAngle = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        inputStream?.close()

        return if (rotationAngle != 0f) {
            val matrix = Matrix().apply { postRotate(rotationAngle) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }
}
