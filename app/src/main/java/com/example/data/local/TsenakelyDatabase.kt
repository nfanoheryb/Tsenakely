package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductEntity::class,
        CustomerEntity::class,
        CreditEntryEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        ExpenseEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TsenakelyDatabase : RoomDatabase() {
    abstract fun tsenakelyDao(): TsenakelyDao

    companion object {
        @Volatile
        private var INSTANCE: TsenakelyDatabase? = null

        fun getInstance(context: Context): TsenakelyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TsenakelyDatabase::class.java,
                    "tsenakely_shop.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
