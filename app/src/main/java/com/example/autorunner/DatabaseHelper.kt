package com.example.autorunner

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_LATITUDE REAL, " +
                "$COLUMN_LONGITUDE REAL)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun deleteLandmark(latitude: Double, longitude: Double) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_LATITUDE = ? AND $COLUMN_LONGITUDE = ?", arrayOf(latitude.toString(), longitude.toString()))
        db.close()
    }

    companion object {
        private const val DATABASE_NAME = "landmarks.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_NAME = "landmarks"
        const val COLUMN_ID = "id"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
    }
} 