package sharpeye.sharpeye.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log

import java.util.ArrayList

/**
 * Allow to access a Boolean Key/Value SQLite database
 * @param context
 */
class BooleanKeyValueDBHelper(context: Context) : DBHelper<BooleanKeyValueModel>(context) {

    /**
     * Called on creation
     * @param db
     */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    /**
     * Called on database Upgrade
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    /**
     * Called on database downgrade
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    /**
     * Allow to insert a BooleanKeyValueModel into the database
     * @return boolean value if it inserted a row or not
     * @param data
     */
    @Throws(SQLiteConstraintException::class)
    override fun insert(data: BooleanKeyValueModel): Boolean {
        // Gets the data repository in write mode
        val db = writableDatabase

        // Create a new map of values, where column names are the keys

        if (read(data.key).isEmpty()) {
            Log.d("BooleanKeyValueDBHelper DATABASE insert", "start")
            val values = ContentValues()
            values.put(DBBooleanKeyValueContract.BooleanEntry.COLUMN_KEY, data.key)
            values.put(DBBooleanKeyValueContract.BooleanEntry.COLUMN_VALUE, if(data.value) 1 else 0)
            db.insert(DBBooleanKeyValueContract.BooleanEntry.TABLE_NAME, null, values)
            Log.d("BooleanKeyValueDBHelper DATABASE insert", "end")
        } else {
            Log.d("BooleanKeyValueDBHelper DATABASE update", "start")
            db.execSQL("UPDATE " + DBBooleanKeyValueContract.BooleanEntry.TABLE_NAME + "" +
                    " SET " + DBBooleanKeyValueContract.BooleanEntry.COLUMN_VALUE +"=" + (if(data.value) 1 else 0) +
                    " WHERE "+ DBBooleanKeyValueContract.BooleanEntry.COLUMN_KEY + "='" + data.key + "'")
            Log.d("BooleanKeyValueDBHelper DATABASE update", "end")
        }
        return true
    }

    /**
     * Delete a row with a string ID
     * @param id
     * @return boolean value if it deleted a row or not
     * @throws SQLiteConstraintException
     */
    @Throws(SQLiteConstraintException::class)
    override fun deleteStringId(id: String): Boolean {
        // Gets the data repository in write mode
        val db = writableDatabase
        // Define 'where' part of query.
        val selection = DBBooleanKeyValueContract.BooleanEntry.COLUMN_KEY + " LIKE ?"
        // Specify arguments in placeholder order.
        val selectionArgs = arrayOf(id)
        // Issue SQL statement.
        db.delete(DBBooleanKeyValueContract.BooleanEntry.TABLE_NAME, selection, selectionArgs)

        return true
    }

    /**
     * Do nothing for this database
     * @param id
     * @return false
     * @throws SQLiteConstraintException
     */
    override fun deleteIntId(id: Int): Boolean { return false }

    /**
     * Read a row in database
     * @param id
     * @return a list of corresponding rows
     */
    override fun read(id: String): ArrayList<BooleanKeyValueModel> {
        val keyValue = ArrayList<BooleanKeyValueModel>()
        val db = writableDatabase
        var cursor: Cursor?
        try {
            cursor = db.rawQuery("select * from " + DBBooleanKeyValueContract.BooleanEntry.TABLE_NAME + " WHERE " + DBBooleanKeyValueContract.BooleanEntry.COLUMN_KEY + "='" + id + "'", null)
        } catch (e: SQLiteException) {
            // if table not yet present, create it
            db.execSQL(SQL_CREATE_ENTRIES)
            return ArrayList()
        }

        var value: Int
        if (cursor!!.moveToFirst()) {
            while (!cursor.isAfterLast) {
                value = cursor.getInt(cursor.getColumnIndex(DBBooleanKeyValueContract.BooleanEntry.COLUMN_VALUE))

                keyValue.add(BooleanKeyValueModel(id, value == 1))
                cursor.moveToNext()
            }
        }
        cursor.close()
        return keyValue
    }

    /**
     * Read all rows in database
     * @return a list with all the rows
     */
    override fun readAll(): ArrayList<BooleanKeyValueModel> {
        val users = ArrayList<BooleanKeyValueModel>()
        val db = writableDatabase
        var cursor: Cursor?
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
        cursor.close()
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