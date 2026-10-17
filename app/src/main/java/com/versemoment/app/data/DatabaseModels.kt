package com.versemoment.app.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Transaction
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.versemoment.app.domain.BibleTranslation
import com.versemoment.app.domain.BibleVerse
import com.versemoment.app.domain.SelectionMode
import com.versemoment.app.domain.WallpaperApplyStatus

@Entity(tableName = "translations")
data class TranslationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val abbreviation: String,
    @ColumnInfo(name = "language_code") val languageCode: String,
    val attribution: String,
    @ColumnInfo(name = "license_id") val licenseId: String,
) {
    fun toDomain() = BibleTranslation(
        id = id,
        name = name,
        abbreviation = abbreviation,
        languageCode = languageCode,
        attribution = attribution,
        licenseId = licenseId,
    )
}

@Entity(
    tableName = "verses",
    indices = [
        Index(value = ["translation_id", "book_id", "chapter", "verse_number"], unique = true),
        Index(value = ["translation_id"]),
        Index(value = ["book_id"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = TranslationEntity::class,
            parentColumns = ["id"],
            childColumns = ["translation_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
)
data class VerseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "translation_id") val translationId: String,
    @ColumnInfo(name = "book_id") val bookId: String,
    @ColumnInfo(name = "book_name") val bookName: String,
    @ColumnInfo(name = "book_order") val bookOrder: Int,
    val chapter: Int,
    @ColumnInfo(name = "verse_number") val verseNumber: Int,
    val text: String,
) {
    fun toDomain() = BibleVerse(
        id = id,
        translationId = translationId,
        bookId = bookId,
        bookName = bookName,
        bookOrder = bookOrder,
        chapter = chapter,
        verseNumber = verseNumber,
        text = text,
        tags = emptyList(),
    )
}

@Entity(
    tableName = "verse_tags",
    primaryKeys = ["verse_id", "tag"],
    indices = [Index(value = ["tag"])],
    foreignKeys = [
        ForeignKey(
            entity = VerseEntity::class,
            parentColumns = ["id"],
            childColumns = ["verse_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
)
data class VerseTagEntity(
    @ColumnInfo(name = "verse_id") val verseId: String,
    val tag: String,
)

@Entity(
    tableName = "favorites",
    primaryKeys = ["verse_id"],
    foreignKeys = [
        ForeignKey(
            entity = VerseEntity::class,
            parentColumns = ["id"],
            childColumns = ["verse_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
)
data class FavoriteEntity(
    @ColumnInfo(name = "verse_id") val verseId: String,
    @ColumnInfo(name = "saved_at") val savedAt: Long,
)

@Entity(
    tableName = "wallpaper_history",
    indices = [
        Index(value = ["selected_at"]),
        Index(value = ["verse_id"]),
        Index(value = ["status"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = VerseEntity::class,
            parentColumns = ["id"],
            childColumns = ["verse_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
)
data class WallpaperHistoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "verse_id") val verseId: String,
    @ColumnInfo(name = "selection_mode") val selectionMode: String,
    @ColumnInfo(name = "selected_at") val selectedAt: Long,
    @ColumnInfo(name = "applied_at") val appliedAt: Long?,
    val status: String,
    @ColumnInfo(name = "theme_id") val themeId: String,
    @ColumnInfo(name = "error_code") val errorCode: String?,
    @ColumnInfo(name = "error_message") val errorMessage: String?,
)

class StringListConverter {
    @TypeConverter
    fun fromString(value: String?): List<String> = value?.split("|")?.filter { it.isNotBlank() } ?: emptyList()

    @TypeConverter
    fun toString(value: List<String>): String = value.joinToString(separator = "|")
}

@Dao
interface VerseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslation(entity: TranslationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerse(entity: VerseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerses(entities: List<VerseEntity>)

    @Transaction
    suspend fun replaceVerses(entities: List<VerseEntity>) {
        entities.chunked(500).forEach { insertVerses(it) }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(entity: VerseTagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(entity: FavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: WallpaperHistoryEntity)

    @Query("SELECT * FROM translations ORDER BY id ASC")
    suspend fun getTranslations(): List<TranslationEntity>

    @Query("SELECT * FROM verses WHERE translation_id = :translationId ORDER BY book_order, chapter, verse_number")
    suspend fun getVersesForTranslation(translationId: String): List<VerseEntity>

    @Query("SELECT COUNT(*) FROM verses WHERE translation_id = :translationId")
    suspend fun countVerses(translationId: String): Int

    @Query("SELECT * FROM verse_tags WHERE verse_id = :verseId")
    suspend fun getTagsForVerse(verseId: String): List<VerseTagEntity>

    @Query("SELECT * FROM favorites ORDER BY saved_at DESC LIMIT :limit")
    suspend fun getFavorites(limit: Int = 50): List<FavoriteEntity>

    @Query("SELECT * FROM wallpaper_history ORDER BY selected_at DESC LIMIT :limit")
    suspend fun getHistory(limit: Int = 50): List<WallpaperHistoryEntity>

    @Query("SELECT * FROM verses WHERE translation_id = :translationId AND book_id = :bookId AND chapter = :chapter AND verse_number = :verseNumber LIMIT 1")
    suspend fun getVerseByReference(translationId: String, bookId: String, chapter: Int, verseNumber: Int): VerseEntity?

    @Query("SELECT * FROM verses WHERE translation_id = :translationId AND chapter = :chapter AND verse_number = :verseNumber LIMIT 1")
    suspend fun getVerseByChapterAndVerse(translationId: String, chapter: Int, verseNumber: Int): VerseEntity?

    @Query("SELECT COUNT(*) FROM favorites WHERE verse_id = :verseId")
    suspend fun favoriteCount(verseId: String): Int

    @Query("DELETE FROM favorites WHERE verse_id = :verseId")
    suspend fun removeFavorite(verseId: String)
}

@Database(
    entities = [
        TranslationEntity::class,
        VerseEntity::class,
        VerseTagEntity::class,
        FavoriteEntity::class,
        WallpaperHistoryEntity::class,
    ],
    version = 1,
)
@TypeConverters(StringListConverter::class)
abstract class VerseMomentDatabase : RoomDatabase() {
    abstract fun verseDao(): VerseDao

    companion object {
        @Volatile
        private var INSTANCE: VerseMomentDatabase? = null

        fun getInstance(context: Context): VerseMomentDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    VerseMomentDatabase::class.java,
                    "versemoment.db",
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = db
                db
            }
        }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Reserved for future schema changes.
    }
}

object DatabaseSeed {
    fun translations(): List<TranslationEntity> = listOf(
        TranslationEntity(
            id = "kjv",
            name = "King James Version",
            abbreviation = "KJV",
            languageCode = "en",
            attribution = "King James Version. Text dataset: Thiago Bodruk, Bible JSON; MIT licensed.",
            licenseId = "mit-thiago-bodruk-bible",
        ),
    )

    fun verses(): List<VerseEntity> = listOf(
        VerseEntity("jhn_8_12", "kjv", "john", "John", 43, 8, 12, "Then spake Jesus again unto them, saying, I am the light of the world: he that followeth me shall not walk in darkness, but shall have the light of life."),
        VerseEntity("ps_23_1", "kjv", "psalms", "Psalms", 19, 23, 1, "The LORD is my shepherd; I shall not want."),
        VerseEntity("php_4_13", "kjv", "philippians", "Philippians", 50, 4, 13, "I can do all things through Christ which strengtheneth me."),
        VerseEntity("isa_40_31", "kjv", "isaiah", "Isaiah", 23, 40, 31, "But they that wait upon the LORD shall renew their strength; they shall mount up with wings as eagles; they shall run, and not be weary; and they shall walk, and not faint."),
        VerseEntity("rom_15_13", "kjv", "romans", "Romans", 45, 15, 13, "Now the God of hope fill you with all joy and peace in believing, that ye may abound in hope, through the power of the Holy Ghost."),
        VerseEntity("1co_13_4", "kjv", "1corinthians", "1 Corinthians", 46, 13, 4, "Charity suffereth long, and is kind; charity envieth not; charity vaunteth not itself, is not puffed up."),
        VerseEntity("jhn_14_6", "kjv", "john", "John", 43, 14, 6, "Jesus saith unto him, I am the way, the truth, and the life: no man cometh unto the Father, but by me."),
        VerseEntity("prov_3_5", "kjv", "proverbs", "Proverbs", 20, 3, 5, "Trust in the LORD with all thine heart; and lean not unto thine own understanding."),
        VerseEntity("php_4_6", "kjv", "philippians", "Philippians", 50, 4, 6, "Be careful for nothing; but in every thing by prayer and supplication with thanksgiving let your requests be made known unto God."),
        VerseEntity("2ti_1_7", "kjv", "2timothy", "2 Timothy", 55, 1, 7, "For God hath not given us the spirit of fear; but of power, and of love, and of a sound mind."),
    )

    fun tags(): List<VerseTagEntity> = listOf(
        VerseTagEntity("jhn_8_12", "light"),
        VerseTagEntity("jhn_8_12", "hope"),
        VerseTagEntity("jhn_8_12", "guidance"),
        VerseTagEntity("ps_23_1", "peace"),
        VerseTagEntity("ps_23_1", "comfort"),
        VerseTagEntity("ps_23_1", "trust"),
        VerseTagEntity("php_4_13", "strength"),
        VerseTagEntity("php_4_13", "discipline"),
        VerseTagEntity("php_4_13", "faith"),
        VerseTagEntity("isa_40_31", "strength"),
        VerseTagEntity("rom_15_13", "hope"),
        VerseTagEntity("rom_15_13", "peace"),
        VerseTagEntity("1co_13_4", "love"),
        VerseTagEntity("1co_13_4", "forgiveness"),
        VerseTagEntity("jhn_14_6", "purpose"),
        VerseTagEntity("prov_3_5", "wisdom"),
        VerseTagEntity("prov_3_5", "faith"),
        VerseTagEntity("php_4_6", "peace"),
        VerseTagEntity("2ti_1_7", "courage"),
        VerseTagEntity("2ti_1_7", "strength"),
    )
}
