package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.TypeConverter
import com.example.core.model.ProjectType

class Converters {
    @TypeConverter
    fun fromProjectType(value: ProjectType) = value.name

    @TypeConverter
    fun toProjectType(value: String) = ProjectType.valueOf(value)
}

@Database(entities = [ProjectEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
}
