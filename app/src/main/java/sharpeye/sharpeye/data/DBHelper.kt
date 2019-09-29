package sharpeye.sharpeye.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.ArrayList

abstract class DBHelper<T>(context: Context): SQLiteOpenHelper(context,
    DATABASE_NAME, null,
    DATABASE_VERSION
)
{
    abstract override fun onCreate(db: SQLiteDatabase)

    abstract override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int)

    abstract override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int)

    abstract fun insert(data: T): Boolean

    @Throws(SQLiteConstraintException::class)
    abstract fun deleteStringId(id: String): Boolean

    @Throws(SQLiteConstraintException::class)
    abstract fun deleteIntId(id: Int): Boolean

    abstract fun read(id: String): ArrayList<T>

    abstract fun readAll(): ArrayList<T>

    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "FeedReader.db"
    }
}