package sharpeye


import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import sharpeye.sharpeye.data.DBHelper

import java.util.ArrayList

class BooleanKeyValueDBHelper(context: Context) : DBHelper<BooleanKeyValueModel>(context) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    @Throws(SQLiteConstraintException::class)
    override fun insert(booleanKeyValue: BooleanKeyValueModel): Boolean {
        // Gets the data repository in write mode
        val db = writableDatabase

        // Create a new map of values, where column names are the keys

        if (read(booleanKeyValue.key).isEmpty()) {
            Log.d("BooleanKeyValueDBHelper DATABASE insert", "start")
            val values = ContentValues()
            values.put(DBBooleanKeyValueContract.BooleanEntry.COLUMN_KEY, booleanKeyValue.key)
            values.put(DBBooleanKeyValueContract.BooleanEntry.COLUMN_VALUE, if(booleanKeyValue.value) 1 else 0)
            val newRowId =
                db.insert(DBBooleanKeyValueContract.BooleanEntry.TABLE_NAME, null, values)
            Log.d("BooleanKeyValueDBHelper DATABASE insert", "end")
        } else {
            Log.d("BooleanKeyValueDBHelper DATABASE update", "start")
            val newRowId =
                db.execSQL("UPDATE " + DBBooleanKeyValueContract.BooleanEntry.TABLE_NAME + "" +
                        " SET " + DBBooleanKeyValueContract.BooleanEntry.COLUMN_VALUE +"=" + (if(booleanKeyValue.value) 1 else 0) +
                        " WHERE "+ DBBooleanKeyValueContract.BooleanEntry.COLUMN_KEY + "='" + booleanKeyValue.key + "'")
            Log.d("BooleanKeyValueDBHelper DATABASE update", "end")
        }
        return true
    }

    @Throws(SQLiteConstraintException::class)
    override fun deleteStringId(key: String): Boolean {
        // Gets the data repository in write mode
        val db = writableDatabase
        // Define 'where' part of query.
        val selection = DBBooleanKeyValueContract.BooleanEntry.COLUMN_KEY + " LIKE ?"
        // Specify arguments in placeholder order.
        val selectionArgs = arrayOf(key)
        // Issue SQL statement.
        db.delete(DBBooleanKeyValueContract.BooleanEntry.TABLE_NAME, selection, selectionArgs)

        return true
    }

    override fun deleteIntId(id: Int): Boolean { return false }

    override fun read(key: String): ArrayList<BooleanKeyValueModel> {
        val keyValue = ArrayList<BooleanKeyValueModel>()
        val db = writableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("select * from " + DBBooleanKeyValueContract.BooleanEntry.TABLE_NAME + " WHERE " + DBBooleanKeyValueContract.BooleanEntry.COLUMN_KEY + "='" + key + "'", null)
        } catch (e: SQLiteException) {
            // if table not yet present, create it
            db.execSQL(SQL_CREATE_ENTRIES)
            return ArrayList()
        }

        var value: Int
        if (cursor!!.moveToFirst()) {
            while (!cursor.isAfterLast) {
                value = cursor.getInt(cursor.getColumnIndex(DBBooleanKeyValueContract.BooleanEntry.COLUMN_VALUE))

                keyValue.add(BooleanKeyValueModel(key, value == 1))
                cursor.moveToNext()
            }
        }
        return keyValue
    }

    override fun readAll(): ArrayList<BooleanKeyValueModel> {
        val users = ArrayList<BooleanKeyValueModel>()
        val db = writableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("select * from " + DBBooleanKeyValueContract.BooleanEntry.TABLE_NAME, null)
        } catch (e: SQLiteException) {
            db.execSQL(SQL_CREATE_ENTRIES)
            return ArrayList()
        }

        var key: String
        var value: Int
        if (cursor!!.moveToFirst()) {
            while (!cursor.isAfterLast) {
                key = cursor.getString(cursor.getColumnIndex(DBBooleanKeyValueContract.BooleanEntry.COLUMN_KEY))
                value = cursor.getInt(cursor.getColumnIndex(DBBooleanKeyValueContract.BooleanEntry.COLUMN_VALUE))

                users.add(BooleanKeyValueModel(key, value == 1))
                cursor.moveToNext()
            }
        }
        return users
    }

    companion object {

        private val SQL_CREATE_ENTRIES =
            "CREATE TABLE " + DBBooleanKeyValueContract.BooleanEntry.TABLE_NAME + " ("+
                    DBBooleanKeyValueContract.BooleanEntry.COLUMN_KEY + " TEXT PRIMARY KEY," +
                    DBBooleanKeyValueContract.BooleanEntry.COLUMN_VALUE + " INT)"

        private val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + DBBooleanKeyValueContract.BooleanEntry.TABLE_NAME
    }
}