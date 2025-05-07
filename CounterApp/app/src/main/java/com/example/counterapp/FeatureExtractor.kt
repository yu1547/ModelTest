package com.example.counterapp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

class FeatureDatabase(context: Context) : SQLiteOpenHelper(context, copyDatabase(context), null, 1) {

    override fun onCreate(db: SQLiteDatabase) {}

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    companion object {
        private fun copyDatabase(context: Context): String {
            val dbFile = File(context.filesDir, "train_features.db")
            if (!dbFile.exists()) {
                context.assets.open("train_features.db").use { inputStream ->
                    dbFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
            return dbFile.absolutePath
        }
    }

    // 🔎 取得所有資料庫內的 ID（確保 `train_features.db` 內的 ID 正確）
    fun getAllIds(): List<Int> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id FROM features ORDER BY id", null)

        val idList = mutableListOf<Int>()
        while (cursor.moveToNext()) {
            idList.add(cursor.getInt(0))
        }

        cursor.close()
        println("✅ SQLite 內所有 ID: ${idList.joinToString(", ")}") // Debug 訊息
        return idList
    }

    fun getClassNameById(imageId: Int): String? {
        val db = readableDatabase

        println("🔎 正在查詢 ID: $imageId")  // 確認輸入的 `ID`

        // 檢查資料庫內是否有該 ID
        val debugCursor = db.rawQuery("SELECT id, label FROM features WHERE id = ?", arrayOf(imageId.toString()))
        if (debugCursor.moveToFirst()) {
            println("✅ 資料庫內的 ID: ${debugCursor.getInt(0)}, 類別: ${debugCursor.getString(1)}")
        } else {
            println("❌ 資料庫內找不到該 ID: $imageId")
        }

        val cursor = db.rawQuery("SELECT label FROM features WHERE id = ?", arrayOf(imageId.toString()))
        return if (cursor.moveToFirst()) {
            val label = cursor.getString(0)
            println("✅ 找到類別: $label")
            label
        } else {
            println("❌ 在資料庫中找不到對應的類別 (ID: $imageId)")
            "未知類別"
        }
    }




}
