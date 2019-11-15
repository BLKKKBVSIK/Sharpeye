package sharpeye.sharpeye.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.ArrayList

/**
 * Helper to Setup and Handle a SQLite database
 * @param context
 */
abstract class DBHelper<T>(context: Context): SQLiteOpenHelper(context,
    DATABASE_NAME, null,
    DATABASE_VERSION
)
{
    /**
     * Called on Helper creation
     * @param db
     */
    abstract override fun onCreate(db: SQLiteDatabase)

    /**
     * Called on database Upgrade
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    abstract override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int)

    /**
     * Called on database downgrade
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    abstract override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int)

    /**
     * Allow to insert a dataModel T into the database
     * @return boolean value if it inserted a row or not
     * @param data
     */
    abstract fun insert(data: T): Boolean

    /**
     * Delete a row with a string ID
     * @param id
     * @return boolean value if it deleted a row or not
     * @throws SQLiteConstraintException
     */
    @Throws(SQLiteConstraintException::class)
    abstract fun deleteStringId(id: String): Boolean

    /**
     * Delete a row with a int ID
     * @param id
     * @return boolean value if it deleted a row or not
     * @throws SQLiteConstraintException
     */
    @Throws(SQLiteConstraintException::class)
    abstract fun deleteIntId(id: Int): Boolean

    /**
     * Read a row in database
     * @param id
     * @return a list of corresponding rows
     */
    abstract fun read(id: String): ArrayList<T>

    /**
     * Read all rows in database
     * @return a list with all the rows
     */
    abstract fun readAll(): ArrayList<T>

    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "FeedReader.db"
    }
}