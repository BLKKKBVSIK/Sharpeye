package sharpeye

import android.provider.BaseColumns

object DBBooleanKeyValueContract {

    /* Inner class that defines the table contents */
    class BooleanEntry : BaseColumns {
        companion object {
            val TABLE_NAME = "BooleanValues"
            val COLUMN_KEY = "key"
            val COLUMN_VALUE = "value"
        }
    }
}