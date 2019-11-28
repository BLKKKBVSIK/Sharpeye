package sharpeye.sharpeye.data

import android.provider.BaseColumns

object DBBooleanKeyValueContract {

    /**
     * Inner class that defines the table contents
     */
    class BooleanEntry : BaseColumns {
        companion object {
            const val TABLE_NAME = "BooleanValues"
            const val COLUMN_KEY = "key"
            const val COLUMN_VALUE = "value"
        }
    }
}